package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.data.db.AppDatabase
import com.example.data.db.BatchEntity
import com.example.data.db.CheckLogEntity
import com.example.data.model.ScraperResult
import com.example.data.repository.IcaiScraperRepository
import com.example.data.repository.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatchMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var prefs: UserPreferences
    private lateinit var db: AppDatabase
    private val scraperRepo = IcaiScraperRepository()
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = UserPreferences(this)
        db = AppDatabase.getDatabase(this)
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                prefs.isMonitoringActive = true
                startForegroundWithNotification("Monitoring active every ${prefs.intervalMinutes}m...")
                startLoop()
            }
            ACTION_STOP -> {
                prefs.isMonitoringActive = false
                stopLoop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CHECK_NOW -> {
                serviceScope.launch {
                    performBatchCheck("Manual Check")
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification(statusText: String) {
        val notification = NotificationHelper.buildForegroundNotification(this, statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } catch (_: Exception) {
                startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
            }
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
        }
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = serviceScope.launch {
            while (isActive && prefs.isMonitoringActive) {
                performBatchCheck("Periodic Check")
                val delayMs = (prefs.intervalMinutes.coerceAtLeast(1) * 60 * 1000L)
                delay(delayMs)
            }
        }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    private suspend fun performBatchCheck(source: String) {
        val activeTargets = prefs.getTargets().filter { it.isEnabled }
        if (activeTargets.isEmpty()) {
            db.checkLogDao().insertLog(
                CheckLogEntity(
                    timestamp = System.currentTimeMillis(),
                    status = "NO_TARGETS",
                    message = "[$source] No active targets configured. Add a target in Settings.",
                    regionName = "-",
                    pouName = "-",
                    courseName = "-",
                    openBatchesCount = 0
                )
            )
            startForegroundWithNotification("No active targets. Add target in Settings.")
            return
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ICAI:BatchMonitorWakeLock"
        )
        wakeLock?.acquire(30 * 1000L)

        try {
            val targetsToCheck = activeTargets
            val mockMode = prefs.mockModeEnabled
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            var totalOpenAcrossTargets = 0
            var summaryPouList = mutableListOf<String>()

            for (target in targetsToCheck) {
                val regVal = target.regionValue
                val regTxt = target.regionText
                val pouVal = target.pouValue
                val pouTxt = target.pouText
                val crsVal = target.courseValue
                val crsTxt = target.courseText

                val result = scraperRepo.checkBatches(
                    regionValue = regVal,
                    regionText = regTxt,
                    pouValue = pouVal,
                    pouText = pouTxt,
                    courseValue = crsVal,
                    courseText = crsTxt,
                    forceMock = mockMode
                )

                when (result) {
                    is ScraperResult.Success -> {
                        val batchList = result.data
                        val timestamp = System.currentTimeMillis()
                        var openCount = 0

                        val entities = mutableListOf<BatchEntity>()

                        for (b in batchList) {
                            val batchId = "${regTxt}_${pouTxt}_${crsTxt}_${b.batchName}".lowercase(Locale.ROOT)
                            val existing = db.batchDao().getBatchById(batchId)
                            val prevAvailable = existing?.availableSeats ?: 0

                            if (b.availableSeats > 0) {
                                openCount++
                                // Check if newly opened or seat count increased
                                if (prevAvailable == 0 || b.availableSeats > prevAvailable) {
                                    // 1. Android Push Notification
                                    NotificationHelper.sendSlotOpenNotification(
                                        context = this,
                                        batchName = b.batchName,
                                        availableSeats = b.availableSeats,
                                        pouName = pouTxt,
                                        courseName = crsTxt
                                    )

                                    // 2. Telegram Notification
                                    if (prefs.telegramEnabled && prefs.telegramBotToken.isNotBlank() && prefs.telegramChatId.isNotBlank()) {
                                        val telegramMsg = "<b>🚨 ICAI SLOT OPEN ALERT!</b>\n\n" +
                                                "<b>Course:</b> ${crsTxt}\n" +
                                                "<b>POU / City:</b> ${pouTxt} (${regTxt})\n" +
                                                "<b>Batch:</b> ${b.batchName}\n" +
                                                "<b>Available Seats:</b> <b>${b.availableSeats}</b>\n" +
                                                "<b>Dates:</b> ${b.dates}\n\n" +
                                                "🔗 <a href='https://www.icaionlineregistration.org/LaunchBatchDetail.aspx'>Register on ICAI Portal Now</a>"
                                        TelegramHelper.sendMessage(
                                            botToken = prefs.telegramBotToken,
                                            chatId = prefs.telegramChatId,
                                            text = telegramMsg
                                        )
                                    }
                                }
                            }

                            entities.add(
                                BatchEntity(
                                    id = batchId,
                                    batchName = b.batchName,
                                    totalSeats = b.totalSeats,
                                    availableSeats = b.availableSeats,
                                    dates = b.dates,
                                    timings = b.timings,
                                    venue = b.venue,
                                    fee = b.fee,
                                    regionName = regTxt,
                                    pouName = pouTxt,
                                    courseName = crsTxt,
                                    lastCheckedTimestamp = timestamp,
                                    isOpen = b.availableSeats > 0
                                )
                            )
                        }

                        if (entities.isNotEmpty()) {
                            db.batchDao().insertBatches(entities)
                        }

                        totalOpenAcrossTargets += openCount
                        summaryPouList.add("$pouTxt ($openCount open)")

                        val logMsg = if (openCount > 0) {
                            "🎉 $openCount OPEN BATCH(ES) in $pouTxt ($crsTxt)"
                        } else {
                            "Checked $pouTxt ($crsTxt) — No seats available."
                        }

                        db.checkLogDao().insertLog(
                            CheckLogEntity(
                                timestamp = timestamp,
                                status = if (openCount > 0) "ALERT_TRIGGERED" else "NO_SEATS",
                                message = "[$source] $logMsg",
                                regionName = regTxt,
                                pouName = pouTxt,
                                courseName = crsTxt,
                                openBatchesCount = openCount
                            )
                        )

                        prefs.lastCheckTime = timestamp
                    }
                    is ScraperResult.Error -> {
                        val errorMsg = result.message
                        db.checkLogDao().insertLog(
                            CheckLogEntity(
                                timestamp = System.currentTimeMillis(),
                                status = "ERROR",
                                message = "[$source Error] $errorMsg",
                                regionName = regTxt,
                                pouName = pouTxt,
                                courseName = crsTxt,
                                openBatchesCount = 0
                            )
                        )
                    }
                }
            }

            val statusSummary = if (totalOpenAcrossTargets > 0) {
                "🎉 $totalOpenAcrossTargets OPEN BATCH(ES) FOUND across active targets!"
            } else {
                "Checked ${targetsToCheck.size} target(s) at $timeStr — No open seats."
            }
            startForegroundWithNotification(statusSummary)
        } finally {
            if (wakeLock?.isHeld == true) {
                try {
                    wakeLock.release()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_CHECK_NOW = "com.example.service.ACTION_CHECK_NOW"

        fun startMonitoring(context: Context) {
            val intent = Intent(context, BatchMonitorService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Handle ForegroundServiceStartNotAllowedException on Android 12+/14+ gracefully
                e.printStackTrace()
            }
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, BatchMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun checkNow(context: Context) {
            val intent = Intent(context, BatchMonitorService::class.java).apply {
                action = ACTION_CHECK_NOW
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
