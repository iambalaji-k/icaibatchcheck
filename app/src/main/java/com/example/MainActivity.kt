package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.theme.IcaiBatchCheckerTheme

enum class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Live Batches", Icons.Default.Dashboard, "tab_dashboard"),
    SETTINGS("Settings", Icons.Default.Settings, "tab_settings"),
    LOGS("History Logs", Icons.Default.History, "tab_logs")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IcaiBatchCheckerTheme {
                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
                val snackbarHostState = remember { SnackbarHostState() }

                val batches by viewModel.batchesFlow.collectAsStateWithLifecycle()
                val logs by viewModel.logsFlow.collectAsStateWithLifecycle()
                val settings by viewModel.settingsState.collectAsStateWithLifecycle()
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                val regions by viewModel.regionsList.collectAsStateWithLifecycle()
                val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

                // Dynamic Notification Permission Request (Android 13+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                LaunchedEffect(statusMessage) {
                    statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "ICAI Batch Checker",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            ScreenTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    ScaffoldContent(
                        modifier = Modifier.padding(innerPadding),
                        currentTab = currentTab,
                        batches = batches,
                        logs = logs,
                        settings = settings,
                        isRefreshing = isRefreshing,
                        regions = regions,
                        onToggleMonitoring = viewModel::toggleMonitoring,
                        onCheckNow = viewModel::triggerCheckNow,
                        onSaveSettings = viewModel::updateSettings,
                        onAddTarget = viewModel::addTarget,
                        onRemoveTarget = viewModel::removeTarget,
                        onToggleTargetEnabled = viewModel::toggleTargetEnabled,
                        onSaveTelegramSettings = viewModel::updateTelegramSettings,
                        onTestTelegram = viewModel::sendTestTelegramMessage,
                        onSendTestNotification = viewModel::sendTestNotification,
                        onClearLogs = viewModel::clearLogs
                    )
                }
            }
        }
    }
}

@Composable
fun ScaffoldContent(
    modifier: Modifier = Modifier,
    currentTab: ScreenTab,
    batches: List<com.example.data.db.BatchEntity>,
    logs: List<com.example.data.db.CheckLogEntity>,
    settings: com.example.ui.SettingsUiState,
    isRefreshing: Boolean,
    regions: List<com.example.data.model.DropdownOption>,
    onToggleMonitoring: () -> Unit,
    onCheckNow: () -> Unit,
    onSaveSettings: (
        regionVal: String,
        regionTxt: String,
        pouVal: String,
        pouTxt: String,
        courseVal: String,
        courseTxt: String,
        intervalMins: Int,
        mockMode: Boolean
    ) -> Unit,
    onAddTarget: (com.example.data.model.BatchTarget) -> Unit,
    onRemoveTarget: (String) -> Unit,
    onToggleTargetEnabled: (String) -> Unit,
    onSaveTelegramSettings: (token: String, chatId: String, enabled: Boolean) -> Unit,
    onTestTelegram: (token: String, chatId: String) -> Unit,
    onSendTestNotification: () -> Unit,
    onClearLogs: () -> Unit
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        when (currentTab) {
            ScreenTab.DASHBOARD -> DashboardScreen(
                batches = batches,
                settings = settings,
                isRefreshing = isRefreshing,
                onToggleMonitoring = onToggleMonitoring,
                onCheckNow = onCheckNow
            )
            ScreenTab.SETTINGS -> ConfigScreen(
                settings = settings,
                regions = regions,
                onSaveSettings = onSaveSettings,
                onAddTarget = onAddTarget,
                onRemoveTarget = onRemoveTarget,
                onToggleTargetEnabled = onToggleTargetEnabled,
                onSaveTelegramSettings = onSaveTelegramSettings,
                onTestTelegram = onTestTelegram,
                onSendTestNotification = onSendTestNotification
            )
            ScreenTab.LOGS -> LogsScreen(
                logs = logs,
                onClearLogs = onClearLogs
            )
        }
    }
}
