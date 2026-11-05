package com.example.domain.model

enum class TriggerType(val displayName: String, val description: String) {
    MANUAL("Manual", "Activated on demand via tap"),
    SCHEDULE("Daily Schedule", "Triggers at a specific time on selected days"),
    BATTERY("Battery Level", "Triggers when battery reaches or drops below threshold"),
    WIFI("Wi-Fi Connection", "Triggers when connected or disconnected from Wi-Fi")
}

enum class ActionType(val displayName: String, val description: String) {
    NOTIFICATION("Show Notification", "Displays a local status or alert notification"),
    OPEN_APP("Open Application", "Launches an installed app via launcher intent"),
    DO_NOT_DISTURB("Do Not Disturb", "Changes DND mode via Notification Policy API"),
    BATTERY_SAVER_GUIDE("Battery Saver Guide", "Prompts user to enable Battery Saver"),
    OPEN_SETTING("Open System Setting", "Guides user directly to specific system settings")
}

enum class MatchCondition {
    ALL, // AND logic
    ANY  // OR logic
}

enum class ExecutionStatus {
    SUCCESS,
    SKIPPED,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
    FAILED
}

enum class DndMode {
    PRIORITY_ONLY,
    TOTAL_SILENCE,
    ALARMS_ONLY,
    OFF
}

enum class WifiTriggerState {
    CONNECTED,
    DISCONNECTED
}

enum class SystemSettingTarget(val displayName: String, val actionIntent: String) {
    BATTERY_SAVER("Battery Saver Settings", "android.settings.BATTERY_SAVER_SETTINGS"),
    DISPLAY("Display & Dark Theme", "android.settings.DISPLAY_SETTINGS"),
    SOUND("Sound & Vibration", "android.settings.SOUND_SETTINGS"),
    WIFI("Network & Internet", "android.settings.WIRELESS_SETTINGS"),
    DO_NOT_DISTURB("Do Not Disturb Policy", "android.settings.ZEN_MODE_AUTOMATION_SETTINGS"),
    NOTIFICATION_LIST("Notification History", "android.settings.NOTIFICATION_SETTINGS")
}
