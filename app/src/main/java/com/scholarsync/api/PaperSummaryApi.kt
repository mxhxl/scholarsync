package com.scholarsync.api

import android.util.Log
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
    companion object {
        private const val TAG = "PaperSummaryApi"
    }

    // Use longRunningClient — Ollama can take up to 180s to generate summaries
    private val client get() = TokenManager.longRunningClient

    private val gson = Gson()

    override fun fetchSummary(paperId: String, baseUrl: String, bearerToken: String?, onResult: (Result<SummaryResponse>) -> Unit) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/summary"
        Log.d(TAG, "Fetching summary for paper $paperId from $url")
        val request = Request.Builder().url(url).get().build()
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "Starting summary request for paper $paperId...")
                val response = client.newCall(request).execute()
                val elapsed = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                Log.d(TAG, "Summary response for paper $paperId: HTTP ${response.code} in ${elapsed}ms (body length: ${body.length})")

                when (response.code) {
                    200 -> {
                        val summary = gson.fromJson(body, SummaryResponse::class.java)
                        if (summary.status == "failed") {
                            Log.w(TAG, "Summary generation failed for paper $paperId (status=failed)")
                            onResult(Result.failure(Exception("AI summary generation failed. Please try again.")))
                        } else {
                            Log.d(TAG, "Summary loaded successfully for paper $paperId (status=${summary.status})")
                            onResult(Result.success(summary))
                        }
                    }
                    202 -> {
                        Log.d(TAG, "Summary still generating for paper $paperId (202)")
                        onResult(Result.failure(Exception("Summary is being generated. Please wait and try again in a moment.")))
                    }
                    else -> {
                        Log.e(TAG, "Summary request failed for paper $paperId: HTTP ${response.code} — $body")
                        onResult(Result.failure(Exception("Failed to load summary (HTTP ${response.code})")))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching summary for paper $paperId", e)
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    override fun fetchLiteratureReview(paperId: String, baseUrl: String, onResult: (Result<LiteratureReviewResponse>) -> Unit) {
        val url = baseUrl.trimEnd('/') + "/v1/papers/$paperId/literature-review"
        Log.d(TAG, "Fetching literature review for paper $paperId from $url")
        val request = Request.Builder().url(url).get().build()
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "Starting literature review request for paper $paperId...")
                val response = client.newCall(request).execute()
                val elapsed = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                Log.d(TAG, "Literature review response for paper $paperId: HTTP ${response.code} in ${elapsed}ms")

                when (response.code) {
                    200 -> {
                        val review = gson.fromJson(body, LiteratureReviewResponse::class.java)
                        if (review.status == "failed") {
                            Log.w(TAG, "Literature review generation failed for paper $paperId")
                            onResult(Result.failure(Exception("Literature review generation failed. Please try again.")))
                        } else {
                            Log.d(TAG, "Literature review loaded for paper $paperId (${review.entries.size} entries)")
                            onResult(Result.success(review))
                        }
                    }
                    else -> {
                        Log.e(TAG, "Literature review failed for paper $paperId: HTTP ${response.code} — $body")
                        onResult(Result.failure(Exception("Failed to load literature review (HTTP ${response.code})")))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching literature review for paper $paperId", e)
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
