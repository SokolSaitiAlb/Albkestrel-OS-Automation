package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RoutineRepository
import com.example.domain.engine.RoutineValidator
import com.example.domain.model.ActionType
import com.example.domain.model.DndMode
import com.example.domain.model.MatchCondition
import com.example.domain.model.Routine
import com.example.domain.model.RoutineAction
import com.example.domain.model.RoutineCondition
import com.example.domain.model.RoutineValidationResult
import com.example.domain.model.SystemSettingTarget
import com.example.domain.model.TriggerType
import com.example.domain.model.WifiTriggerState
import com.example.platform.AutomationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InstalledAppInfo(
    val packageName: String,
    val label: String
)

data class CreateEditUiState(
    val routineId: Long = 0L,
    val name: String = "",
    val description: String = "",
    val icon: String = "bolt",
    val isEnabled: Boolean = true,
    val matchMode: MatchCondition = MatchCondition.ALL,
    val conditions: List<RoutineCondition> = emptyList(),
    val actions: List<RoutineAction> = emptyList(),
    val validationErrors: List<String> = emptyList(),
    val isSavedSuccessfully: Boolean = false,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isAppPickerOpen: Boolean = false,
    val pendingActionIndexForAppPicker: Int = -1
)

class CreateEditRoutineViewModel(
    private val repository: RoutineRepository,
    private val scheduler: AutomationScheduler,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val packageManager = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
                val appList = resolveInfoList.map {
                    InstalledAppInfo(
                        packageName = it.activityInfo.packageName,
                        label = it.loadLabel(packageManager).toString()
                    )
                }.sortedBy { it.label.lowercase() }

                _uiState.value = _uiState.value.copy(installedApps = appList)
            } catch (_: Exception) {
            }
        }
    }

    fun loadRoutine(routineId: Long) {
        if (routineId <= 0) {
            // New routine defaults
            _uiState.value = CreateEditUiState(
                routineId = 0L,
                name = "",
                description = "",
                icon = "bolt",
                isEnabled = true,
                matchMode = MatchCondition.ALL,
                conditions = listOf(
                    RoutineCondition(
                        type = TriggerType.SCHEDULE,
                        hour = 8,
                        minute = 0,
                        daysOfWeek = listOf(1, 2, 3, 4, 5)
                    )
                ),
                actions = listOf(
                    RoutineAction(
                        type = ActionType.NOTIFICATION,
                        notificationTitle = "Routine Started",
                        notificationBody = "Your custom automation was triggered",
                        orderIndex = 0
                    )
                ),
                installedApps = _uiState.value.installedApps
            )
            return
        }

        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId)
            if (routine != null) {
                _uiState.value = _uiState.value.copy(
                    routineId = routine.id,
                    name = routine.name,
                    description = routine.description,
                    icon = routine.icon,
                    isEnabled = routine.isEnabled,
                    matchMode = routine.matchMode,
                    conditions = routine.conditions,
                    actions = routine.actions
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, validationErrors = emptyList())
    }

    fun setDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun setIcon(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun setMatchMode(mode: MatchCondition) {
        _uiState.value = _uiState.value.copy(matchMode = mode)
    }

    // Condition management
    fun addCondition(type: TriggerType) {
        val newCondition = when (type) {
            TriggerType.MANUAL -> RoutineCondition(type = TriggerType.MANUAL)
            TriggerType.SCHEDULE -> RoutineCondition(
                type = TriggerType.SCHEDULE,
                hour = 12,
                minute = 0,
                daysOfWeek = listOf(1, 2, 3, 4, 5)
            )
            TriggerType.BATTERY -> RoutineCondition(
                type = TriggerType.BATTERY,
                batteryThreshold = 25,
                triggerWhenCharging = false
            )
            TriggerType.WIFI -> RoutineCondition(
                type = TriggerType.WIFI,
                wifiState = WifiTriggerState.CONNECTED
            )
        }
        val updated = _uiState.value.conditions + newCondition
        _uiState.value = _uiState.value.copy(conditions = updated, validationErrors = emptyList())
    }

    fun removeCondition(index: Int) {
        if (index in _uiState.value.conditions.indices) {
            val updated = _uiState.value.conditions.toMutableList().apply { removeAt(index) }
            _uiState.value = _uiState.value.copy(conditions = updated)
        }
    }

    fun updateCondition(index: Int, condition: RoutineCondition) {
        if (index in _uiState.value.conditions.indices) {
            val updated = _uiState.value.conditions.toMutableList().apply { set(index, condition) }
            _uiState.value = _uiState.value.copy(conditions = updated, validationErrors = emptyList())
        }
    }

    // Action management
    fun addAction(type: ActionType) {
        val order = _uiState.value.actions.size
        val newAction = when (type) {
            ActionType.NOTIFICATION -> RoutineAction(
                type = ActionType.NOTIFICATION,
                notificationTitle = "Alert from Albkestrel",
                notificationBody = "Routine executed successfully",
                orderIndex = order
            )
            ActionType.OPEN_APP -> {
                val firstApp = _uiState.value.installedApps.firstOrNull()
                RoutineAction(
                    type = ActionType.OPEN_APP,
                    targetPackageName = firstApp?.packageName ?: "",
                    targetAppLabel = firstApp?.label ?: "",
                    orderIndex = order
                )
            }
            ActionType.DO_NOT_DISTURB -> RoutineAction(
                type = ActionType.DO_NOT_DISTURB,
                dndMode = DndMode.PRIORITY_ONLY,
                orderIndex = order
            )
            ActionType.BATTERY_SAVER_GUIDE -> RoutineAction(
                type = ActionType.BATTERY_SAVER_GUIDE,
                orderIndex = order
            )
            ActionType.OPEN_SETTING -> RoutineAction(
                type = ActionType.OPEN_SETTING,
                settingTarget = SystemSettingTarget.BATTERY_SAVER,
                orderIndex = order
            )
        }
        val updated = _uiState.value.actions + newAction
        _uiState.value = _uiState.value.copy(actions = updated, validationErrors = emptyList())
    }

    fun removeAction(index: Int) {
        if (index in _uiState.value.actions.indices) {
            val updated = _uiState.value.actions.toMutableList().apply { removeAt(index) }
            _uiState.value = _uiState.value.copy(actions = updated)
        }
    }

    fun updateAction(index: Int, action: RoutineAction) {
        if (index in _uiState.value.actions.indices) {
            val updated = _uiState.value.actions.toMutableList().apply { set(index, action) }
            _uiState.value = _uiState.value.copy(actions = updated, validationErrors = emptyList())
        }
    }

    fun openAppPickerForAction(actionIndex: Int) {
        _uiState.value = _uiState.value.copy(
            isAppPickerOpen = true,
            pendingActionIndexForAppPicker = actionIndex
        )
    }

    fun closeAppPicker() {
        _uiState.value = _uiState.value.copy(
            isAppPickerOpen = false,
            pendingActionIndexForAppPicker = -1
        )
    }

    fun selectAppForAction(app: InstalledAppInfo) {
        val index = _uiState.value.pendingActionIndexForAppPicker
        if (index in _uiState.value.actions.indices) {
            val current = _uiState.value.actions[index]
            val updatedAction = current.copy(
                targetPackageName = app.packageName,
                targetAppLabel = app.label
            )
            updateAction(index, updatedAction)
        }
        closeAppPicker()
    }

    fun saveRoutine() {
        val current = _uiState.value
        val routine = Routine(
            id = current.routineId,
            name = current.name.trim(),
            description = current.description.trim(),
            icon = current.icon,
            isEnabled = current.isEnabled,
            matchMode = current.matchMode,
            conditions = current.conditions,
            actions = current.actions.mapIndexed { idx, act -> act.copy(orderIndex = idx) }
        )

        when (val validation = RoutineValidator.validate(routine)) {
            is RoutineValidationResult.Invalid -> {
                _uiState.value = _uiState.value.copy(validationErrors = validation.errors)
            }
            RoutineValidationResult.Valid -> {
                viewModelScope.launch {
                    val savedId = repository.saveRoutine(routine)
                    if (routine.isEnabled) {
                        scheduler.scheduleRoutineAlarm(routine.copy(id = savedId))
                    } else {
                        scheduler.cancelRoutineAlarm(savedId)
                    }
                    _uiState.value = _uiState.value.copy(isSavedSuccessfully = true)
                }
            }
        }
    }
}
