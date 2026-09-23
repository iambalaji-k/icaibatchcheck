package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatchTarget
import com.example.data.model.DropdownOption
import com.example.ui.SettingsUiState
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOpenSeats

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen(
    settings: SettingsUiState,
    regions: List<DropdownOption>,
    pous: List<DropdownOption> = emptyList(),
    isLoadingPous: Boolean = false,
    onRegionChanged: (String) -> Unit = {},
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
    onAddTarget: (BatchTarget) -> Unit,
    onRemoveTarget: (String) -> Unit,
    onToggleTargetEnabled: (String) -> Unit,
    onSaveTelegramSettings: (token: String, chatId: String, enabled: Boolean) -> Unit,
    onTestTelegram: (token: String, chatId: String) -> Unit,
    onSendTestNotification: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit = {}
) {
    // New target entry state
    var selectedRegionVal by remember { mutableStateOf("4") }
    var selectedRegionTxt by remember { mutableStateOf("Southern") }
    var pouTxtInput by remember { mutableStateOf("Chennai") }
    var pouValInput by remember { mutableStateOf("3") }
    var courseTxtInput by remember { mutableStateOf("AICITSS - Advanced Information Technology (Adv ITT)") }
    var courseValInput by remember { mutableStateOf("48") }

    // Settings state
    var selectedInterval by remember(settings) { mutableStateOf(settings.intervalMinutes) }
    var mockModeEnabled by remember(settings) { mutableStateOf(settings.mockModeEnabled) }

    var telegramToken by remember(settings) { mutableStateOf(settings.telegramBotToken) }
    var telegramChatId by remember(settings) { mutableStateOf(settings.telegramChatId) }
    var telegramEnabled by remember(settings) { mutableStateOf(settings.telegramEnabled) }

    var regionDropdownExpanded by remember { mutableStateOf(false) }
    var pouDropdownExpanded by remember { mutableStateOf(false) }

    // Sync POU selection whenever pous list updates
    LaunchedEffect(pous) {
        if (pous.isNotEmpty()) {
            val matching = pous.firstOrNull { it.text.equals(pouTxtInput, ignoreCase = true) }
            if (matching != null) {
                pouValInput = matching.value
                pouTxtInput = matching.text
            } else if (pouTxtInput.isBlank() || pous.none { it.value == pouValInput }) {
                pouValInput = pous.first().value
                pouTxtInput = pous.first().text
            }
        }
    }

    val regionOptions = if (regions.isNotEmpty()) regions else listOf(
        DropdownOption("1", "Central"),
        DropdownOption("2", "Eastern"),
        DropdownOption("3", "Northern"),
        DropdownOption("4", "Southern"),
        DropdownOption("5", "Western")
    )

    val popularCourses = listOf(
        DropdownOption("48", "AICITSS - Advanced Information Technology (Adv ITT)"),
        DropdownOption("45", "AICITSS - Management & Communication Skills (MCS)"),
        DropdownOption("47", "ICITSS - Information Technology Course (ITT)"),
        DropdownOption("46", "ICITSS - Orientation Course (OC)")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Active Targets
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_targets_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Monitored Targets",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${settings.targets.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (settings.targets.isEmpty()) {
                    Text(
                        text = "No targets configured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        settings.targets.forEach { target ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("target_item_${target.id}"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (target.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = target.pouText,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                Text(
                                                    text = target.regionText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = target.courseText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Switch(
                                            checked = target.isEnabled,
                                            onCheckedChange = { onToggleTargetEnabled(target.id) },
                                            modifier = Modifier.testTag("toggle_target_${target.id}")
                                        )
                                        IconButton(
                                            onClick = { onRemoveTarget(target.id) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("delete_target_${target.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Add Target
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Add Target",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Region Dropdown
                ExposedDropdownMenuBox(
                    expanded = regionDropdownExpanded,
                    onExpandedChange = { regionDropdownExpanded = !regionDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRegionTxt,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("region_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = regionDropdownExpanded,
                        onDismissRequest = { regionDropdownExpanded = false }
                    ) {
                        regionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.text) },
                                onClick = {
                                    selectedRegionVal = option.value
                                    selectedRegionTxt = option.text
                                    regionDropdownExpanded = false
                                    onRegionChanged(option.value)
                                }
                            )
                        }
                    }
                }

                // Dynamic POU / Center Dropdown (Replaces Manual Input)
                ExposedDropdownMenuBox(
                    expanded = pouDropdownExpanded,
                    onExpandedChange = { pouDropdownExpanded = !pouDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (isLoadingPous) "Loading centers..." else pouTxtInput.ifBlank { "Select Center / POU" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Center / POU City") },
                        leadingIcon = {
                            if (isLoadingPous) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pouDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("pou_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = pouDropdownExpanded,
                        onDismissRequest = { pouDropdownExpanded = false }
                    ) {
                        if (pous.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(if (isLoadingPous) "Loading centers from ICAI..." else "No centers found") },
                                onClick = { pouDropdownExpanded = false }
                            )
                        } else {
                            pous.forEach { pouOption ->
                                DropdownMenuItem(
                                    text = { Text(pouOption.text) },
                                    onClick = {
                                        pouValInput = pouOption.value
                                        pouTxtInput = pouOption.text
                                        pouDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Course Selection with FlowRow FilterChips (Teal/Emerald Selected State)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Course",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularCourses.forEach { crs ->
                            val isSelected = courseValInput == crs.value || courseTxtInput.equals(crs.text, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    courseValInput = crs.value
                                    courseTxtInput = crs.text
                                },
                                label = {
                                    Text(
                                        text = crs.text,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (pouTxtInput.isNotBlank() && courseTxtInput.isNotBlank()) {
                            onAddTarget(
                                BatchTarget(
                                    regionValue = selectedRegionVal,
                                    regionText = selectedRegionTxt,
                                    pouValue = pouValInput,
                                    pouText = pouTxtInput.trim(),
                                    courseValue = courseValInput,
                                    courseText = courseTxtInput.trim(),
                                    isEnabled = true
                                )
                            )
                        }
                    },
                    enabled = pouTxtInput.isNotBlank() && courseTxtInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_target_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Target", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 3: Check Interval
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Check Interval",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(2, 5, 10, 15, 30).forEach { mins ->
                        FilterChip(
                            selected = selectedInterval == mins,
                            onClick = {
                                selectedInterval = mins
                                onSaveSettings(
                                    selectedRegionVal,
                                    selectedRegionTxt,
                                    pouValInput,
                                    pouTxtInput,
                                    courseValInput,
                                    courseTxtInput,
                                    mins,
                                    mockModeEnabled
                                )
                            },
                            label = { Text("${mins}m") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("interval_chip_${mins}m")
                        )
                    }
                }
            }
        }

        // Section 4: Telegram Alerts
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("telegram_config_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Telegram Alerts",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Switch(
                        checked = telegramEnabled,
                        onCheckedChange = { telegramEnabled = it },
                        modifier = Modifier.testTag("telegram_switch")
                    )
                }

                if (telegramEnabled) {
                    OutlinedTextField(
                        value = telegramToken,
                        onValueChange = { telegramToken = it },
                        label = { Text("Bot Token") },
                        placeholder = { Text("123456789:ABCdefGh...") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_token_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    OutlinedTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        label = { Text("Chat ID") },
                        placeholder = { Text("e.g. 123456789 (@userinfobot)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_chatid_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSaveTelegramSettings(telegramToken.trim(), telegramChatId.trim(), telegramEnabled)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("save_telegram_button")
                        ) {
                            Text("Save")
                        }

                        Button(
                            onClick = {
                                onTestTelegram(telegramToken.trim(), telegramChatId.trim())
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("test_telegram_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Test Bot")
                        }
                    }
                }
            }
        }

        // Section 5: Appearance & Theme
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_appearance_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Default follows system dark or light theme. You can also pick Light, Dark, or AMOLED True Black.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = settings.themeMode == mode
                        val icon = when (mode) {
                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                            AppThemeMode.DARK -> Icons.Default.DarkMode
                            AppThemeMode.AMOLED -> Icons.Default.Contrast
                        }
                        val tag = when (mode) {
                            AppThemeMode.SYSTEM -> "config_theme_chip_system"
                            AppThemeMode.LIGHT -> "config_theme_chip_light"
                            AppThemeMode.DARK -> "config_theme_chip_dark"
                            AppThemeMode.AMOLED -> "config_theme_chip_amoled"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.title, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag(tag)
                        )
                    }
                }
            }
        }

        // Section 6: Diagnostics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Simulation Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Simulate open slot alert",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mockModeEnabled,
                        onCheckedChange = {
                            mockModeEnabled = it
                            onSaveSettings(
                                selectedRegionVal,
                                selectedRegionTxt,
                                pouValInput,
                                pouTxtInput,
                                courseValInput,
                                courseTxtInput,
                                selectedInterval,
                                it
                            )
                        },
                        modifier = Modifier.testTag("mock_mode_switch")
                    )
                }

                OutlinedButton(
                    onClick = onSendTestNotification,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("send_test_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Notification")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
