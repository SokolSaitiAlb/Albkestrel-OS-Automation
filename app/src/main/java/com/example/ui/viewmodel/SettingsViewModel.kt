package com.example.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.RoutineRepository
import com.example.domain.model.ExecutionLog
import com.example.platform.CapabilityStatus
import com.example.platform.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceInfo(
    val model: String,
    val deviceCodename: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildId: String,
    val isPixel8a: Boolean
)

data class SettingsUiState(
    val capabilities: List<CapabilityStatus> = emptyList(),
    val deviceInfo: DeviceInfo,
    val showClearHistoryDialog: Boolean = false,
    val feedbackMessage: String? = null
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val routineRepository: RoutineRepository,
    private val permissionManager: PermissionManager,
    context: Context
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val executionHistory: StateFlow<List<ExecutionLog>> = routineRepository.getRecentLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            capabilities = permissionManager.getAllCapabilities(),
            deviceInfo = inspectDeviceInfo()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun refreshCapabilities() {
        _uiState.value = _uiState.value.copy(
            capabilities = permissionManager.getAllCapabilities()
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun promptClearHistory() {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = true)
    }

    fun dismissClearHistory() {
        _uiState.value = _uiState.value.copy(showClearHistoryDialog = false)
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            routineRepository.clearHistory()
            _uiState.value = _uiState.value.copy(
                showClearHistoryDialog = false,
                feedbackMessage = "Execution history cleared"
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    private fun inspectDeviceInfo(): DeviceInfo {
        val model = Build.MODEL
        val device = Build.DEVICE // "akita" for Pixel 8a
        val manufacturer = Build.MANUFACTURER
        val isAkita = device.equals("akita", ignoreCase = true) ||
                model.contains("Pixel 8a", ignoreCase = true)

        return DeviceInfo(
            model = model,
            deviceCodename = device,
            manufacturer = manufacturer,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.DISPLAY,
            isPixel8a = isAkita
        )
    }
}
