package com.example.domain.model

data class RoutineCondition(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TriggerType,
    val isEnabled: Boolean = true,
    // Schedule parameters
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5), // 1=Mon, 7=Sun (ISO-8601)
    // Battery parameters
    val batteryThreshold: Int = 20, // percentage (0-100)
    val triggerWhenCharging: Boolean = false,
    // Wi-Fi parameters
    val wifiState: WifiTriggerState = WifiTriggerState.CONNECTED,
    val targetSsid: String? = null // Optional filter if known
) {
    fun summary(): String {
        return when (type) {
            TriggerType.MANUAL -> "Triggered manually on demand"
            TriggerType.SCHEDULE -> {
                val timeStr = String.format("%02d:%02d", hour, minute)
                val daysStr = if (daysOfWeek.size == 7) "Every day"
                else if (daysOfWeek == listOf(1, 2, 3, 4, 5)) "Weekdays"
                else if (daysOfWeek == listOf(6, 7)) "Weekends"
                else daysOfWeek.joinToString(", ") { dayNumberToShortName(it) }
                "At $timeStr ($daysStr)"
            }
            TriggerType.BATTERY -> {
                if (triggerWhenCharging) "When charging"
                else "Battery ≤ $batteryThreshold%"
            }
            TriggerType.WIFI -> {
                val stateText = if (wifiState == WifiTriggerState.CONNECTED) "Connected to Wi-Fi" else "Disconnected from Wi-Fi"
                if (!targetSsid.isNullOrBlank()) "$stateText ($targetSsid)" else stateText
            }
        }
    }

    private fun dayNumberToShortName(day: Int): String = when (day) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}

data class RoutineAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ActionType,
    val orderIndex: Int = 0,
    // Notification params
    val notificationTitle: String = "Routine Activated",
    val notificationBody: String = "Automation completed successfully",
    // Open app params
    val targetPackageName: String = "",
    val targetAppLabel: String = "",
    // DND params
    val dndMode: DndMode = DndMode.PRIORITY_ONLY,
    // System setting target
    val settingTarget: SystemSettingTarget = SystemSettingTarget.BATTERY_SAVER
) {
    fun summary(): String {
        return when (type) {
            ActionType.NOTIFICATION -> "Notification: \"$notificationTitle\""
            ActionType.OPEN_APP -> if (targetAppLabel.isNotBlank()) "Launch $targetAppLabel" else "Launch app"
            ActionType.DO_NOT_DISTURB -> when (dndMode) {
                DndMode.PRIORITY_ONLY -> "Turn on DND (Priority)"
                DndMode.TOTAL_SILENCE -> "Turn on DND (Total Silence)"
                DndMode.ALARMS_ONLY -> "Turn on DND (Alarms Only)"
                DndMode.OFF -> "Turn off DND"
            }
            ActionType.BATTERY_SAVER_GUIDE -> "Open Battery Saver prompt"
            ActionType.OPEN_SETTING -> "Open ${settingTarget.displayName}"
        }
    }
}

data class Routine(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: String = "bolt", // e.g. "sleep", "work", "home", "battery", "game", "bolt"
    val isEnabled: Boolean = true,
    val matchMode: MatchCondition = MatchCondition.ALL,
    val conditions: List<RoutineCondition> = emptyList(),
    val actions: List<RoutineAction> = emptyList(),
    val lastExecutedAt: Long? = null,
    val lastExecutionStatus: ExecutionStatus? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun triggerSummary(): String {
        val enabledConditions = conditions.filter { it.isEnabled }
        return when {
            enabledConditions.isEmpty() -> "No active triggers"
            enabledConditions.size == 1 -> enabledConditions.first().summary()
            matchMode == MatchCondition.ALL -> "${enabledConditions.size} triggers (All must match)"
            else -> "${enabledConditions.size} triggers (Any may match)"
        }
    }

    fun actionsSummary(): String {
        return when {
            actions.isEmpty() -> "No actions"
            actions.size == 1 -> actions.first().summary()
            else -> "${actions.size} actions: ${actions.first().summary()} + ${actions.size - 1} more"
        }
    }
}

data class ExecutionResult(
    val routineId: Long,
    val routineName: String,
    val actionType: ActionType,
    val status: ExecutionStatus,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExecutionLog(
    val id: Long = 0,
    val routineId: Long,
    val routineName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: ExecutionStatus,
    val details: String,
    val actionResultsSummary: String
)
