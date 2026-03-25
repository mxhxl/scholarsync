package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// ── Request models ──────────────────────────────────────────────────────────

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

// ── Response models ─────────────────────────────────────────────────────────

data class UserResponse(
    val id: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val institution: String?
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserResponse
)

data class RefreshResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class ApiError(
    val detail: String?
)

// ── Interface ───────────────────────────────────────────────────────────────

interface AuthApi {
    fun register(
        baseUrl: String,
        email: String,
        password: String,
        fullName: String,
        onResult: (Result<TokenResponse>) -> Unit
    )

    fun login(
        baseUrl: String,
        email: String,
        password: String,
        onResult: (Result<TokenResponse>) -> Unit
    )

    fun refreshToken(
        baseUrl: String,
        refreshToken: String,
        onResult: (Result<RefreshResponse>) -> Unit
    )

    fun changePassword(
        baseUrl: String,
        bearerToken: String?,
        currentPassword: String,
        newPassword: String,
        onResult: (Result<Unit>) -> Unit
    )
}

// ── Implementation ──────────────────────────────────────────────────────────

class AuthApiImpl : AuthApi {
    private val publicClient get() = TokenManager.publicClient
    private val authedClient get() = TokenManager.authenticatedClient

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    override fun register(
        baseUrl: String,
        email: String,
        password: String,
        fullName: String,
        onResult: (Result<TokenResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/auth/register"
        val body = RegisterRequest(email = email, password = password, fullName = fullName)
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = publicClient.newCall(request).execute()
                val responseBody = response.body?.string()
                when {
                    response.isSuccessful && responseBody != null -> {
                        val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                        onResult(Result.success(tokenResponse))
                    }
                    response.code == 409 -> {
                        onResult(Result.failure(Exception("An account with this email already exists")))
                    }
                    else -> {
                        val errorMsg = parseErrorMessage(responseBody) ?: "Registration failed (HTTP ${response.code})"
                        onResult(Result.failure(Exception(errorMsg)))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    override fun login(
        baseUrl: String,
        email: String,
        password: String,
        onResult: (Result<TokenResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/auth/login"
        val body = LoginRequest(email = email, password = password)
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = publicClient.newCall(request).execute()
                val responseBody = response.body?.string()
                when {
                    response.isSuccessful && responseBody != null -> {
                        val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                        onResult(Result.success(tokenResponse))
                    }
                    response.code == 401 -> {
                        val errorMsg = parseErrorMessage(responseBody)
                        if (errorMsg?.contains("inactive", ignoreCase = true) == true) {
                            onResult(Result.failure(Exception("Your account has been deactivated")))
                        } else {
                            onResult(Result.failure(Exception("Invalid email or password")))
                        }
                    }
                    else -> {
                        val errorMsg = parseErrorMessage(responseBody) ?: "Login failed (HTTP ${response.code})"
                        onResult(Result.failure(Exception(errorMsg)))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    override fun refreshToken(
        baseUrl: String,
        refreshToken: String,
        onResult: (Result<RefreshResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/auth/refresh"
        val body = RefreshRequest(refreshToken = refreshToken)
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = publicClient.newCall(request).execute()
                val responseBody = response.body?.string()
                when {
                    response.isSuccessful && responseBody != null -> {
                        val refreshResponse = gson.fromJson(responseBody, RefreshResponse::class.java)
                        onResult(Result.success(refreshResponse))
                    }
                    else -> {
                        onResult(Result.failure(Exception("Session expired. Please sign in again.")))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    override fun changePassword(
        baseUrl: String,
        bearerToken: String?,
        currentPassword: String,
        newPassword: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/auth/change-password"
        val body = ChangePasswordRequest(
            currentPassword = currentPassword,
            newPassword = newPassword
        )
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()
        Thread {
            try {
                val response = authedClient.newCall(request).execute()
                when {
                    response.code == 204 || response.isSuccessful -> onResult(Result.success(Unit))
                    response.code == 400 -> {
                        onResult(Result.failure(Exception("Current password is incorrect")))
                    }
                    else -> {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        onResult(Result.failure(Exception("HTTP ${response.code}: $errorBody")))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    private fun parseErrorMessage(responseBody: String?): String? {
        if (responseBody.isNullOrBlank()) return null
        return try {
            val error = gson.fromJson(responseBody, ApiError::class.java)
            error.detail
        } catch (_: Exception) {
            null
        }
    }
}
