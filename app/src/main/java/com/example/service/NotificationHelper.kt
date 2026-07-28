package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID_ALERTS = "icai_batch_alerts"
    const val CHANNEL_ID_SERVICE = "icai_foreground_monitor"
    const val NOTIFICATION_ID_FOREGROUND = 1001
    private const val NOTIFICATION_ID_SLOT_ALERT = 2002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Alert Channel for high priority slot openings
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "ICAI Batch Open Slot Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sends instant push notifications when ICAI course seats become available."
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(alertChannel)

            // Silent Foreground Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "ICAI Background Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays ongoing status while background slot checking is active."
                setSound(null, null)
            }
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildForegroundNotification(context: Context, statusText: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setContentTitle("ICAI Batch Checker Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun sendSlotOpenNotification(
        context: Context,
        batchName: String,
        availableSeats: Int,
        pouName: String,
        courseName: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎉 ICAI Slot Open ($availableSeats Seat${if (availableSeats > 1) "s" else ""})!"
        val message = "[$pouName] $batchName has $availableSeats available slot(s)! Tap to open app & register now."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_SLOT_ALERT + (0..999).random(), builder.build())
    }

    fun sendTestNotification(context: Context) {
        sendSlotOpenNotification(
            context = context,
            batchName = "Chennai AICITSS BATCH #102 (TEST)",
            availableSeats = 3,
            pouName = "Chennai",
            courseName = "AICITSS"
        )
    }
}
