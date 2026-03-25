package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

// ── Response models ─────────────────────────────────────────────────────────

data class FeedPaperResponse(
    val id: String,
    @SerializedName("external_id") val externalId: String,
    val source: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val abstract: String? = null,
    @SerializedName("published_date") val publishedDate: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null,
    @SerializedName("citation_count") val citationCount: Int = 0,
    val venue: String? = null
)

data class FeedItemResponse(
    val id: String,
    @SerializedName("paper_id") val paperId: String,
    @SerializedName("relevance_score") val relevanceScore: Double,
    val priority: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("is_saved") val isSaved: Boolean,
    @SerializedName("created_at") val createdAt: String,
    val paper: FeedPaperResponse
)

data class FeedApiResponse(
    val items: List<FeedItemResponse> = emptyList(),
    val total: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class RefreshFeedResponse(
    val added: Int = 0,
    val message: String = ""
)

// ── Implementation ──────────────────────────────────────────────────────────

class FeedApi {
    private val client get() = TokenManager.authenticatedClient

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun getFeed(
        baseUrl: String,
        bearerToken: String,
        limit: Int = 20,
        offset: Int = 0,
        filter: String = "all",
        onResult: (Result<FeedApiResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/feed/?limit=$limit&offset=$offset&filter=$filter"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val feedResponse = gson.fromJson(body, FeedApiResponse::class.java)
                    onResult(Result.success(feedResponse))
                } else {
                    onResult(Result.failure(Exception("Failed to load feed (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun getPaper(
        baseUrl: String,
        bearerToken: String,
        paperId: String,
        onResult: (Result<FeedPaperResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val paper = gson.fromJson(body, FeedPaperResponse::class.java)
                    onResult(Result.success(paper))
                } else {
                    onResult(Result.failure(Exception("Failed to load paper (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun markRead(
        baseUrl: String,
        paperId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/feed/$paperId/mark-read"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(Exception("Failed to mark read (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun refreshFeed(
        baseUrl: String,
        bearerToken: String,
        onResult: (Result<RefreshFeedResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/feed/refresh"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val refreshResponse = gson.fromJson(body, RefreshFeedResponse::class.java)
                    onResult(Result.success(refreshResponse))
                } else {
                    onResult(Result.failure(Exception("Feed refresh failed (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
