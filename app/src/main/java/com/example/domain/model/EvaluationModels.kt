package com.example.domain.model

data class SystemEvaluationContext(
    val currentTimestamp: Long = System.currentTimeMillis(),
    val currentHour: Int = 0,
    val currentMinute: Int = 0,
    val currentDayOfWeek: Int = 1, // 1=Monday .. 7=Sunday
    val minuteTolerance: Int = 2,
    val batteryPercentage: Int = 100,
    val isBatteryCharging: Boolean = false,
    val isWifiConnected: Boolean = false,
    val currentWifiSsid: String? = null,
    val isManualInvocation: Boolean = false
)

data class ConditionEvaluationResult(
    val isSatisfied: Boolean,
    val matchedConditionIds: List<String>,
    val reason: String
)

sealed class RoutineValidationResult {
    object Valid : RoutineValidationResult()
    data class Invalid(val errors: List<String>) : RoutineValidationResult() {
        val formattedErrorMessage: String
            get() = errors.joinToString("\n• ", prefix = "• ")
    }
}
