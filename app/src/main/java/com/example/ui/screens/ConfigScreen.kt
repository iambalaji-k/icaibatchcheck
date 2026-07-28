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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BatchTarget
import com.example.data.model.DropdownOption
import com.example.ui.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen(
    settings: SettingsUiState,
    regions: List<DropdownOption>,
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
    onSendTestNotification: () -> Unit
) {
    // New target entry state
    var selectedRegionVal by remember { mutableStateOf("4") }
    var selectedRegionTxt by remember { mutableStateOf("Southern") }
    var pouTxtInput by remember { mutableStateOf("Chennai") }
    var pouValInput by remember { mutableStateOf("3") }
    var courseTxtInput by remember { mutableStateOf("AICITSS - Advanced Information Technology") }
    var courseValInput by remember { mutableStateOf("48") }

    // General & Telegram state
    var selectedInterval by remember(settings) { mutableStateOf(settings.intervalMinutes) }
    var mockModeEnabled by remember(settings) { mutableStateOf(settings.mockModeEnabled) }

    var telegramToken by remember(settings) { mutableStateOf(settings.telegramBotToken) }
    var telegramChatId by remember(settings) { mutableStateOf(settings.telegramChatId) }
    var telegramEnabled by remember(settings) { mutableStateOf(settings.telegramEnabled) }

    var regionDropdownExpanded by remember { mutableStateOf(false) }

    val regionOptions = if (regions.isNotEmpty()) regions else listOf(
        DropdownOption("1", "Central"),
        DropdownOption("2", "Eastern"),
        DropdownOption("3", "Northern"),
        DropdownOption("4", "Southern"),
        DropdownOption("5", "Western")
    )

    val popularPous = listOf("Chennai", "Bengaluru", "Hyderabad", "Mumbai", "Delhi", "Kolkata", "Coimbatore", "Ernakulam", "Pune")
    val popularCourses = listOf(
        DropdownOption("48", "AICITSS - Advanced Information Technology"),
        DropdownOption("47", "AICITSS - MCS (Management & Communication)"),
        DropdownOption("46", "ICITSS - Information Technology"),
        DropdownOption("45", "ICITSS - Orientation Course")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Target Configurations & Notifications",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        // 1. ACTIVE TARGETS SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_targets_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Targets (${settings.targets.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Monitor multiple ICAI POUs / Courses concurrently. All enabled targets are auto-checked in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (settings.targets.isEmpty()) {
                    Text(
                        text = "No targets configured yet. Add your first target below!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        settings.targets.forEach { target ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("target_item_${target.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (target.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${target.pouText} (${target.regionText})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (target.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = target.courseText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = target.isEnabled,
                                            onCheckedChange = { onToggleTargetEnabled(target.id) },
                                            modifier = Modifier.testTag("toggle_target_${target.id}")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onRemoveTarget(target.id) },
                                            modifier = Modifier.testTag("delete_target_${target.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Target",
                                                tint = MaterialTheme.colorScheme.error
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

        // 2. ADD NEW TARGET SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Target",
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
                        label = { Text("ICAI Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("region_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = regionDropdownExpanded,
                        onDismissRequest = { regionDropdownExpanded = false }
                    ) {
                        regionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.text} (ID: ${option.value})") },
                                onClick = {
                                    selectedRegionVal = option.value
                                    selectedRegionTxt = option.text
                                    regionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // POU City Input
                OutlinedTextField(
                    value = pouTxtInput,
                    onValueChange = { pouTxtInput = it },
                    label = { Text("POU City / Branch") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pou_text_field"),
                    singleLine = true
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    popularPous.forEach { pou ->
                        FilterChip(
                            selected = pouTxtInput.equals(pou, ignoreCase = true),
                            onClick = { pouTxtInput = pou },
                            label = { Text(pou, fontSize = 12.sp) },
                            leadingIcon = if (pouTxtInput.equals(pou, ignoreCase = true)) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                // Course Selection Input
                OutlinedTextField(
                    value = courseTxtInput,
                    onValueChange = { courseTxtInput = it },
                    label = { Text("Course Name") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_text_field"),
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    popularCourses.forEach { crs ->
                        val isSelected = courseValInput == crs.value
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                courseValInput = crs.value
                                courseTxtInput = crs.text
                            },
                            label = { Text(crs.text, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_target_button"),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Target to Monitoring List")
                }
            }
        }

        // 3. TELEGRAM BOT NOTIFICATIONS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("telegram_config_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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

                Text(
                    text = "Receive instant slot opening alerts directly on your phone or group via Telegram Bot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = { telegramToken = it },
                    label = { Text("Telegram Bot Token") },
                    placeholder = { Text("e.g. 123456789:ABCdefGhIJK...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("telegram_token_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = telegramChatId,
                    onValueChange = { telegramChatId = it },
                    label = { Text("Telegram Chat ID / Channel") },
                    placeholder = { Text("e.g. 123456789 or @my_channel") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("telegram_chatid_input"),
                    singleLine = true
                )

                Text(
                    text = "💡 Tip: Start a chat with @userinfobot on Telegram to quickly get your numeric Chat ID.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveTelegramSettings(telegramToken.trim(), telegramChatId.trim(), telegramEnabled)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_telegram_button")
                    ) {
                        Text("Save Config")
                    }

                    Button(
                        onClick = {
                            onTestTelegram(telegramToken.trim(), telegramChatId.trim())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_telegram_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Test Bot")
                    }
                }
            }
        }

        // 4. CHECK FREQUENCY INTERVAL
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-Check Frequency",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Select how often the background service checks ICAI servers for all active targets:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("interval_chip_${mins}m")
                        )
                    }
                }
            }
        }

        // 5. TESTING TOOLS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Testing & Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simulation Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Generates simulated open seat alerts to verify notifications without waiting for real slots.",
                            style = MaterialTheme.typography.bodySmall,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_test_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Test Device Notification")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
