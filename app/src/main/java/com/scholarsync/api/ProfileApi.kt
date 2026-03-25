package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// ── Request models ──────────────────────────────────────────────────────────

data class ProfileSetupRequest(
    @SerializedName("research_field") val researchField: String,
    val topics: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    @SerializedName("authors_following") val authorsFollowing: List<String> = emptyList()
)

data class NotificationPreferencesRequest(
    @SerializedName("digest_time") val digestTime: String? = null,
    @SerializedName("overlap_sensitivity") val overlapSensitivity: String? = null,
    @SerializedName("enable_high_priority") val enableHighPriority: Boolean? = null,
    @SerializedName("enable_overlap_alerts") val enableOverlapAlerts: Boolean? = null,
    @SerializedName("enable_email") val enableEmail: Boolean? = null
)

data class ProfileGetResponse(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("research_field") val researchField: String = "",
    val topics: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    @SerializedName("authors_following") val authorsFollowing: List<String> = emptyList()
)

// ── Implementation ──────────────────────────────────────────────────────────

class ProfileApi {
    private val client get() = TokenManager.authenticatedClient

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun setupProfile(
        baseUrl: String,
        bearerToken: String,
        researchField: String,
        topics: List<String>,
        keywords: List<String>,
        authorsFollowing: List<String>,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/profile/setup"
        val body = ProfileSetupRequest(
            researchField = researchField,
            topics = topics,
            keywords = keywords,
            authorsFollowing = authorsFollowing
        )
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    onResult(Result.failure(Exception("Profile setup failed (HTTP ${response.code}): $errorBody")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun getProfile(
        baseUrl: String,
        onResult: (Result<ProfileGetResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/profile/"
        val request = Request.Builder().url(url).get().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    onResult(Result.success(gson.fromJson(body, ProfileGetResponse::class.java)))
                } else {
                    onResult(Result.failure(Exception("HTTP ${response.code}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }

    fun followAuthor(
        baseUrl: String,
        authorName: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/profile/follow-author"
        val json = gson.toJson(mapOf("author_name" to authorName))
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(Exception("HTTP ${response.code}")))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }

    fun unfollowAuthor(
        baseUrl: String,
        authorName: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/profile/unfollow-author"
        val json = gson.toJson(mapOf("author_name" to authorName))
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(Exception("HTTP ${response.code}")))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }

    fun updateNotificationPreferences(
        baseUrl: String,
        bearerToken: String,
        digestTime: String?,
        overlapSensitivity: String?,
        enableHighPriority: Boolean?,
        enableOverlapAlerts: Boolean?,
        enableEmail: Boolean?,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/profile/preferences"
        val body = NotificationPreferencesRequest(
            digestTime = digestTime,
            overlapSensitivity = overlapSensitivity,
            enableHighPriority = enableHighPriority,
            enableOverlapAlerts = enableOverlapAlerts,
            enableEmail = enableEmail
        )
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(url)
            .put(json.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    onResult(Result.failure(Exception("Preferences update failed (HTTP ${response.code}): $errorBody")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
