package com.scholarsync.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class StreakResponse(
    @SerializedName("current_streak") val currentStreak: Int = 0,
    @SerializedName("longest_streak") val longestStreak: Int = 0,
    @SerializedName("total_papers_read") val totalPapersRead: Int = 0,
    @SerializedName("total_xp") val totalXp: Int = 0,
    @SerializedName("last_read_date") val lastReadDate: String? = null,
    @SerializedName("streak_start_date") val streakStartDate: String? = null,
    @SerializedName("today_papers") val todayPapers: Int = 0,
    @SerializedName("weekly_goal_progress") val weeklyGoalProgress: Int = 0,
    @SerializedName("weekly_goal") val weeklyGoal: Int = 5,
    @SerializedName("read_dates") val readDates: List<String> = emptyList()
)

data class RecordReadResponse(
    @SerializedName("xp_earned") val xpEarned: Int = 0,
    val streak: StreakResponse
)

class StreaksApi {
    private val client get() = TokenManager.authenticatedClient
    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun getStreak(
        baseUrl: String,
        onResult: (Result<StreakResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/streaks/me"
        val request = Request.Builder().url(url).get().build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    onResult(Result.success(gson.fromJson(body, StreakResponse::class.java)))
                } else {
                    onResult(Result.failure(Exception("HTTP ${response.code}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }

    fun recordRead(
        baseUrl: String,
        paperId: String,
        onResult: (Result<RecordReadResponse>) -> Unit
    ) {
        val url = baseUrl.trimEnd('/') + "/v1/streaks/record-read/$paperId"
        val request = Request.Builder()
            .url(url)
            .post("{}".toRequestBody(jsonType))
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    onResult(Result.success(gson.fromJson(body, RecordReadResponse::class.java)))
                } else {
                    onResult(Result.failure(Exception("HTTP ${response.code}")))
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }.start()
    }
}
