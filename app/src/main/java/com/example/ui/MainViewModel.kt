package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IcaiCatalog
import com.example.data.db.AppDatabase
import com.example.data.db.BatchDao
import com.example.data.db.BatchEntity
import com.example.data.db.CheckLogEntity
import com.example.data.model.BatchTarget
import com.example.data.model.DropdownOption
import com.example.data.model.ScraperResult
import com.example.data.repository.BatchParser
import com.example.data.repository.IcaiScraperRepository
import com.example.data.repository.UserPreferences
import com.example.service.BatchMonitorService
import com.example.service.MonitoringScheduler
import com.example.service.NotificationHelper
import com.example.service.TelegramHelper
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class SettingsUiState(
    val intervalMinutes: Int = 5,
    val isMonitoringPersisted: Boolean = false,
    val isMonitoringActive: Boolean = false,
    val isMonitoringWorkFallback: Boolean = false,
    val mockModeEnabled: Boolean = false,
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val telegramEnabled: Boolean = false,
    val targets: List<BatchTarget> = emptyList(),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val prefs = UserPreferences(application)
    private val scraperRepo = IcaiScraperRepository

    val batchesFlow: StateFlow<List<BatchEntity>> = db.batchDao().getAllBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logsFlow: StateFlow<List<CheckLogEntity>> = db.checkLogDao().getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeMode = MutableStateFlow(parseThemeMode(prefs.themeMode))
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _settingsBase = MutableStateFlow(readSettingsFromPrefs())

    // UI shows the service's live state; prefs only serve as persistence.
    val settingsState: StateFlow<SettingsUiState> = combine(
        _settingsBase,
        BatchMonitorService.isMonitoringLive
    ) { base, live ->
        // Fast mode: trust the running service, plus the degraded WorkManager
        // fallback (checks run at 15m while the loop could not be restarted).
        // Periodic mode: WorkManager's schedule survives reboot, so the
        // persisted flag is the truth there.
        base.copy(isMonitoringActive = live || (base.isMonitoringPersisted &&
            (!MonitoringScheduler.isFastInterval(base.intervalMinutes) || base.isMonitoringWorkFallback)))
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, readSettingsFromPrefs())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _regionsList = MutableStateFlow<List<DropdownOption>>(emptyList())
    val regionsList: StateFlow<List<DropdownOption>> = _regionsList.asStateFlow()

    private val _regionsOffline = MutableStateFlow(false)
    val regionsOffline: StateFlow<Boolean> = _regionsOffline.asStateFlow()

    private val _pouList = MutableStateFlow<List<DropdownOption>>(emptyList())
    val pouList: StateFlow<List<DropdownOption>> = _pouList.asStateFlow()

    private val _pousOffline = MutableStateFlow(false)
    val pousOffline: StateFlow<Boolean> = _pousOffline.asStateFlow()

    private val _isLoadingPous = MutableStateFlow(false)
    val isLoadingPous: StateFlow<Boolean> = _isLoadingPous.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadRegions()
        refreshPouList()
    }

    private fun applyPouResult(regionValue: String, res: ScraperResult<List<DropdownOption>>) {
        when (res) {
            is ScraperResult.Success -> {
                _pouList.value = res.data
                _pousOffline.value = false
            }
            is ScraperResult.Error -> {
                // Keep the picker usable offline, but label the list as bundled.
                _pouList.value = IcaiCatalog.fallbackPousForRegion(regionValue)
                _pousOffline.value = true
                _statusMessage.value = "Could not load centers from ICAI, showing bundled offline list. ${res.message}"
            }
        }
    }

    private fun refreshPouList() {
        viewModelScope.launch {
            _isLoadingPous.value = true
            applyPouResult(prefs.regionValue, scraperRepo.fetchPousForRegion(prefs.regionValue))
            _isLoadingPous.value = false
        }
    }

    private fun parseThemeMode(name: String): AppThemeMode =
        runCatching { AppThemeMode.valueOf(name) }.getOrDefault(AppThemeMode.SYSTEM)

    private fun readSettingsFromPrefs(): SettingsUiState {
        return SettingsUiState(
            intervalMinutes = prefs.intervalMinutes,
            isMonitoringPersisted = prefs.isMonitoringActive,
            isMonitoringActive = prefs.isMonitoringActive,
            isMonitoringWorkFallback = prefs.monitoringWorkFallback,
            mockModeEnabled = prefs.mockModeEnabled,
            telegramBotToken = prefs.telegramBotToken,
            telegramChatId = prefs.telegramChatId,
            telegramEnabled = prefs.telegramEnabled,
            targets = prefs.getTargets(),
            themeMode = parseThemeMode(prefs.themeMode)
        )
    }

    private fun refreshSettings() {
        _settingsBase.value = readSettingsFromPrefs()
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.themeMode = mode.name
        _themeMode.value = mode
        refreshSettings()
        _statusMessage.value = "Switched to ${mode.title} mode"
    }

    fun cycleThemeMode() {
        val nextMode = when (_themeMode.value) {
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.AMOLED
            AppThemeMode.AMOLED -> AppThemeMode.LIGHT
        }
        setThemeMode(nextMode)
    }

    fun loadRegions() {
        viewModelScope.launch {
            when (val res = scraperRepo.fetchInitialRegions()) {
                is ScraperResult.Success -> {
                    _regionsList.value = res.data
                    _regionsOffline.value = false
                }
                is ScraperResult.Error -> {
                    // Keep the picker usable offline, but label the list as bundled.
                    _regionsList.value = IcaiCatalog.FALLBACK_REGIONS
                    _regionsOffline.value = true
                    _statusMessage.value = "Could not load regions from ICAI, showing bundled offline list. ${res.message}"
                }
            }
        }
    }

    fun loadPousForRegion(regionValue: String) {
        viewModelScope.launch {
            _isLoadingPous.value = true
            applyPouResult(regionValue, scraperRepo.fetchPousForRegion(regionValue))
            _isLoadingPous.value = false
        }
    }

    fun toggleMonitoring() {
        val app = getApplication<Application>()
        // Act on what the UI shows: after process recreation the live flow is
        // false (service dead), so the button means "Start" — never stop.
        if (settingsState.value.isMonitoringActive) {
            MonitoringScheduler.stop(app)
            prefs.isMonitoringActive = false
            prefs.monitoringWorkFallback = false
            _statusMessage.value = "Monitoring stopped."
        } else {
            val started = MonitoringScheduler.start(app, prefs.intervalMinutes)
            if (started) {
                prefs.isMonitoringActive = true
                _statusMessage.value = if (MonitoringScheduler.isFastInterval(prefs.intervalMinutes)) {
                    "Fast mode: keeps a visible notification for exact ${prefs.intervalMinutes}m checks."
                } else {
                    "Monitoring started (every ${prefs.intervalMinutes}m, battery-friendly)."
                }
            } else {
                prefs.isMonitoringActive = false
                _statusMessage.value = "Could not start monitoring. Android blocked the background service. Retry with the app in the foreground."
            }
        }
        refreshSettings()
    }

    fun triggerCheckNow() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val startedAt = BatchMonitorService.lastCheckCompletedAt.value
            val periodicMode = settingsState.value.isMonitoringActive &&
                !MonitoringScheduler.isFastInterval(prefs.intervalMinutes)
            val started = if (periodicMode) {
                MonitoringScheduler.checkNowViaWork(app)
                true
            } else {
                BatchMonitorService.checkNow(app)
            }
            if (!started) {
                _statusMessage.value = "Could not start the check. Android blocked the service; try again with the app open."
                return@launch
            }
            _isRefreshing.value = true
            _statusMessage.value = "Checking ICAI portal for all targets..."
            // Wait for the service to signal completion (any outcome), bounded only
            // as a safety net. Previously this waited on prefs that never change on
            // network errors, freezing the spinner for the full window.
            val completed = withTimeoutOrNull(CHECK_NOW_MAX_WAIT_MS) {
                BatchMonitorService.lastCheckCompletedAt.first { it > startedAt }
            }
            if (completed == null) {
                _statusMessage.value = "Still checking. Results will appear in the Activity tab."
            }
            _isRefreshing.value = false
            refreshSettings()
        }
    }

    fun updateInterval(intervalMins: Int) {
        prefs.intervalMinutes = intervalMins
        refreshSettings()
        _statusMessage.value = "Check interval set to ${intervalMins}m." +
            if (MonitoringScheduler.isFastInterval(intervalMins)) " (fast mode: visible notification kept)" else ""
        if (prefs.isMonitoringActive) {
            // Move between fast loop and WorkManager schedule as needed.
            MonitoringScheduler.reschedule(getApplication(), intervalMins)
        }
    }

    fun setMockMode(enabled: Boolean) {
        prefs.mockModeEnabled = enabled
        refreshSettings()
        _statusMessage.value = if (enabled) "Simulation mode on: alerts use fake data." else "Simulation mode off."
    }

    fun addTarget(newTarget: BatchTarget) {
        val current = prefs.getTargets().toMutableList()
        current.add(newTarget)
        prefs.saveTargets(current)
        refreshSettings()
        _statusMessage.value = "Added monitoring target for ${newTarget.pouText}."
    }

    fun removeTarget(targetId: String) {
        val targetToRemove = prefs.getTargets().find { it.id == targetId }
        val current = prefs.getTargets().filter { it.id != targetId }
        prefs.saveTargets(current)
        if (targetToRemove != null) {
            viewModelScope.launch {
                db.batchDao().deleteBatchesByTarget(
                    targetToRemove.regionText,
                    targetToRemove.pouText,
                    targetToRemove.courseText
                )
            }
        }
        refreshSettings()
        _statusMessage.value = "Target removed."
    }

    fun toggleTargetEnabled(targetId: String) {
        val current = prefs.getTargets().map {
            if (it.id == targetId) it.copy(isEnabled = !it.isEnabled) else it
        }
        prefs.saveTargets(current)
        refreshSettings()
    }

    fun updateTelegramSettings(token: String, chatId: String, enabled: Boolean) {
        if (enabled) {
            if (!TelegramHelper.isValidToken(token)) {
                _statusMessage.value = "Bot Token format looks invalid (expected 123456:ABC-...)."
                return
            }
            if (!TelegramHelper.isValidChatId(chatId)) {
                _statusMessage.value = "Chat ID must be numeric or a public @username (use @userinfobot to find yours)."
                return
            }
        }
        prefs.telegramBotToken = token.trim()
        prefs.telegramChatId = chatId.trim()
        prefs.telegramEnabled = enabled
        refreshSettings()
        _statusMessage.value = "Telegram Bot configuration saved."
    }

    fun sendTestTelegramMessage(token: String, chatId: String) {
        viewModelScope.launch {
            if (token.isBlank() || chatId.isBlank()) {
                _statusMessage.value = "Please enter both Telegram Bot Token and Chat ID."
                return@launch
            }
            _statusMessage.value = "Sending test message to Telegram..."
            val testMessage = "<b>🤖 ICAI Batch Checker Test</b>\n\n" +
                    "Your Telegram notifications are configured successfully.\n" +
                    "You will receive live slot alerts here whenever seats open."
            val res = TelegramHelper.sendMessage(token, chatId, testMessage)
            if (res.isSuccess) {
                _statusMessage.value = "✅ Telegram message sent successfully."
            } else {
                _statusMessage.value = "❌ Telegram error: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun sendTestNotification() {
        NotificationHelper.sendTestNotification(getApplication())
        _statusMessage.value = "Test notification sent."
    }

    fun clearLogs() {
        viewModelScope.launch {
            db.checkLogDao().clearLogs()
            _statusMessage.value = "Logs cleared."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun notifyPermissionRequested() {
        _statusMessage.value = "Notifications are blocked, so slot alerts will not appear. Enable them in System Settings > Apps > ICAI Batch Checker."
    }

    companion object {
        private const val CHECK_NOW_MAX_WAIT_MS = 30_000L
    }
}
