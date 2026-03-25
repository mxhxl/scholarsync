package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Request

data class UserStatsResponse(
    @SerializedName("papers_read") val papersRead: Int,
    @SerializedName("saved_papers") val savedPapers: Int,
    val summaries: Int,
    @SerializedName("unread_alerts") val unreadAlerts: Int
)

class StatsApi {
    private val client get() = TokenManager.authenticatedClient
    private val gson = Gson()

    fun getUserStats(
        baseUrl: String,
        onResult: (Result<UserStatsResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/stats/me"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val stats = gson.fromJson(body, UserStatsResponse::class.java)
                    onResult(Result.success(stats))
                } else {
                    onResult(Result.failure(Exception("Failed to fetch stats (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
