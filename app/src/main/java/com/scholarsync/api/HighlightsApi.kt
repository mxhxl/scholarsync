package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class HighlightResponse(
    val id: String,
    @SerializedName("paper_id") val paperId: String,
    @SerializedName("highlighted_text") val highlightedText: String,
    val note: String? = null,
    val color: String = "yellow",
    @SerializedName("page_number") val pageNumber: Int? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("updated_at") val updatedAt: String = ""
)

data class HighlightsListResponse(
    val items: List<HighlightResponse> = emptyList(),
    val total: Int = 0
)

class HighlightsApi {
    private val client get() = TokenManager.authenticatedClient
    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun getHighlights(
        baseUrl: String,
        paperId: String,
        onResult: (Result<HighlightsListResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/highlights"
        val request = Request.Builder().url(url).get().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    onResult(Result.success(gson.fromJson(body, HighlightsListResponse::class.java)))
                } else {
                    onResult(Result.failure(Exception("HTTP ${response.code}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }

    fun createHighlight(
        baseUrl: String,
        paperId: String,
        highlightedText: String,
        note: String? = null,
        color: String = "yellow",
        onResult: (Result<HighlightResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/highlights"
        val payload = mutableMapOf<String, Any>("highlighted_text" to highlightedText, "color" to color)
        if (note != null) payload["note"] = note
        val request = Request.Builder()
            .url(url)
            .post(gson.toJson(payload).toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    onResult(Result.success(gson.fromJson(body, HighlightResponse::class.java)))
                } else {
                    onResult(Result.failure(Exception("HTTP ${response.code}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }

    fun deleteHighlight(
        baseUrl: String,
        paperId: String,
        highlightId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/highlights/$highlightId"
        val request = Request.Builder().url(url).delete().build()

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
