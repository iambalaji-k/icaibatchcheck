package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.UserPreferences

/**
 * Restores monitoring after device reboot or an app update. Periodic (>=15m)
 * mode needs nothing here — WorkManager reschedules itself — but the fast-mode
 * loop service cannot be started straight from BOOT_COMPLETED on Android 12+,
 * so a one-time WorkManager job re-arms it a minute later.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val prefs = UserPreferences(context)
                if (prefs.isMonitoringActive) {
                    if (MonitoringScheduler.isFastInterval(prefs.intervalMinutes)) {
                        prefs.monitoringStartedAt = 0L
                        MonitoringScheduler.scheduleFastLoopResume(context, delayMinutes = 1)
                    }
                    // periodic mode: WorkManager already restored the schedule
                }
            }
        }
    }
}
