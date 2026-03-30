package com.example.beautyapp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationPollingWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    private val backendClient = BackendClient()

    override fun doWork(): Result {
        val session = SessionManager(applicationContext)
        val token = session.getAccessToken() ?: return Result.success()

        return runCatching {
            val notifications = backendClient.getMyNotifications(token, unreadOnly = true)
            notifications.forEach { item ->
                val posted = AppNotificationHelper.showUserNotification(applicationContext, item)
                if (posted) {
                    backendClient.markNotificationRead(token, item.id)
                }
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
