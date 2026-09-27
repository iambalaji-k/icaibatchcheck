package com.example.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.BatchEntity
import com.example.data.db.CheckLogEntity
import com.example.data.model.ScraperResult
import com.example.data.repository.BatchParser
import com.example.data.repository.IcaiScraperRepository
import com.example.data.repository.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The actual seat-check pipeline, shared by the looping foreground service
 * (fast intervals) and the WorkManager worker (periodic / reboot-resilient).
 * Pure orchestration: scrape -> dedupe alerts -> persist -> notify.
 */
object BatchCheckEngine {

    private const val TAG = "BatchCheckEngine"

    private val _lastCheckCompletedAt = MutableStateFlow(0L)

    /** Wall-clock of the last finished check (any outcome); UI waits on this. */
    val lastCheckCompletedAt: StateFlow<Long> = _lastCheckCompletedAt.asStateFlow()

    /**
     * @param onStatus callback for user-visible progress; the looping service
     * routes this to its foreground notification, the worker to its own.
     */
    suspend fun runCheck(
        context: Context,
        source: String,
        onStatus: (String, Boolean) -> Unit = { _, _ -> }
    ) {
        val prefs = UserPreferences(context)
        val db = AppDatabase.getDatabase(context)
        try {
            val activeTargets = prefs.getTargets().filter { it.isEnabled }
            if (activeTargets.isEmpty()) {
                db.checkLogDao().insertLog(
                    CheckLogEntity(
                        timestamp = System.currentTimeMillis(),
                        status = "NO_TARGETS",
                        message = "[$source] No active targets configured. Add a target in the Targets tab.",
                        regionName = "-",
                        pouName = "-",
                        courseName = "-",
                        openBatchesCount = 0
                    )
                )
                onStatus("No active targets. Add one in the Targets tab.", false)
                return
            }

            val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ICAI:BatchMonitorWakeLock")
            // Worst case: each target performs 3 requests with capped timeouts.
            wakeLock?.acquire(60_000L * activeTargets.size.coerceAtMost(6))

            try {
                onStatus("Checking ${activeTargets.size} target(s)...", true)
                val mockMode = prefs.mockModeEnabled
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                var totalOpenAcrossTargets = 0
                var errorCount = 0

                for (target in activeTargets) {
                    val regVal = target.regionValue
                    val regTxt = target.regionText
                    val pouVal = target.pouValue
                    val pouTxt = target.pouText
                    val crsVal = target.courseValue
                    val crsTxt = target.courseText

                    val result = IcaiScraperRepository.checkBatches(
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
                            val currentIds = mutableSetOf<String>()

                            // Room renders an empty collection as "IN ()", which SQLite
                            // rejects — skip the lookup when the scrape found nothing.
                            val existing = if (batchList.isEmpty()) emptyMap()
                            else db.batchDao().getBatchesByIds(
                                batchList.map { b ->
                                    BatchParser.buildBatchId(regTxt, pouTxt, crsTxt, b.batchName)
                                }
                            ).associateBy { it.id }

                            for (b in batchList) {
                                val batchId = BatchParser.buildBatchId(regTxt, pouTxt, crsTxt, b.batchName)
                                currentIds.add(batchId)
                                val prevAvailable = existing[batchId]?.availableSeats ?: 0

                                if (b.availableSeats > 0) {
                                    openCount++
                                    if (AlertPolicy.shouldAlert(prevAvailable, b.availableSeats)) {
                                        NotificationHelper.sendSlotOpenNotification(
                                            context = context,
                                            batchName = b.batchName,
                                            availableSeats = b.availableSeats,
                                            pouName = pouTxt,
                                            courseName = crsTxt,
                                            simulated = mockMode
                                        )

                                        if (prefs.telegramEnabled && prefs.telegramBotToken.isNotBlank() && prefs.telegramChatId.isNotBlank()) {
                                            val telegramMsg = TelegramHelper.buildAlertMessage(
                                                course = crsTxt,
                                                pou = pouTxt,
                                                region = regTxt,
                                                batchName = b.batchName,
                                                availableSeats = b.availableSeats,
                                                dates = b.dates,
                                                simulated = mockMode
                                            )
                                            TelegramHelper.sendMessage(
                                                botToken = prefs.telegramBotToken,
                                                chatId = prefs.telegramChatId,
                                                text = telegramMsg
                                            ).onFailure { err ->
                                                Log.w(TAG, "Telegram alert delivery failed", err)
                                                try {
                                                    db.checkLogDao().insertLog(
                                                        CheckLogEntity(
                                                            timestamp = System.currentTimeMillis(),
                                                            status = "ERROR",
                                                            message = "[Telegram] Alert not delivered for ${b.batchName}: ${err.localizedMessage ?: err.message}",
                                                            regionName = regTxt,
                                                            pouName = pouTxt,
                                                            courseName = crsTxt,
                                                            openBatchesCount = 0
                                                        )
                                                    )
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                }

                                entities.add(
                                    BatchEntity(
                                        id = batchId,
                                        batchName = b.batchName,
                                        totalSeats = b.totalSeats,
                                        availableSeats = b.availableSeats,
                                        knownCapacity = b.knownCapacity,
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
                                db.batchDao().deleteStaleBatches(
                                    regionText = regTxt,
                                    pouText = pouTxt,
                                    courseText = crsTxt,
                                    keepIds = currentIds.toList()
                                )
                            } else {
                                db.batchDao().deleteBatchesByTarget(regTxt, pouTxt, crsTxt)
                            }

                            totalOpenAcrossTargets += openCount

                            val logMsg = if (openCount > 0) {
                                "🎉 $openCount open batch(es) in $pouTxt ($crsTxt)"
                            } else {
                                "Checked $pouTxt ($crsTxt), no seats available."
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
                            errorCount++
                            db.checkLogDao().insertLog(
                                CheckLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    status = "ERROR",
                                    message = "[$source Error] ${result.message}",
                                    regionName = regTxt,
                                    pouName = pouTxt,
                                    courseName = crsTxt,
                                    openBatchesCount = 0
                                )
                            )
                        }
                    }
                }

                val statusSummary = when {
                    totalOpenAcrossTargets > 0 -> "🎉 $totalOpenAcrossTargets open batch(es) found across active targets."
                    errorCount > 0 && errorCount == activeTargets.size -> "Check failed: ICAI server unreachable"
                    errorCount > 0 -> "Checked with errors: ICAI server unreachable for some targets"
                    else -> "Checked ${activeTargets.size} target(s) at $timeStr, no open seats."
                }
                onStatus(statusSummary, false)
            } finally {
                if (wakeLock?.isHeld == true) {
                    try {
                        wakeLock.release()
                    } catch (_: Exception) {}
                }
            }
        } finally {
            _lastCheckCompletedAt.value = System.currentTimeMillis()
        }
    }
}
