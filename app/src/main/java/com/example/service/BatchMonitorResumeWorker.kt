package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.UserPreferences

/**
 * Re-arms the fast-mode looping foreground service when the app is allowed to
 * start services again (after boot or after the Android 14+ dataSync quota
 * window). No-op if the user has since paused monitoring.
 *
 * Android 12+ rejects background startForegroundService calls, which is exactly
 * when this worker runs — so a failed loop restart must NOT mean "monitoring
 * stopped". It degrades to the WorkManager periodic schedule: checks keep
 * running (every 15m instead of 2/5/10m) until the service can start again.
 */
class BatchMonitorResumeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        if (prefs.isMonitoringActive && !BatchMonitorService.isMonitoringLive.value) {
            // Fresh quota window: restart the timing base.
            prefs.monitoringStartedAt = System.currentTimeMillis()
            val started = BatchMonitorService.startMonitoring(applicationContext)
            if (!started) {
                prefs.monitoringWorkFallback = true
                MonitoringScheduler.schedulePeriodic(applicationContext, MonitoringScheduler.FAST_INTERVAL_MIN)
                NotificationHelper.sendStatusNotification(
                    context = applicationContext,
                    title = "Monitoring continues: reduced cadence",
                    message = "Android blocked the fast loop in the background. Checks keep running every 15 minutes automatically; open the app once to restore fast mode."
                )
            } else {
                prefs.monitoringWorkFallback = false
            }
        }
        return Result.success()
    }
}
