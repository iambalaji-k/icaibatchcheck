package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistudio.icaibatchchecker.R
import com.example.MainActivity
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {

    const val CHANNEL_ID_ALERTS = "icai_batch_alerts"
    const val CHANNEL_ID_SERVICE = "icai_foreground_monitor"
    const val NOTIFICATION_ID_FOREGROUND = 1001

    private const val GROUP_KEY_ALERTS = "com.example.icai.SLOT_ALERTS"
    private const val NOTIFICATION_ID_GROUP_SUMMARY = 3001
    private const val PAUSE_ACTION_REQUEST_CODE = 77
    private const val SLOT_ALERT_ID_BASE = 2002

    // Sequential, ever-increasing IDs: no collisions, each alert is its own notification.
    private val nextAlertId = AtomicInteger(SLOT_ALERT_ID_BASE)

    private fun mainActivityPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceActionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, BatchMonitorService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannelGroup(
                NotificationChannelGroup(GROUP_KEY_ALERTS, "Slot Alerts")
            )

            // Alert Channel for high priority slot openings
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Open slot alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = GROUP_KEY_ALERTS
                description = "Instant alerts when ICAI course seats become available."
                enableVibration(true)
                setShowBadge(true)
                // Accent color tints the notification icon glow on modern Android.
                lightColor = Color.parseColor("#10B981")
            }
            manager.createNotificationChannel(alertChannel)

            // Silent Foreground Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Monitoring status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing status card while the monitor runs, with pause control."
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * M3 "card"-style ongoing notification: title + subtitle, determinate
     * progress while a check runs, and an inline Pause control. Status updates
     * never re-sound (setOnlyAlertOnce).
     */
    fun buildForegroundNotification(
        context: Context,
        statusText: String,
        isChecking: Boolean = false,
        intervalMinutes: Int = 0
    ): Notification {
        val contentIntent = mainActivityPendingIntent(context, 0)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ICAI Batch Checker")
            .setContentText(statusText)
            .setSubText(if (intervalMinutes > 0) "Auto-check every ${intervalMinutes}m" else "Monitoring")
            .setColor(Color.parseColor("#059669"))
            .setColorized(false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notification,
                    "Pause",
                    serviceActionPendingIntent(context, BatchMonitorService.ACTION_STOP, PAUSE_ACTION_REQUEST_CODE)
                ).build()
            )

        if (isChecking) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(0, 0, false)
        }
        return builder.build()
    }

    /**
     * Slot-open alert as a M3 conversation-style card: course header, batch
     * line, seat-count message with timestamp, and a Register action. Multiple
     * alerts stack under one group; only the summary makes noise, so a burst of
     * openings is one chime, not five.
     */
    fun sendSlotOpenNotification(
        context: Context,
        batchName: String,
        availableSeats: Int,
        pouName: String,
        courseName: String,
        simulated: Boolean = false
    ) {
        createNotificationChannels(context)

        // One id for both the PendingIntent request code and the notification,
        // so intents of different alerts never overwrite each other.
        val alertId = nextAlertId.getAndIncrement()
        val contentIntent = mainActivityPendingIntent(context, alertId)
        val prefix = if (simulated) "🧪 Simulation · " else "🎉 Slot open · "
        val seatWord = if (availableSeats == 1) "seat" else "slots"
        val headline = "$prefix$availableSeats $seatWord"
        val detail = "$batchName\n$pouName · $courseName\nTap to register before they fill up."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.parseColor("#059669"))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(headline)
                    .setSummaryText("$pouName · $courseName")
                    .bigText(detail)
            )
            .setContentTitle(headline)
            .setContentText(batchName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setGroup(GROUP_KEY_ALERTS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_notification,
                    "Open app",
                    contentIntent
                ).build()
            )
            .build()

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(alertId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — nothing to show, by design.
        }
        postGroupSummary(context)
    }

    /** Calm status card (limits reached, info); never disguised as a seat alert. */
    fun sendStatusNotification(context: Context, title: String, message: String) {
        createNotificationChannels(context)

        val statusId = nextAlertId.getAndIncrement()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(Color.parseColor("#F59E0B"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent(context, statusId))

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(statusId, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun postGroupSummary(context: Context) {
        val summary = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ICAI Batch Checker")
            .setContentText("Open slots detected — swipe to expand")
            .setColor(Color.parseColor("#059669"))
            .setGroup(GROUP_KEY_ALERTS)
            .setGroupSummary(true)
            // Children own the sound/vibration; the bundle header stays quiet.
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent(context, 0))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("ICAI Batch Checker")
                    .setSummaryText("Slot alerts")
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GROUP_SUMMARY, summary)
        } catch (_: SecurityException) {}
    }

    /** Remove the ongoing monitoring card explicitly (some OEMs keep it otherwise). */
    fun cancelForegroundNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_FOREGROUND)
    }

    fun sendTestNotification(context: Context) {
        sendSlotOpenNotification(
            context = context,
            batchName = "Chennai AICITSS BATCH #102 (TEST)",
            availableSeats = 3,
            pouName = "Chennai",
            courseName = "AICITSS",
            simulated = true
        )
    }
}
