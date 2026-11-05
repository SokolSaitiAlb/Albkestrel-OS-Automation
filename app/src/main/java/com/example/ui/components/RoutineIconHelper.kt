package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.domain.model.ActionType
import com.example.domain.model.TriggerType

object RoutineIconHelper {

    fun getIconForRoutine(iconKey: String): ImageVector {
        return when (iconKey.lowercase()) {
            "sleep", "bedtime" -> Icons.Default.Bedtime
            "work", "focus" -> Icons.Default.Work
            "home" -> Icons.Default.Home
            "battery" -> Icons.Default.BatteryAlert
            "game", "gaming" -> Icons.Default.SportsEsports
            else -> Icons.Default.Bolt
        }
    }

    fun getIconForTrigger(triggerType: TriggerType): ImageVector {
        return when (triggerType) {
            TriggerType.MANUAL -> Icons.Default.TouchApp
            TriggerType.SCHEDULE -> Icons.Default.Schedule
            TriggerType.BATTERY -> Icons.Default.BatteryAlert
            TriggerType.WIFI -> Icons.Default.Wifi
        }
    }

    fun getIconForAction(actionType: ActionType): ImageVector {
        return when (actionType) {
            ActionType.NOTIFICATION -> Icons.Default.Notifications
            ActionType.OPEN_APP -> Icons.Default.OpenInNew
            ActionType.DO_NOT_DISTURB -> Icons.Default.Bedtime
            ActionType.BATTERY_SAVER_GUIDE -> Icons.Default.BatteryAlert
            ActionType.OPEN_SETTING -> Icons.Default.Settings
        }
    }

    val availableRoutineIcons = listOf(
        "bolt" to Icons.Default.Bolt,
        "sleep" to Icons.Default.Bedtime,
        "work" to Icons.Default.Work,
        "home" to Icons.Default.Home,
        "battery" to Icons.Default.BatteryAlert,
        "game" to Icons.Default.SportsEsports
    )
}
