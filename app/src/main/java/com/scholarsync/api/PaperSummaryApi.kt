package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Request

/**
 * API client for fetching AI-generated paper summary and literature review from the backend.
 */
data class SummaryResponse(
    @SerializedName("paper_id") val paperId: String = "",
    val purpose: String = "",
    val methodology: String = "",
    @SerializedName("key_results") val keyResults: String = "",
    val limitations: String = "",
    @SerializedName("relevance_to_field") val relevance: String = "",
    @SerializedName("research_gaps") val researchGaps: String = "",
    val status: String = "",
    @SerializedName("generated_at") val generatedAt: String? = null,
    @SerializedName("model_version") val modelVersion: String? = null
)

data class LitReviewEntry(
    @SerializedName("ref_no") val refNo: Int = 0,
    val authors: String = "",
    val year: String = "",
    val title: String = "",
    val methodology: String = "",
    @SerializedName("key_findings") val keyFindings: String = "",
    val limitations: String = ""
)

data class LiteratureReviewResponse(
    @SerializedName("paper_id") val paperId: String = "",
    val entries: List<LitReviewEntry> = emptyList(),
    val status: String = ""
)

// 202 response when summary is still generating
data class SummaryGeneratingResponse(
    val status: String = "",
    @SerializedName("retry_after") val retryAfter: Int = 60
)

interface PaperSummaryApi {
    fun fetchSummary(paperId: String, baseUrl: String, bearerToken: String?, onResult: (Result<SummaryResponse>) -> Unit)
    fun fetchLiteratureReview(paperId: String, baseUrl: String, onResult: (Result<LiteratureReviewResponse>) -> Unit)
}

class PaperSummaryApiImpl : PaperSummaryApi {
    private val client get() = TokenManager.authenticatedClient

    private val gson = Gson()

    override fun fetchSummary(paperId: String, baseUrl: String, bearerToken: String?, onResult: (Result<SummaryResponse>) -> Unit) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/summary"
        val request = Request.Builder().url(url).get().build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                when (response.code) {
                    200 -> {
                        val summary = gson.fromJson(body, SummaryResponse::class.java)
                        if (summary.status == "failed") {
                            onResult(Result.failure(Exception("AI summary generation failed. Please try again.")))
                        } else {
                            onResult(Result.success(summary))
                        }
                    }
                    202 -> {
                        onResult(Result.failure(Exception("Summary is being generated. Please wait and try again in a moment.")))
                    }
                    else -> {
                        onResult(Result.failure(Exception("Failed to load summary (HTTP ${response.code})")))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    override fun fetchLiteratureReview(paperId: String, baseUrl: String, onResult: (Result<LiteratureReviewResponse>) -> Unit) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/literature-review"
        val request = Request.Builder().url(url).get().build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                when (response.code) {
                    200 -> {
                        val review = gson.fromJson(body, LiteratureReviewResponse::class.java)
                        if (review.status == "failed") {
                            onResult(Result.failure(Exception("Literature review generation failed. Please try again.")))
                        } else {
                            onResult(Result.success(review))
                        }
                    }
                    else -> {
                        onResult(Result.failure(Exception("Failed to load literature review (HTTP ${response.code})")))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
