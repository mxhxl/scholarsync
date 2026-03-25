package com.scholarsync.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Helper to test notifications during development. Delete this file before release.
 *
 * Usage from MainActivity or any screen:
 *   NotificationTest.fireTestNotification(context)       // instant local notification
 *   NotificationTest.triggerWorkerNow(context)            // run the real worker immediately
 */
object NotificationTest {

    /**
     * Fires a fake notification immediately — tests that the channel, permission,
     * and display logic all work without needing the backend.
     */
    fun fireTestNotification(context: Context) {
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showNewPaperNotification(
            context = context,
            notificationId = 12345,
            paperTitle = "Attention Is All You Need: Revisiting Transformer Architectures for Low-Resource NLP",
            topic = "NLP"
        )
        Log.d("NotificationTest", "Test notification fired")
    }

    /**
     * Enqueues a one-time run of AlertCheckWorker so you don't have to wait 30 min.
     * Requires the backend to be running and to have at least one unread alert.
     */
    fun triggerWorkerNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AlertCheckWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
        Log.d("NotificationTest", "One-time AlertCheckWorker enqueued")
    }
}
