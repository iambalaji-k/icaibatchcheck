package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IcaiUrls
import com.example.data.db.BatchEntity
import com.example.data.repository.BatchParser
import com.example.ui.SettingsUiState
import com.example.ui.theme.StatusFull
import com.example.ui.theme.StatusFullContainer
import com.example.ui.theme.StatusOpen
import com.example.ui.theme.StatusOpenContainer
import com.example.ui.theme.onStatusOpen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    batches: List<BatchEntity>,
    settings: SettingsUiState,
    isRefreshing: Boolean,
    onToggleMonitoring: () -> Unit,
    onCheckNow: () -> Unit,
    onNavigateToTargets: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeTargets = settings.targets.filter { it.isEnabled }

    // Filter work is memoized on its actual inputs so unrelated recompositions
    // (spinner, clock) do not rebuild lists and re-key every card.
    val filteredBatches = remember(batches, activeTargets) {
        if (activeTargets.isEmpty()) {
            emptyList()
        } else {
            batches.filter { batch ->
                activeTargets.any { target ->
                    batch.pouName.equals(target.pouText, ignoreCase = true) &&
                            BatchParser.courseMatches(batch.courseName, target.courseText)
                }
            }
        }
    }
    val openBatches = remember(filteredBatches) { filteredBatches.filter { it.isOpen } }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showOnlyOpen by rememberSaveable { mutableStateOf(false) }

    val displayedBatches = remember(filteredBatches, showOnlyOpen, searchQuery) {
        filteredBatches.filter { batch ->
            val matchesFilter = if (showOnlyOpen) batch.isOpen else true
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                batch.batchName.contains(searchQuery, ignoreCase = true) ||
                        batch.venue.contains(searchQuery, ignoreCase = true) ||
                        batch.dates.contains(searchQuery, ignoreCase = true) ||
                        batch.timings.contains(searchQuery, ignoreCase = true) ||
                        batch.courseName.contains(searchQuery, ignoreCase = true) ||
                        batch.pouName.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(2.dp)) }

        // Expressive Control Strip
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("status_control_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isMonitoringActive)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status bar row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (settings.isMonitoringActive) StatusOpen() else MaterialTheme.colorScheme.outline)
                            )
                            Text(
                                text = if (settings.isMonitoringActive) "Auto-Checking" else "Monitoring Paused",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (settings.isMonitoringActive)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "${settings.intervalMinutes}m cycle",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Active Target Chips Row (Clean, minimal)
                    if (activeTargets.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeTargets.forEach { target ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = "${target.pouText} • ${target.courseText}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons
                    val hasTargets = activeTargets.isNotEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleMonitoring,
                            enabled = hasTargets || settings.isMonitoringActive,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("toggle_monitoring_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.isMonitoringActive) StatusFull() else StatusOpen()
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.isMonitoringActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (settings.isMonitoringActive) "Pause"
                                else if (!hasTargets) "No Targets"
                                else "Start Monitor",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onCheckNow,
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("check_now_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .semantics { contentDescription = "Checking, please wait" },
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Check Now",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expressive Key Metrics (Centered 2-Column Grid)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val openColor = StatusOpen()
                val openContainer = StatusOpenContainer()
                // Open Seats Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 96.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (openBatches.isNotEmpty()) openContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${openBatches.size}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (openBatches.isNotEmpty()) openColor else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Open Seats",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Batches Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 96.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${filteredBatches.size}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Total Batches",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Battery-optimization guidance: shown while monitoring is active and the
        // app is still subject to Doze deferral.
        if (settings.isMonitoringActive && !isIgnoringBatteryOptimizations(context)) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("battery_optimization_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery saver may delay checks",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Allow unrestricted background running for reliable alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        TextButton(
                            onClick = { openBatterySettings(context) },
                            modifier = Modifier.testTag("battery_settings_button")
                        ) {
                            Text("Open settings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Dedicated Single Prominent Action Banner for ICAI Portal
        item {
            OutlinedButton(
                onClick = { openIcaiPortal(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .heightIn(min = 48.dp)
                    .testTag("open_icai_web_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open ICAI Registration Portal",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        // Search & Filter Row
        if (filteredBatches.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_search_input"),
                        placeholder = { Text("Filter batches, dates, venue...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showOnlyOpen,
                            onClick = { showOnlyOpen = false },
                            label = { Text("All (${filteredBatches.size})") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                        FilterChip(
                            selected = showOnlyOpen,
                            onClick = { showOnlyOpen = true },
                            label = { Text("Open Seats (${openBatches.size})") },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                if (showOnlyOpen) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusOpenContainer(),
                                selectedLabelColor = onStatusOpen()
                            ),
                            modifier = Modifier.testTag("filter_chip_open_only")
                        )
                    }
                }
            }
        }

        // Batch List or Empty State
        if (displayedBatches.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventSeat,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (settings.targets.isEmpty()) "No Targets Set"
                            else if (searchQuery.isNotBlank() || showOnlyOpen) "No Matches"
                            else "No Batches Loaded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (settings.targets.isEmpty()) "Add a target in the Targets tab"
                            else if (searchQuery.isNotBlank() || showOnlyOpen) "Try adjusting your filters"
                            else "Tap 'Check Now' to fetch live batches",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (settings.targets.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onNavigateToTargets,
                                modifier = Modifier.testTag("empty_add_target_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Target", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            items(displayedBatches, key = { it.id }) { batch ->
                BatchCardItem(batch = batch)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun isIgnoringBatteryOptimizations(context: android.content.Context): Boolean {
    val pm = context.getSystemService(android.os.PowerManager::class.java) ?: return false
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatterySettings(context: android.content.Context) {
    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

private fun openIcaiPortal(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(IcaiUrls.PORTAL)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
fun BatchCardItem(batch: BatchEntity) {
    val context = LocalContext.current
    val timeFormatted = rememberTimeFormat(batch.lastCheckedTimestamp)
    val openColor = StatusOpen()
    val fullColor = StatusFull()
    val fullContainer = StatusFullContainer()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .testTag("batch_item_${batch.batchName}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (batch.isOpen)
                StatusOpenContainer().copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Course / City pill & Availability Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "${batch.pouName} • ${batch.courseName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (batch.isOpen) openColor else fullContainer
                ) {
                    Text(
                        text = if (batch.isOpen) "${batch.availableSeats} Open" else "Full",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (batch.isOpen) MaterialTheme.colorScheme.onPrimary else fullColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Batch Name Title
            Text(
                text = batch.batchName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Capacity indicator. The percentage bar is shown ONLY when the portal
            // actually published a total; otherwise we'd imply a misleading
            // "avail of avail = 100%" reading. Just show the seat count instead.
            val total = batch.totalSeats
            val avail = batch.availableSeats
            if (batch.knownCapacity && total > 0) {
                val progress = (avail.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$avail of $total seats left",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (batch.isOpen) openColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (batch.isOpen) openColor else fullColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else if (avail > 0) {
                Text(
                    text = "$avail seat${if (avail == 1) "" else "s"} available",
                    style = MaterialTheme.typography.labelMedium,
                    color = openColor
                )
            }

            // Key Info Row (Icons with clean values)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (batch.dates.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = batch.dates,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (batch.timings.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = batch.timings,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (batch.venue.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = batch.venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom bar: Updated time + Register Button if open
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checked: $timeFormatted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (batch.isOpen) {
                    Button(
                        onClick = { openIcaiPortal(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = openColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun rememberTimeFormat(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val context = LocalContext.current
    return remember(timestamp, context) { formatTimestamp(context, timestamp) }
}
