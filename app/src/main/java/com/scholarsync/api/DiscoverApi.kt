package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

data class DiscoverPaperResult(
    @SerializedName("external_id") val externalId: String = "",
    val source: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val abstract: String? = null,
    @SerializedName("published_date") val publishedDate: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null,
    @SerializedName("citation_count") val citationCount: Int = 0,
    val venue: String? = null
)

data class DiscoverSearchResponse(
    val query: String = "",
    val source: String = "",
    val total: Int = 0,
    val results: List<DiscoverPaperResult> = emptyList()
)

data class SavePaperResponse(
    val id: String = "",
    val title: String = ""
)

class DiscoverApi {
    private val client get() = TokenManager.authenticatedClient

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun search(
        baseUrl: String,
        bearerToken: String,
        query: String,
        source: String = "all",
        maxResults: Int = 15,
        onResult: (Result<DiscoverSearchResponse>) -> Unit
    ) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = baseUrl.trimEnd('/') +
            "/v1/discover/search?q=$encodedQuery&source=$source&max_results=$maxResults"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val result = gson.fromJson(body, DiscoverSearchResponse::class.java)
                    onResult(Result.success(result))
                } else {
                    onResult(Result.failure(Exception("Search failed (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun savePaper(
        baseUrl: String,
        bearerToken: String,
        paper: DiscoverPaperResult,
        onResult: (Result<SavePaperResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/discover/save"
        val bodyJson = gson.toJson(mapOf(
            "external_id" to paper.externalId,
            "source" to paper.source,
            "title" to paper.title,
            "authors" to paper.authors,
            "abstract" to (paper.abstract ?: ""),
            "published_date" to paper.publishedDate,
            "pdf_url" to paper.pdfUrl,
            "citation_count" to paper.citationCount,
            "venue" to paper.venue
        ))
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val result = gson.fromJson(body, SavePaperResponse::class.java)
                    onResult(Result.success(result))
                } else {
                    onResult(Result.failure(Exception("Failed to save paper (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
