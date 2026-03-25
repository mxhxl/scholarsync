package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.Request

data class AlertPaperResponse(
    val id: String,
    val title: String,
    val authors: List<String> = emptyList(),
    @SerializedName("published_date") val publishedDate: String? = null,
    val source: String = "",
    val venue: String? = null
)

data class AlertResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("paper_id") val paperId: String,
    val type: String,
    val title: String,
    val description: String,
    @SerializedName("similarity_score") val similarityScore: Double,
    @SerializedName("comparison_report") val comparisonReport: Map<String, Any>? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("is_acknowledged") val isAcknowledged: Boolean,
    @SerializedName("created_at") val createdAt: String,
    val paper: AlertPaperResponse? = null
)

data class AlertsListResponse(
    val items: List<AlertResponse> = emptyList(),
    val total: Int = 0
)

data class UnreadCountResponse(
    val count: Int = 0
)

class AlertsApi {
    private val client get() = TokenManager.authenticatedClient
    private val gson = Gson()

    fun getAlerts(
        baseUrl: String,
        type: String? = null,
        isRead: Boolean? = null,
        limit: Int = 20,
        offset: Int = 0,
        onResult: (Result<AlertsListResponse>) -> Unit
    ) {
        val urlBuilder = StringBuilder(baseUrl.trimEnd('/') + "/v1/alerts/?limit=$limit&offset=$offset")
        if (type != null) urlBuilder.append("&type=$type")
        if (isRead != null) urlBuilder.append("&is_read=$isRead")

        val request = Request.Builder()
            .url(urlBuilder.toString())
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val result = gson.fromJson(body, AlertsListResponse::class.java)
                    onResult(Result.success(result))
                } else {
                    onResult(Result.failure(Exception("Failed to fetch alerts (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun getUnreadCount(
        baseUrl: String,
        onResult: (Result<Int>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/alerts/unread-count"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val result = gson.fromJson(body, UnreadCountResponse::class.java)
                    onResult(Result.success(result.count))
                } else {
                    onResult(Result.failure(Exception("Failed to fetch unread count (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    /**
     * Synchronous version for use in WorkManager background thread.
     */
    fun getUnreadAlertsSync(
        baseUrl: String,
        limit: Int = 5
    ): Result<AlertsListResponse> {
        val url = baseUrl.trimEnd('/') + "/v1/alerts/?is_read=false&limit=$limit&offset=0"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val result = gson.fromJson(body, AlertsListResponse::class.java)
                Result.success(result)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun markAlertRead(
        baseUrl: String,
        alertId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/alerts/$alertId/read"
        val request = Request.Builder()
            .url(url)
            .post(okhttp3.RequestBody.create(null, ByteArray(0)))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.code == 204 || response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(Exception("HTTP ${response.code}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }
}
