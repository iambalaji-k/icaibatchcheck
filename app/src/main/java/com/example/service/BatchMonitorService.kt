package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.CheckLogEntity
import com.example.data.repository.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BatchMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var prefs: UserPreferences
    private lateinit var db: AppDatabase
    private var loopJob: Job? = null

    // One scrape at a time: concurrent checks would interleave ASP.NET
    // __VIEWSTATE posts on the shared session and Room prune/insert batches.
    private val checkMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        prefs = UserPreferences(this)
        db = AppDatabase.getDatabase(this)
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // System-initiated restart (intent == null): resume the loop when the
        // user still has monitoring on. Monitoring must never stay dead.
        if (intent == null) {
            if (prefs.isMonitoringActive) {
                startForegroundWithNotification("Monitoring active every ${prefs.intervalMinutes}m...")
                if (prefs.monitoringWorkFallback) {
                    // Loop is back after a degraded stretch: stop the extra work schedule.
                    prefs.monitoringWorkFallback = false
                    MonitoringScheduler.cancelPeriodic(this)
                }
                _isMonitoringLive.value = true
                startLoop()
            } else {
                stopSelf()
            }
            return START_STICKY
        }

        when (val action = intent.action) {
            ACTION_START -> {
                // Every start path must enter the foreground synchronously, otherwise
                // Android 8+ kills the process with a RemoteServiceException.
                startForegroundWithNotification("Monitoring active every ${prefs.intervalMinutes}m...")
                prefs.monitoringStartedAt = System.currentTimeMillis()
                prefs.isMonitoringActive = true
                if (prefs.monitoringWorkFallback) {
                    // Loop is back: drop the degraded periodic schedule.
                    prefs.monitoringWorkFallback = false
                    MonitoringScheduler.cancelPeriodic(this)
                }
                _isMonitoringLive.value = true
                startLoop()
            }
            ACTION_STOP -> {
                // Not calling startForeground here: a service brought up via
                // startService() has no 5s deadline, and "flash then remove" would
                // just blink the notification at the user while pausing.
                shutdownMonitoring()
            }
            ACTION_CHECK_NOW -> {
                startForegroundWithNotification("Checking now...")
                serviceScope.launch {
                    try {
                        performBatchCheck("Manual Check")
                    } catch (e: Exception) {
                        Log.e(TAG, "Manual check failed", e)
                    } finally {
                        if (!prefs.isMonitoringActive) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }
                }
            }
            else -> {
                if (!prefs.isMonitoringActive) stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app from recents must NOT stop monitoring; only an
        // explicit ACTION_STOP (in-app Pause) does.
        if (!prefs.isMonitoringActive && loopJob == null) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun shutdownMonitoring() {
        prefs.isMonitoringActive = false
        prefs.monitoringStartedAt = 0L
        prefs.monitoringWorkFallback = false
        _isMonitoringLive.value = false
        stopLoop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Belt and braces: some OEM shells leave the ongoing card posted after
        // stopForeground when the service dies in the same stroke.
        NotificationHelper.cancelForegroundNotification(this)
        stopSelf()
    }

    private fun startForegroundWithNotification(statusText: String, isChecking: Boolean = false) {
        val notification = NotificationHelper.buildForegroundNotification(
            context = this,
            statusText = statusText,
            isChecking = isChecking,
            intervalMinutes = if (prefs.isMonitoringActive) prefs.intervalMinutes else 0
        )
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    AlertPolicy.exceededDataSyncQuota(prefs.monitoringStartedAt, System.currentTimeMillis())) {
                    // Android 14+ caps dataSync FGS at ~6h/24h. Instead of dying,
                    // pause the loop and let WorkManager re-arm it shortly after
                    // the quota window — monitoring resumes on its own.
                    NotificationHelper.sendStatusNotification(
                        context = this@BatchMonitorService,
                        title = "Resuming soon",
                        message = "Daily system limit reached — monitoring auto-resumes in ~20 minutes. No action needed."
                    )
                    MonitoringScheduler.scheduleFastLoopResume(this@BatchMonitorService, delayMinutes = 20)
                    prefs.monitoringStartedAt = 0L
                    stopLoop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    NotificationHelper.cancelForegroundNotification(this@BatchMonitorService)
                    stopSelf()
                    return@launch
                }
                try {
                    performBatchCheck("Periodic Check")
                } catch (e: Exception) {
                    Log.e(TAG, "Periodic check failed", e)
                    try {
                        db.checkLogDao().insertLog(
                            CheckLogEntity(
                                timestamp = System.currentTimeMillis(),
                                status = "ERROR",
                                message = "[Periodic Check] Unexpected error: ${e.localizedMessage ?: "see logs"}",
                                regionName = "-",
                                pouName = "-",
                                courseName = "-",
                                openBatchesCount = 0
                            )
                        )
                    } catch (_: Exception) {}
                }
                val delayMs = (prefs.intervalMinutes.coerceAtLeast(1) * 60 * 1000L)
                delay(delayMs)
            }
        }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    private suspend fun performBatchCheck(source: String) = checkMutex.withLock {
        BatchCheckEngine.runCheck(
            context = this@BatchMonitorService,
            source = source,
            onStatus = { text, checking -> startForegroundWithNotification(text, checking) }
        )
    }


    override fun onDestroy() {
        super.onDestroy()
        _isMonitoringLive.value = false
        // Some OEM shells keep the ongoing card after stopForeground(); cancelling
        // at destruction is the guaranteed removal path.
        NotificationHelper.cancelForegroundNotification(this)
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "BatchMonitorService"

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_CHECK_NOW = "com.example.service.ACTION_CHECK_NOW"

        private val _isMonitoringLive = MutableStateFlow(false)

        /** Authoritative fast-mode state; UI combines this with the periodic schedule. */
        val isMonitoringLive: StateFlow<Boolean> = _isMonitoringLive.asStateFlow()

        /** Check-completion signal lives in the engine (shared by service + worker). */
        val lastCheckCompletedAt: StateFlow<Long> = BatchCheckEngine.lastCheckCompletedAt

        fun startMonitoring(context: Context): Boolean {
            val intent = Intent(context, BatchMonitorService::class.java).apply {
                action = ACTION_START
            }
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException on Android 12+/14+
                Log.e(TAG, "startMonitoring failed", e)
                false
            }
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, BatchMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "stopMonitoring failed", e)
                clearMonitoringPrefs(context)
            }
        }

        private fun clearMonitoringPrefs(context: Context) {
            UserPreferences(context).apply {
                isMonitoringActive = false
                monitoringStartedAt = 0L
            }
            _isMonitoringLive.value = false
        }

        fun checkNow(context: Context): Boolean {
            val intent = Intent(context, BatchMonitorService::class.java).apply {
                action = ACTION_CHECK_NOW
            }
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "checkNow failed", e)
                false
            }
        }
    }
}
