package com.scholarsync.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.scholarsync.api.AlertsApi
import com.scholarsync.data.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic WorkManager worker that polls the backend for new unread alerts
 * and shows Android notifications for papers matching the user's topics.
 */
class AlertCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sessionManager = SessionManager(applicationContext)

            // Skip if user isn't logged in
            if (!sessionManager.isLoggedIn()) {
                return@withContext Result.success()
            }

            val baseUrl = sessionManager.getApiBaseUrl()
            val lastCheckedTime = sessionManager.getLastAlertCheckTime()
            val alertsApi = AlertsApi()

            // Fetch recent unread alerts
            val response = alertsApi.getUnreadAlertsSync(baseUrl, limit = 10)

            if (response.isFailure) {
                Log.w(TAG, "Failed to fetch alerts: ${response.exceptionOrNull()?.message}")
                return@withContext Result.retry()
            }

            val alertsData = response.getOrNull() ?: return@withContext Result.success()
            val alerts = alertsData.items

            // Filter to only alerts created after our last check
            val newAlerts = if (lastCheckedTime > 0) {
                alerts.filter { alert ->
                    try {
                        parseIsoTimestamp(alert.createdAt) > lastCheckedTime
                    } catch (_: Exception) {
                        true // If we can't parse, assume it's new
                    }
                }
            } else {
                // First run: don't spam notifications for all existing alerts.
                // Just record the current time and skip.
                sessionManager.setLastAlertCheckTime(System.currentTimeMillis())
                return@withContext Result.success()
            }

            if (newAlerts.isEmpty()) {
                return@withContext Result.success()
            }

            // Show notifications
            if (newAlerts.size == 1) {
                val alert = newAlerts.first()
                val topic = extractTopic(alert.title, alert.description)
                NotificationHelper.showNewPaperNotification(
                    context = applicationContext,
                    notificationId = alert.id.hashCode(),
                    paperTitle = alert.paper?.title ?: alert.title,
                    topic = topic
                )
            } else if (newAlerts.size <= 3) {
                // Show individual notifications for 2-3 alerts
                newAlerts.forEach { alert ->
                    val topic = extractTopic(alert.title, alert.description)
                    NotificationHelper.showNewPaperNotification(
                        context = applicationContext,
                        notificationId = alert.id.hashCode(),
                        paperTitle = alert.paper?.title ?: alert.title,
                        topic = topic
                    )
                }
            } else {
                // 4+ alerts: show a summary notification
                val topics = newAlerts.mapNotNull { extractTopicOrNull(it.title, it.description) }
                NotificationHelper.showBatchNotification(
                    context = applicationContext,
                    count = newAlerts.size,
                    topics = topics
                )
            }

            // Update last check time
            sessionManager.setLastAlertCheckTime(System.currentTimeMillis())

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AlertCheckWorker failed", e)
            Result.retry()
        }
    }

    /**
     * Extracts a topic name from the alert title/description.
     * The backend alert title often includes the topic, e.g. "New paper: Machine Learning".
     */
    private fun extractTopic(title: String, description: String): String {
        return extractTopicOrNull(title, description) ?: "your research interests"
    }

    private fun extractTopicOrNull(title: String, description: String): String? {
        // Backend format: "New paper on {topic}" in title
        val titlePattern = Regex("New paper on (.+)", RegexOption.IGNORE_CASE)
        titlePattern.find(title)?.let { return it.groupValues[1].trim() }

        // Backend format: "New paper by {author}" for followed-author alerts
        val authorPattern = Regex("New paper by (.+)", RegexOption.IGNORE_CASE)
        authorPattern.find(title)?.let { return it.groupValues[1].trim() }

        // Backend format: matching your "{topic}" interest in description
        val descPattern = Regex("matching your [\"'](.+?)[\"']", RegexOption.IGNORE_CASE)
        descPattern.find(description)?.let { return it.groupValues[1].trim() }

        // Fallback: use the alert title itself if it's short enough
        return if (title.length <= 50) title else null
    }

    /**
     * Parses an ISO 8601 timestamp string to epoch millis.
     */
    private fun parseIsoTimestamp(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            try {
                // Try parsing without timezone info
                java.time.LocalDateTime.parse(iso.replace(" ", "T"))
                    .atZone(java.time.ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                0L
            }
        }
    }

    companion object {
        private const val TAG = "AlertCheckWorker"
        const val WORK_NAME = "scholarsync_alert_check"
    }
}
