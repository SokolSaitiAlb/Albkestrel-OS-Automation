package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ActionType
import com.example.domain.model.DndMode
import com.example.domain.model.MatchCondition
import com.example.domain.model.RoutineAction
import com.example.domain.model.RoutineCondition
import com.example.domain.model.SystemSettingTarget
import com.example.domain.model.TriggerType
import com.example.domain.model.WifiTriggerState
import com.example.ui.components.RoutineIconHelper
import com.example.ui.viewmodel.CreateEditRoutineViewModel
import com.example.ui.viewmodel.InstalledAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditRoutineScreen(
    routineId: Long?,
    viewModel: CreateEditRoutineViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    androidx.activity.compose.BackHandler(onBack = onNavigateBack)

    LaunchedEffect(routineId) {
        viewModel.loadRoutine(routineId ?: 0L)
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onNavigateBack()
        }
    }

    var showAddConditionMenu by remember { mutableStateOf(false) }
    var showAddActionMenu by remember { mutableStateOf(false) }

    if (uiState.isAppPickerOpen) {
        AppPickerDialog(
            apps = uiState.installedApps,
            onAppSelected = { viewModel.selectAppForAction(it) },
            onDismiss = { viewModel.closeAppPicker() }
        )
    }

    Scaffold(
        modifier = modifier.testTag("screen_create_edit_routine"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.routineId > 0) "Edit Routine" else "New Routine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Living Flow Pipeline",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_routine_editor")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveRoutine()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_save_routine")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Validation Error Alert
            if (uiState.validationErrors.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("card_validation_errors")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Please fix the following issues:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.validationErrors.forEach { err ->
                                Text(
                                    text = "• $err",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Node 1: General Details & Icon
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = RoutineIconHelper.getIconForRoutine(uiState.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Routine Identity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Configure naming and launcher icon",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.setName(it) },
                            label = { Text("Routine Name") },
                            placeholder = { Text("e.g. Night Shift, Arrive at Gym") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_routine_name"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = { viewModel.setDescription(it) },
                            label = { Text("Description (Optional)") },
                            placeholder = { Text("What this routine accomplishes") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_routine_description"),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Icon Style",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RoutineIconHelper.availableRoutineIcons.forEach { (key, iconVector) ->
                                val isSelected = uiState.icon == key
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setIcon(key)
                                        }
                                        .testTag("icon_picker_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = key,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Flow Connector 1
            item {
                PipelineConnector(label = "TRIGGER CONDITIONS (WHEN)")
            }

            // Node 2: Conditions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Triggers (${uiState.conditions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "When should this routine activate?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { showAddConditionMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_add_condition")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Trigger")
                        }

                        DropdownMenu(
                            expanded = showAddConditionMenu,
                            onDismissRequest = { showAddConditionMenu = false }
                        ) {
                            TriggerType.values().forEach { trigger ->
                                DropdownMenuItem(
                                    text = { Text(trigger.displayName) },
                                    leadingIcon = {
                                        Icon(RoutineIconHelper.getIconForTrigger(trigger), contentDescription = null)
                                    },
                                    onClick = {
                                        showAddConditionMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.addCondition(trigger)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Match Mode Toggle (All vs Any)
            if (uiState.conditions.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Condition Match Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.matchMode == MatchCondition.ALL,
                                onClick = { viewModel.setMatchMode(MatchCondition.ALL) },
                                label = { Text("Match All (AND)") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("chip_match_all")
                            )
                            FilterChip(
                                selected = uiState.matchMode == MatchCondition.ANY,
                                onClick = { viewModel.setMatchMode(MatchCondition.ANY) },
                                label = { Text("Match Any (OR)") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("chip_match_any")
                            )
                        }
                    }
                }
            }

            // List of Conditions
            items(uiState.conditions.indices.toList()) { index ->
                val condition = uiState.conditions[index]
                ConditionCard(
                    condition = condition,
                    onUpdate = { viewModel.updateCondition(index, it) },
                    onDelete = { viewModel.removeCondition(index) }
                )
            }

            // Flow Connector 2
            item {
                PipelineConnector(label = "EXECUTION PIPELINE (THEN)")
            }

            // Node 3: Actions Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Actions (${uiState.actions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ordered sequence of device operations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { showAddActionMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_add_action")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Action")
                        }

                        DropdownMenu(
                            expanded = showAddActionMenu,
                            onDismissRequest = { showAddActionMenu = false }
                        ) {
                            ActionType.values().forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.displayName) },
                                    leadingIcon = {
                                        Icon(RoutineIconHelper.getIconForAction(action), contentDescription = null)
                                    },
                                    onClick = {
                                        showAddActionMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.addAction(action)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // List of Actions
            items(uiState.actions.indices.toList()) { index ->
                val action = uiState.actions[index]
                ActionCard(
                    action = action,
                    actionIndex = index,
                    onUpdate = { viewModel.updateAction(index, it) },
                    onDelete = { viewModel.removeAction(index) },
                    onPickApp = { viewModel.openAppPickerForAction(index) }
                )
            }
        }
    }
}

@Composable
private fun PipelineConnector(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionCard(
    condition: RoutineCondition,
    onUpdate: (RoutineCondition) -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = RoutineIconHelper.getIconForTrigger(condition.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = condition.type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = condition.isEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpdate(condition.copy(isEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("switch_condition_enabled")
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Condition",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (condition.type) {
                TriggerType.MANUAL -> {
                    Text(
                        text = "Fires when user manually taps 'Run' on this routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TriggerType.SCHEDULE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Hour: ${String.format("%02d", condition.hour)}", style = MaterialTheme.typography.labelSmall)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val newHour = if (condition.hour == 0) 23 else condition.hour - 1
                                        onUpdate(condition.copy(hour = newHour))
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-")
                                }
                                OutlinedTextField(
                                    value = condition.hour.toString(),
                                    onValueChange = { str ->
                                        if (str.isEmpty()) onUpdate(condition.copy(hour = 0))
                                        else str.toIntOrNull()?.let { onUpdate(condition.copy(hour = it.coerceIn(0, 23))) }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        val newHour = if (condition.hour == 23) 0 else condition.hour + 1
                                        onUpdate(condition.copy(hour = newHour))
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+")
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Minute: ${String.format("%02d", condition.minute)}", style = MaterialTheme.typography.labelSmall)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val newMin = if (condition.minute <= 4) 55 else condition.minute - 5
                                        onUpdate(condition.copy(minute = newMin))
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-5")
                                }
                                OutlinedTextField(
                                    value = condition.minute.toString(),
                                    onValueChange = { str ->
                                        if (str.isEmpty()) onUpdate(condition.copy(minute = 0))
                                        else str.toIntOrNull()?.let { onUpdate(condition.copy(minute = it.coerceIn(0, 59))) }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        val newMin = if (condition.minute >= 55) 0 else condition.minute + 5
                                        onUpdate(condition.copy(minute = newMin))
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+5")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick time presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "07:00" to (7 to 0),
                            "09:00" to (9 to 0),
                            "18:00" to (18 to 0),
                            "22:30" to (22 to 30)
                        ).forEach { (label, time) ->
                            FilterChip(
                                selected = condition.hour == time.first && condition.minute == time.second,
                                onClick = { onUpdate(condition.copy(hour = time.first, minute = time.second)) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Days of Week:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (dayIdx in 1..7) {
                            val isSelected = condition.daysOfWeek.contains(dayIdx)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val newDays = if (isSelected) {
                                            condition.daysOfWeek - dayIdx
                                        } else {
                                            (condition.daysOfWeek + dayIdx).sorted()
                                        }
                                        onUpdate(condition.copy(daysOfWeek = newDays))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNames[dayIdx - 1],
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                TriggerType.BATTERY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (condition.triggerWhenCharging) "When plugged into charger"
                            else "Threshold: ${condition.batteryThreshold}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        FilterChip(
                            selected = condition.triggerWhenCharging,
                            onClick = { onUpdate(condition.copy(triggerWhenCharging = !condition.triggerWhenCharging)) },
                            label = { Text("Charging State") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (!condition.triggerWhenCharging) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = condition.batteryThreshold.toFloat(),
                            onValueChange = { onUpdate(condition.copy(batteryThreshold = it.toInt())) },
                            valueRange = 5f..90f,
                            steps = 16
                        )
                    }
                }
                TriggerType.WIFI -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = condition.wifiState == WifiTriggerState.CONNECTED,
                            onClick = { onUpdate(condition.copy(wifiState = WifiTriggerState.CONNECTED)) },
                            label = { Text("Connected") },
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = condition.wifiState == WifiTriggerState.DISCONNECTED,
                            onClick = { onUpdate(condition.copy(wifiState = WifiTriggerState.DISCONNECTED)) },
                            label = { Text("Disconnected") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    action: RoutineAction,
    actionIndex: Int,
    onUpdate: (RoutineAction) -> Unit,
    onDelete: () -> Unit,
    onPickApp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${actionIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = action.type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Action",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (action.type) {
                ActionType.NOTIFICATION -> {
                    OutlinedTextField(
                        value = action.notificationTitle,
                        onValueChange = { onUpdate(action.copy(notificationTitle = it)) },
                        label = { Text("Notification Title") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = action.notificationBody,
                        onValueChange = { onUpdate(action.copy(notificationBody = it)) },
                        label = { Text("Notification Message") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                ActionType.OPEN_APP -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (action.targetAppLabel.isNotBlank()) action.targetAppLabel else "No App Selected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (action.targetPackageName.isNotBlank()) {
                                Text(
                                    text = action.targetPackageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Button(
                            onClick = onPickApp,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Choose App")
                        }
                    }
                }
                ActionType.DO_NOT_DISTURB -> {
                    Text(
                        text = "Set Do Not Disturb mode:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DndMode.values().forEach { mode ->
                            FilterChip(
                                selected = action.dndMode == mode,
                                onClick = { onUpdate(action.copy(dndMode = mode)) },
                                shape = RoundedCornerShape(10.dp),
                                label = {
                                    Text(
                                        when (mode) {
                                            DndMode.PRIORITY_ONLY -> "Priority"
                                            DndMode.TOTAL_SILENCE -> "Silence"
                                            DndMode.ALARMS_ONLY -> "Alarms"
                                            DndMode.OFF -> "Turn Off"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
                ActionType.BATTERY_SAVER_GUIDE -> {
                    Text(
                        text = "Guides user directly to system Battery Saver screen (direct background toggle requires system signature).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ActionType.OPEN_SETTING -> {
                    Text(
                        text = "Target System Setting Screen:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SystemSettingTarget.values().forEach { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdate(action.copy(settingTarget = target)) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = action.settingTarget == target,
                                    onClick = { onUpdate(action.copy(settingTarget = target)) },
                                    shape = RoundedCornerShape(10.dp),
                                    label = { Text(target.displayName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppPickerDialog(
    apps: List<InstalledAppInfo>,
    onAppSelected: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Application",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No applications found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onAppSelected(app) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
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
