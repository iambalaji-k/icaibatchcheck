package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldOpenSeats
import com.example.ui.theme.IcaiBatchCheckerTheme

enum class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Batches", Icons.Default.GridView, "tab_dashboard"),
    SETTINGS("Targets", Icons.Default.Tune, "tab_settings"),
    LOGS("Activity", Icons.Default.History, "tab_logs")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            IcaiBatchCheckerTheme(themeMode = currentThemeMode) {
                val context = LocalContext.current
                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
                val snackbarHostState = remember { SnackbarHostState() }

                val batches by viewModel.batchesFlow.collectAsStateWithLifecycle()
                val logs by viewModel.logsFlow.collectAsStateWithLifecycle()
                val settings by viewModel.settingsState.collectAsStateWithLifecycle()
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                val regions by viewModel.regionsList.collectAsStateWithLifecycle()
                val pouList by viewModel.pouList.collectAsStateWithLifecycle()
                val isLoadingPous by viewModel.isLoadingPous.collectAsStateWithLifecycle()
                val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

                val openBatchesCount = remember(batches) {
                    batches.count { it.isOpen }
                }

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
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "ICAI Batches",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    // Expressive status pill
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (settings.isMonitoringActive)
                                            EmeraldOpenSeats.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (settings.isMonitoringActive) EmeraldOpenSeats
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (settings.isMonitoringActive) "Active" else "Paused",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (settings.isMonitoringActive) EmeraldOpenSeats
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                // Cyclical theme mode toggle: System -> Dark -> AMOLED -> Light
                                IconButton(
                                    onClick = viewModel::cycleThemeMode,
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    val themeIcon = when (currentThemeMode) {
                                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                                        AppThemeMode.DARK -> Icons.Default.DarkMode
                                        AppThemeMode.AMOLED -> Icons.Default.Contrast
                                    }
                                    Icon(
                                        imageVector = themeIcon,
                                        contentDescription = "Theme: ${currentThemeMode.title}",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = viewModel::triggerCheckNow,
                                    enabled = !isRefreshing,
                                    modifier = Modifier.testTag("check_now_top_button")
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Check Now",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            ScreenTab.entries.forEach { tab ->
                                val isDashboard = tab == ScreenTab.DASHBOARD
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {
                                        if (isDashboard && openBatchesCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = EmeraldOpenSeats,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text("$openBatchesCount")
                                                    }
                                                }
                                            ) {
                                                Icon(tab.icon, contentDescription = tab.title)
                                            }
                                        } else {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
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
                        pous = pouList,
                        isLoadingPous = isLoadingPous,
                        onRegionChanged = viewModel::loadPousForRegion,
                        onNavigateToTargets = { currentTab = ScreenTab.SETTINGS },
                        onToggleMonitoring = viewModel::toggleMonitoring,
                        onCheckNow = viewModel::triggerCheckNow,
                        onSaveSettings = viewModel::updateSettings,
                        onAddTarget = viewModel::addTarget,
                        onRemoveTarget = viewModel::removeTarget,
                        onToggleTargetEnabled = viewModel::toggleTargetEnabled,
                        onSaveTelegramSettings = viewModel::updateTelegramSettings,
                        onTestTelegram = viewModel::sendTestTelegramMessage,
                        onSendTestNotification = viewModel::sendTestNotification,
                        onClearLogs = viewModel::clearLogs,
                        onSetThemeMode = viewModel::setThemeMode
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
    pous: List<com.example.data.model.DropdownOption> = emptyList(),
    isLoadingPous: Boolean = false,
    onRegionChanged: (String) -> Unit = {},
    onNavigateToTargets: () -> Unit = {},
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
    onClearLogs: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        when (currentTab) {
            ScreenTab.DASHBOARD -> DashboardScreen(
                batches = batches,
                settings = settings,
                isRefreshing = isRefreshing,
                onToggleMonitoring = onToggleMonitoring,
                onCheckNow = onCheckNow,
                onNavigateToTargets = onNavigateToTargets
            )
            ScreenTab.SETTINGS -> ConfigScreen(
                settings = settings,
                regions = regions,
                pous = pous,
                isLoadingPous = isLoadingPous,
                onRegionChanged = onRegionChanged,
                onSaveSettings = onSaveSettings,
                onAddTarget = onAddTarget,
                onRemoveTarget = onRemoveTarget,
                onToggleTargetEnabled = onToggleTargetEnabled,
                onSaveTelegramSettings = onSaveTelegramSettings,
                onTestTelegram = onTestTelegram,
                onSendTestNotification = onSendTestNotification,
                onSetThemeMode = onSetThemeMode
            )
            ScreenTab.LOGS -> LogsScreen(
                logs = logs,
                onClearLogs = onClearLogs
            )
        }
    }
}

