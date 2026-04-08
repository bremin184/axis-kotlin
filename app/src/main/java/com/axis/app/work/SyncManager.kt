package com.axis.app.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncManager {

    private const val SYNC_WORK_TAG = "axis_sync_work"
    private const val REMINDER_WORK_TAG = "axis_savings_reminder"

    fun scheduleSync(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Notification Channel
        com.axis.app.ui.util.NotificationHelper.createNotificationChannel(context)

        // Constraints for better battery/data management
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<TransactionSyncWorker>(
            15, TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build()

        // Use UPDATE to ensure changes to constraints or intervals are applied
        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )

        // Savings Reminder (Every 24 hours)
        val reminderRequest = PeriodicWorkRequestBuilder<SavingsReminderWorker>(
            1, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            REMINDER_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
