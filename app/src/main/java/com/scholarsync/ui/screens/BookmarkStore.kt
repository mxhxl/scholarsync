package com.scholarsync.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.scholarsync.api.LibraryApi

/**
 * In-memory store that tracks which paper IDs are saved in the user's library.
 * Loaded from the backend API; screens observe [savedPaperIds] for UI indicators.
 */
object LibrarySavedStore {
    val savedPaperIds = mutableStateListOf<String>()

    private val libraryApi = LibraryApi()

    fun isSaved(paperId: String): Boolean =
        savedPaperIds.contains(paperId)

    fun markSaved(paperId: String) {
        if (!savedPaperIds.contains(paperId)) {
            savedPaperIds.add(paperId)
        }
    }

    fun refresh(baseUrl: String, token: String) {
        libraryApi.getLibrary(baseUrl, token, limit = 100) { result ->
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { response ->
                    savedPaperIds.clear()
                    savedPaperIds.addAll(response.items.map { it.paper.id })
                }
            }
        }
    }
}

// Legacy BookmarkStore kept for BookmarksScreen compatibility
object BookmarkStore {
    val bookmarks = mutableStateListOf<BookmarkItem>()

    fun isBookmarked(paperId: String): Boolean =
        bookmarks.any { it.id == paperId }

    fun toggle(paperId: String, title: String = "Paper", author: String = "") {
        val index = bookmarks.indexOfFirst { it.id == paperId }
        if (index >= 0) {
            bookmarks.removeAt(index)
        } else {
            bookmarks.add(BookmarkItem(id = paperId, title = title.ifBlank { "Paper $paperId" }, author = author))
        }
    }

    fun remove(paperId: String) {
        bookmarks.removeAll { it.id == paperId }
    }
}
