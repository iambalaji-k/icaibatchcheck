package com.example.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Single entry point for "start/stop/resume monitoring".
 *
 * - intervalMinutes >= 15: WorkManager periodic work. It survives reboot,
 *   app updates and process death on its own; each run holds a foreground
 *   service only for the seconds of the actual scrape.
 * - intervalMinutes < 15 ("fast mode"): the looping foreground service, made
 *   resilient with START_STICKY, boot resume and quota auto-resume.
 */
object MonitoringScheduler {

    private const val PERIODIC_WORK = "icai_batch_check_periodic"
    private const val RESUME_WORK = "icai_batch_check_resume"
    const val FAST_INTERVAL_MIN = 15

    fun isFastInterval(intervalMinutes: Int): Boolean = intervalMinutes < FAST_INTERVAL_MIN

    fun start(context: Context, intervalMinutes: Int): Boolean {
        return if (isFastInterval(intervalMinutes)) {
            cancelPeriodic(context)
            BatchMonitorService.startMonitoring(context)
        } else {
            BatchMonitorService.stopMonitoring(context)
            schedulePeriodic(context, intervalMinutes)
            true
        }
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(RESUME_WORK)
        BatchMonitorService.stopMonitoring(context)
    }

    /** Called when the interval changes while monitoring is active. */
    fun reschedule(context: Context, intervalMinutes: Int) {
        start(context, intervalMinutes)
    }

    /** Immediate one-off check via work (periodic-mode "Check Now"). */
    fun checkNowViaWork(context: Context) {
        val request = OneTimeWorkRequestBuilder<BatchCheckWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "icai_batch_check_now",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedulePeriodic(context: Context, intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<BatchCheckWorker>(
            intervalMinutes.coerceAtLeast(FAST_INTERVAL_MIN).toLong(), TimeUnit.MINUTES
        ).setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }

    /**
     * Fast-mode resume: a BOOT_COMPLETED receiver may not launch a foreground
     * service directly, but WorkManager can — and WorkManager itself restores
     * pending work after boot. Used for boot AND for the Android 14+ dataSync
     * quota gap (re-arm the loop ~20 minutes after the daily limit is hit).
     */
    fun scheduleFastLoopResume(context: Context, delayMinutes: Long) {
        val request = OneTimeWorkRequestBuilder<BatchMonitorResumeWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RESUME_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
