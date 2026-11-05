package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RoutineRepository
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.Routine
import com.example.platform.ActionExecutor
import com.example.platform.AutomationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutinesUiState(
    val routineToDelete: Routine? = null,
    val feedbackMessage: String? = null
)

class RoutinesViewModel(
    private val repository: RoutineRepository,
    private val actionExecutor: ActionExecutor,
    private val scheduler: AutomationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesUiState())
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    val routines: StateFlow<List<Routine>> = repository.getAllRoutines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleEnabled(routine: Routine) {
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

    fun promptDelete(routine: Routine) {
        _uiState.value = _uiState.value.copy(routineToDelete = routine)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(routineToDelete = null)
    }

    fun confirmDelete() {
        val routine = _uiState.value.routineToDelete ?: return
        viewModelScope.launch {
            scheduler.cancelRoutineAlarm(routine.id)
            repository.deleteRoutine(routine.id)
            _uiState.value = _uiState.value.copy(
                routineToDelete = null,
                feedbackMessage = "Deleted \"${routine.name}\""
            )
        }
    }

    fun duplicateRoutine(routine: Routine) {
        viewModelScope.launch {
            val newId = repository.duplicateRoutine(routine.id)
            if (newId != null) {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Duplicated as \"${routine.name} (Copy)\""
                )
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
                details = "Executed manually from Routines list",
                actionsSummary = summary
            )

            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Executed \"${routine.name}\": $overallStatus"
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}
