package com.scholarsync.api

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// ── Response models ─────────────────────────────────────────────────────────

data class FolderResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val name: String,
    @SerializedName("created_at") val createdAt: String
)

data class SavedPaperPaper(
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

data class SavedPaperResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("paper_id") val paperId: String,
    @SerializedName("folder_id") val folderId: String? = null,
    val tags: List<String> = emptyList(),
    @SerializedName("personal_note") val personalNote: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("saved_at") val savedAt: String,
    val paper: SavedPaperPaper
)

data class SavedPapersApiResponse(
    val items: List<SavedPaperResponse> = emptyList(),
    val total: Int = 0
)

// ── Request models ──────────────────────────────────────────────────────────

data class SavePaperRequest(
    @SerializedName("paper_id") val paperId: String,
    @SerializedName("folder_id") val folderId: String? = null,
    val tags: List<String> = emptyList(),
    @SerializedName("personal_note") val personalNote: String? = null
)

data class FolderCreateRequest(val name: String)

// ── Implementation ──────────────────────────────────────────────────────────

class LibraryApi {
    private val client get() = TokenManager.authenticatedClient
    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    // ── Folders ─────────────────────────────────────────────────────────────

    fun getFolders(
        baseUrl: String,
        bearerToken: String,
        onResult: (Result<List<FolderResponse>>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/library/folders"
        val request = Request.Builder().url(url).get().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val folders = gson.fromJson(body, Array<FolderResponse>::class.java).toList()
                    onResult(Result.success(folders))
                } else {
                    onResult(Result.failure(Exception("Failed to load folders (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun createFolder(
        baseUrl: String,
        bearerToken: String,
        name: String,
        onResult: (Result<FolderResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/library/folders"
        val jsonBody = gson.toJson(FolderCreateRequest(name))
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val folder = gson.fromJson(body, FolderResponse::class.java)
                    onResult(Result.success(folder))
                } else {
                    onResult(Result.failure(Exception("Failed to create folder (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    // ── Save / Get / Delete papers ──────────────────────────────────────────

    fun savePaper(
        baseUrl: String,
        bearerToken: String,
        paperId: String,
        folderId: String? = null,
        onResult: (Result<SavedPaperResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/library/save"
        val jsonBody = gson.toJson(SavePaperRequest(paperId = paperId, folderId = folderId))
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val saved = gson.fromJson(body, SavedPaperResponse::class.java)
                    onResult(Result.success(saved))
                } else if (response.code == 409) {
                    onResult(Result.failure(Exception("Paper is already in your library")))
                } else {
                    onResult(Result.failure(Exception("Failed to save paper (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun getLibrary(
        baseUrl: String,
        bearerToken: String,
        folderId: String? = null,
        limit: Int = 20,
        offset: Int = 0,
        onResult: (Result<SavedPapersApiResponse>) -> Unit
    ) {
        var url = baseUrl.trimEnd('/') + "/v1/library/?limit=$limit&offset=$offset"
        if (folderId != null) url += "&folder_id=$folderId"
        val request = Request.Builder().url(url).get().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val library = gson.fromJson(body, SavedPapersApiResponse::class.java)
                    onResult(Result.success(library))
                } else {
                    onResult(Result.failure(Exception("Failed to load library (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }

    fun deleteSavedPaper(
        baseUrl: String,
        bearerToken: String,
        paperId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/library/$paperId"
        val request = Request.Builder().url(url).delete().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.code == 204 || response.isSuccessful) {
                    onResult(Result.success(Unit))
                } else {
                    onResult(Result.failure(Exception("Failed to remove paper (HTTP ${response.code})")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(Exception("Network error: ${e.message}")))
            }
        }.start()
    }
}
