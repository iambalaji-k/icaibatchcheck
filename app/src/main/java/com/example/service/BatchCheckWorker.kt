package com.example.service

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aistudio.icaibatchchecker.R

/**
 * Periodic seat check driven by WorkManager — survives reboot, app update and
 * OEM kills without holding a foreground service idle. The FGS is held only
 * for the duration of the scrape (Android 14+ dataSync quota: seconds per run
 * instead of hours).
 */
class BatchCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        return try {
            // Progress goes to the Activity log; the FGS card is for the loop service.
            BatchCheckEngine.runCheck(
                context = applicationContext,
                source = "Background Check"
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Periodic mode may run before the app UI ever created the channels;
        // posting to a missing channel silently drops the FGS notification and
        // Android 12+ can then stop the worker's foreground service.
        NotificationHelper.createNotificationChannels(applicationContext)
        val notification: Notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ICAI Batch Checker")
            .setContentText("Running scheduled seat check...")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        // The type is declared for androidx.work's SystemForegroundService in
        // AndroidManifest.xml (tools:node="merge"); on Android 14+ passing it
        // here without that declaration throws MissingForegroundServiceTypeException.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1500
    }
}
