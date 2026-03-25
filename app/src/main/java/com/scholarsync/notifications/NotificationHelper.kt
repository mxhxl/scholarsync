package com.scholarsync.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.scholarsync.MainActivity
import com.scholarsync.R

object NotificationHelper {

    const val CHANNEL_ID = "scholarsync_new_papers"
    private const val CHANNEL_NAME = "New Papers"
    private const val CHANNEL_DESC = "Notifications when new papers matching your research interests are published"

    /**
     * Creates the notification channel (safe to call multiple times; no-op if already exists).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a notification for a single new paper alert.
     */
    fun showNewPaperNotification(
        context: Context,
        notificationId: Int,
        paperTitle: String,
        topic: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New paper on $topic")
            .setContentText(paperTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(paperTitle))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission not granted — silently ignore
        }
    }

    /**
     * Shows a summary notification when multiple new papers are found at once.
     */
    fun showBatchNotification(
        context: Context,
        count: Int,
        topics: List<String>
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "alerts")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val topicText = if (topics.isNotEmpty()) {
            topics.distinct().joinToString(", ")
        } else {
            "your research interests"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$count new papers available")
            .setContentText("New research on $topicText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$count new papers matching $topicText have been published."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(BATCH_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    private const val BATCH_NOTIFICATION_ID = 99999
}
