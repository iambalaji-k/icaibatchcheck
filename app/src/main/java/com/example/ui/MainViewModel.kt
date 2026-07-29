package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BatchEntity
import com.example.data.db.CheckLogEntity
import com.example.data.model.BatchTarget
import com.example.data.model.DropdownOption
import com.example.data.model.ScraperResult
import com.example.data.repository.IcaiScraperRepository
import com.example.data.repository.UserPreferences
import com.example.service.BatchMonitorService
import com.example.service.NotificationHelper
import com.example.service.TelegramHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val regionValue: String = "4",
    val regionText: String = "Southern",
    val pouValue: String = "3",
    val pouText: String = "Chennai",
    val courseValue: String = "48",
    val courseText: String = "AICITSS - Advanced Information Technology",
    val intervalMinutes: Int = 5,
    val isMonitoringActive: Boolean = false,
    val mockModeEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val notifyOnlyNewSeats: Boolean = true,
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val telegramEnabled: Boolean = false,
    val targets: List<BatchTarget> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val prefs = UserPreferences(application)
    private val scraperRepo = IcaiScraperRepository()

    val batchesFlow: StateFlow<List<BatchEntity>> = db.batchDao().getAllBatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logsFlow: StateFlow<List<CheckLogEntity>> = db.checkLogDao().getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settingsState = MutableStateFlow(readSettingsFromPrefs())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _regionsList = MutableStateFlow<List<DropdownOption>>(emptyList())
    val regionsList: StateFlow<List<DropdownOption>> = _regionsList.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadRegions()
    }

    private fun readSettingsFromPrefs(): SettingsUiState {
        return SettingsUiState(
            regionValue = prefs.regionValue,
            regionText = prefs.regionText,
            pouValue = prefs.pouValue,
            pouText = prefs.pouText,
            courseValue = prefs.courseValue,
            courseText = prefs.courseText,
            intervalMinutes = prefs.intervalMinutes,
            isMonitoringActive = prefs.isMonitoringActive,
            mockModeEnabled = prefs.mockModeEnabled,
            soundEnabled = prefs.soundEnabled,
            notifyOnlyNewSeats = prefs.notifyOnlyNewSeats,
            telegramBotToken = prefs.telegramBotToken,
            telegramChatId = prefs.telegramChatId,
            telegramEnabled = prefs.telegramEnabled,
            targets = prefs.getTargets()
        )
    }

    fun loadRegions() {
        viewModelScope.launch {
            when (val res = scraperRepo.fetchInitialRegions()) {
                is ScraperResult.Success -> {
                    _regionsList.value = res.data
                }
                is ScraperResult.Error -> {}
            }
        }
    }

    fun toggleMonitoring() {
        val current = prefs.isMonitoringActive
        if (current) {
            BatchMonitorService.stopMonitoring(getApplication())
            prefs.isMonitoringActive = false
            _statusMessage.value = "Monitoring Stopped."
        } else {
            BatchMonitorService.startMonitoring(getApplication())
            prefs.isMonitoringActive = true
            _statusMessage.value = "Background Monitoring Started (Every ${prefs.intervalMinutes}m)."
        }
        _settingsState.value = readSettingsFromPrefs()
    }

    fun triggerCheckNow() {
        viewModelScope.launch {
            _isRefreshing.value = true
            BatchMonitorService.checkNow(getApplication())
            _statusMessage.value = "Checking ICAI portal for all targets..."
            kotlinx.coroutines.delay(1500)
            _isRefreshing.value = false
            _settingsState.value = readSettingsFromPrefs()
        }
    }

    fun updateSettings(
        regionVal: String,
        regionTxt: String,
        pouVal: String,
        pouTxt: String,
        courseVal: String,
        courseTxt: String,
        intervalMins: Int,
        mockMode: Boolean
    ) {
        prefs.regionValue = regionVal
        prefs.regionText = regionTxt
        prefs.pouValue = pouVal
        prefs.pouText = pouTxt
        prefs.courseValue = courseVal
        prefs.courseText = courseTxt
        prefs.intervalMinutes = intervalMins
        prefs.mockModeEnabled = mockMode

        _settingsState.value = readSettingsFromPrefs()
        _statusMessage.value = "Settings updated successfully."

        if (prefs.isMonitoringActive) {
            BatchMonitorService.startMonitoring(getApplication()) // restart loop with new settings
        }
    }

    fun addTarget(newTarget: BatchTarget) {
        val current = prefs.getTargets().toMutableList()
        current.add(newTarget)
        prefs.saveTargets(current)
        _settingsState.value = readSettingsFromPrefs()
        _statusMessage.value = "Added new monitoring target for ${newTarget.pouText}!"
    }

    fun removeTarget(targetId: String) {
        val targetToRemove = prefs.getTargets().find { it.id == targetId }
        val current = prefs.getTargets().filter { it.id != targetId }
        prefs.saveTargets(current)
        if (targetToRemove != null) {
            viewModelScope.launch {
                db.batchDao().deleteBatchesByPouAndCourse(targetToRemove.pouText, targetToRemove.courseText)
            }
        }
        _settingsState.value = readSettingsFromPrefs()
        _statusMessage.value = "Target removed."
    }

    fun toggleTargetEnabled(targetId: String) {
        val current = prefs.getTargets().map {
            if (it.id == targetId) it.copy(isEnabled = !it.isEnabled) else it
        }
        prefs.saveTargets(current)
        _settingsState.value = readSettingsFromPrefs()
    }

    fun updateTelegramSettings(token: String, chatId: String, enabled: Boolean) {
        prefs.telegramBotToken = token
        prefs.telegramChatId = chatId
        prefs.telegramEnabled = enabled
        _settingsState.value = readSettingsFromPrefs()
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
                    "Your Telegram notifications are configured successfully!\n" +
                    "You will receive live slot alerts here whenever seats open."
            val res = TelegramHelper.sendMessage(token, chatId, testMessage)
            if (res.isSuccess) {
                _statusMessage.value = "✅ Telegram message sent successfully!"
            } else {
                _statusMessage.value = "❌ Telegram Error: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun sendTestNotification() {
        NotificationHelper.sendTestNotification(getApplication())
        _statusMessage.value = "Test Notification Sent!"
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
}

