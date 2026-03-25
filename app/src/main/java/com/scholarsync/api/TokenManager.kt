package com.scholarsync.api

import android.content.Context
import com.google.gson.Gson
import com.scholarsync.data.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Centralized token manager that provides an OkHttpClient with automatic
 * token refresh on 401 responses. All API clients should use [authenticatedClient]
 * instead of creating their own OkHttpClient.
 */
object TokenManager {

    @Volatile
    private var sessionManager: SessionManager? = null

    @Volatile
    private var onSessionExpired: (() -> Unit)? = null

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // Lock to prevent multiple simultaneous refresh attempts
    private val refreshLock = Any()

    fun init(context: Context, onSessionExpired: () -> Unit) {
        sessionManager = SessionManager(context.applicationContext)
        this.onSessionExpired = onSessionExpired
    }

    fun getSessionManager(): SessionManager? = sessionManager

    /**
     * OkHttpClient that automatically:
     * 1. Adds the Bearer token to every request
     * 2. On 401, attempts to refresh the token and retry once
     * 3. If refresh fails, triggers [onSessionExpired]
     */
    val authenticatedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .build()
    }

    /** Unauthenticated client for login/register calls */
    val publicClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val sm = sessionManager ?: return chain.proceed(chain.request())

            val accessToken = sm.getAccessToken()
            val originalRequest = chain.request()

            // Add token to request if available
            val authedRequest = if (accessToken != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            } else {
                originalRequest
            }

            val response = chain.proceed(authedRequest)

            // If not 401, return as-is
            if (response.code != 401) return response

            // Got 401 — try to refresh the token
            val refreshToken = sm.getRefreshToken() ?: run {
                // No refresh token available — session is fully expired
                onSessionExpired?.invoke()
                return response
            }

            synchronized(refreshLock) {
                // Double-check: another thread may have already refreshed
                val currentToken = sm.getAccessToken()
                if (currentToken != null && currentToken != accessToken) {
                    // Token was already refreshed by another thread — retry with new token
                    response.close()
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                    return chain.proceed(retryRequest)
                }

                // Actually refresh
                val newTokens = doRefresh(sm.getApiBaseUrl(), refreshToken)
                if (newTokens != null) {
                    sm.setTokens(newTokens.accessToken, newTokens.refreshToken)
                    response.close()
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                    return chain.proceed(retryRequest)
                } else {
                    // Refresh failed — session is truly expired
                    sm.clearTokens()
                    onSessionExpired?.invoke()
                    return response
                }
            }
        }
    }

    /**
     * Synchronously calls the refresh endpoint.
     * Returns new tokens on success, null on failure.
     */
    private fun doRefresh(baseUrl: String, refreshToken: String): RefreshResponse? {
        return try {
            val url = baseUrl.trimEnd('/') + "/v1/auth/refresh"
            val body = gson.toJson(RefreshRequest(refreshToken = refreshToken))
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(jsonType))
                .build()

            // Use a separate plain client for the refresh call (no interceptor)
            val refreshClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val response = refreshClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                gson.fromJson(responseBody, RefreshResponse::class.java)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
