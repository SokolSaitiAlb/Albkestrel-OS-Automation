package com.example.platform

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class CapabilityStatus(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isSpecialAccess: Boolean = false,
    val actionIntent: Intent? = null,
    val limitationNote: String? = null
)

class PermissionManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasDndAccess(): Boolean {
        return notificationManager?.isNotificationPolicyAccessGranted ?: false
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun getNotificationPolicyIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getBatterySaverSettingsIntent(): Intent {
        return Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getExactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun getBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getAppDetailsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getAllCapabilities(): List<CapabilityStatus> {
        val list = mutableListOf<CapabilityStatus>()

        // 1. Notifications
        list.add(
            CapabilityStatus(
                id = "post_notifications",
                title = "Local Notifications",
                description = "Allows routines to display execution summaries and alerts.",
                isGranted = hasPostNotificationPermission(),
                isSpecialAccess = false,
                actionIntent = getAppDetailsSettingsIntent()
            )
        )

        // 2. Notification Policy Access (DND)
        list.add(
            CapabilityStatus(
                id = "dnd_policy",
                title = "Do Not Disturb (Notification Policy)",
                description = "Required to switch DND profiles automatically via public Android SDK.",
                isGranted = hasDndAccess(),
                isSpecialAccess = true,
                actionIntent = getNotificationPolicyIntent(),
                limitationNote = "Pixel 8a & LineageOS require granting Special App Access in system settings."
            )
        )

        // 3. Exact Alarm scheduling
        list.add(
            CapabilityStatus(
                id = "exact_alarms",
                title = "Exact Alarms Scheduling",
                description = "Guarantees punctual schedule triggers down to the exact minute.",
                isGranted = canScheduleExactAlarms(),
                isSpecialAccess = true,
                actionIntent = getExactAlarmSettingsIntent(),
                limitationNote = "Fallback: WorkManager evaluates time-based routines periodically if exact alarm is restricted."
            )
        )

        // 4. Background Battery Exemption
        list.add(
            CapabilityStatus(
                id = "battery_optimization",
                title = "Unrestricted Battery Background",
                description = "Prevents Android Doze from delaying automation evaluation on Pixel 8a.",
                isGranted = isIgnoringBatteryOptimizations(),
                isSpecialAccess = true,
                actionIntent = getBatteryOptimizationSettingsIntent(),
                limitationNote = "Standard standalone app limitation: cannot bypass Doze without user approval."
            )
        )

        return list
    }
}
