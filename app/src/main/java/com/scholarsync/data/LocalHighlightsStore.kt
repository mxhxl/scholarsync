package com.scholarsync.data

import android.content.Context
import android.content.SharedPreferences
import com.scholarsync.api.HighlightResponse
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalHighlightsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scholarsync_highlights", Context.MODE_PRIVATE)

    fun getHighlights(paperId: String): List<HighlightResponse> {
        val json = prefs.getString("highlights_$paperId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HighlightResponse(
                    id = obj.getString("id"),
                    paperId = obj.getString("paper_id"),
                    highlightedText = obj.getString("highlighted_text"),
                    note = obj.optString("note", "").ifBlank { null },
                    color = obj.optString("color", "yellow"),
                    pageNumber = if (obj.has("page_number")) obj.optInt("page_number") else null,
                    createdAt = obj.optString("created_at", ""),
                    updatedAt = obj.optString("updated_at", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHighlight(paperId: String, highlightedText: String, note: String?, color: String): HighlightResponse {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis().toString()
        val highlight = HighlightResponse(
            id = id,
            paperId = paperId,
            highlightedText = highlightedText,
            note = note,
            color = color,
            createdAt = now,
            updatedAt = now
        )
        val current = getHighlights(paperId).toMutableList()
        current.add(highlight)
        saveAll(paperId, current)
        return highlight
    }

    fun deleteHighlight(paperId: String, highlightId: String) {
        val current = getHighlights(paperId).filterNot { it.id == highlightId }
        saveAll(paperId, current)
    }

    private fun saveAll(paperId: String, items: List<HighlightResponse>) {
        val arr = JSONArray()
        items.forEach { h ->
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("paper_id", h.paperId)
                put("highlighted_text", h.highlightedText)
                put("note", h.note ?: "")
                put("color", h.color)
                if (h.pageNumber != null) put("page_number", h.pageNumber)
                put("created_at", h.createdAt)
                put("updated_at", h.updatedAt)
            })
        }
        prefs.edit().putString("highlights_$paperId", arr.toString()).apply()
    }
}
