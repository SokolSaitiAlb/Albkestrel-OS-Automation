package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RoutineRepository
import com.example.domain.model.ExecutionLog
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.Routine
import com.example.platform.ActionExecutor
import com.example.platform.AutomationScheduler
import com.example.platform.SystemStateMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val isWifiConnected: Boolean = false,
    val wifiSsid: String? = null,
    val bannerMessage: String? = null
)

class HomeViewModel(
    private val repository: RoutineRepository,
    private val systemStateMonitor: SystemStateMonitor,
    private val actionExecutor: ActionExecutor,
    private val scheduler: AutomationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val allRoutines: StateFlow<List<Routine>> = repository.getAllRoutines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeCount: StateFlow<Int> = repository.getActiveCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val recentLogs: StateFlow<List<ExecutionLog>> = repository.getLatestFiveLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshSystemStatus()
    }

    fun refreshSystemStatus() {
        val (battery, charging) = systemStateMonitor.getBatteryInfo()
        val wifiConnected = systemStateMonitor.isWifiConnected()
        val ssid = systemStateMonitor.getWifiSsid()

        _uiState.value = _uiState.value.copy(
            batteryPercentage = battery,
            isCharging = charging,
            isWifiConnected = wifiConnected,
            wifiSsid = ssid
        )
    }

    fun toggleRoutine(routine: Routine) {
        viewModelScope.launch {
            val newState = !routine.isEnabled
            repository.setRoutineEnabled(routine.id, newState)
            if (newState) {
                scheduler.scheduleRoutineAlarm(routine.copy(isEnabled = true))
            } else {
                scheduler.cancelRoutineAlarm(routine.id)
            }
        }
    }

    fun runRoutineNow(routine: Routine) {
        viewModelScope.launch {
            val results = actionExecutor.executeActions(routine)
            val overallStatus = com.example.domain.engine.ExecutionStatusResolver.resolveOverallStatus(results)

            val summary = results.joinToString(" • ") { "${it.actionType.displayName}: ${it.status.name}" }
            repository.recordExecution(
                routineId = routine.id,
                status = overallStatus,
                details = "Executed manually from Home screen",
                actionsSummary = summary
            )

            _uiState.value = _uiState.value.copy(
                bannerMessage = "Executed \"${routine.name}\": $overallStatus"
            )
        }
    }

    fun clearBanner() {
        _uiState.value = _uiState.value.copy(bannerMessage = null)
    }
}
