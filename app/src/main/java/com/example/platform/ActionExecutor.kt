package com.example.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.ActionType
import com.example.domain.model.DndMode
import com.example.domain.model.ExecutionResult
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.Routine
import com.example.domain.model.RoutineAction

class ActionExecutor(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val permissionManager = PermissionManager(context)

    companion object {
        const val CHANNEL_ID = "albkestrel_automation_channel"
        const val CHANNEL_NAME = "Routine Automation Alerts"
        private var isChannelCreated = false
    }

    init {
        createNotificationChannelIfNeeded()
    }

    private fun createNotificationChannelIfNeeded() {
        if (!isChannelCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications triggered by Albkestrel OS Automation routines"
            }
            notificationManager?.createNotificationChannel(channel)
            isChannelCreated = true
        }
    }

    /**
     * Executes all actions in order and returns a list of explicit results.
     */
    fun executeActions(routine: Routine): List<ExecutionResult> {
        val results = mutableListOf<ExecutionResult>()
        val sortedActions = routine.actions.sortedBy { it.orderIndex }

        for (action in sortedActions) {
            val result = executeSingleAction(routine, action)
            results.add(result)
        }
        return results
    }

    fun executeSingleAction(routine: Routine, action: RoutineAction): ExecutionResult {
        return when (action.type) {
            ActionType.NOTIFICATION -> executeNotificationAction(routine, action)
            ActionType.OPEN_APP -> executeOpenAppAction(routine, action)
            ActionType.DO_NOT_DISTURB -> executeDndAction(routine, action)
            ActionType.BATTERY_SAVER_GUIDE -> executeBatterySaverGuideAction(routine, action)
            ActionType.OPEN_SETTING -> executeOpenSettingAction(routine, action)
        }
    }

    private fun executeNotificationAction(routine: Routine, action: RoutineAction): ExecutionResult {
        if (!permissionManager.hasPostNotificationPermission()) {
            return ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.PERMISSION_REQUIRED,
                message = "Notification permission (POST_NOTIFICATIONS) is not granted."
            )
        }

        return try {
            createNotificationChannelIfNeeded()

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                routine.id.toInt(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.albkestrel_icon_art)
                .setContentTitle(action.notificationTitle.ifBlank { routine.name })
                .setContentText(action.notificationBody.ifBlank { "Routine executed" })
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationId = (routine.id * 100 + action.orderIndex).toInt()
            notificationManager?.notify(notificationId, notification)

            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SUCCESS,
                message = "Notification \"${action.notificationTitle}\" displayed."
            )
        } catch (e: Exception) {
            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.FAILED,
                message = "Failed to post notification: ${e.message}"
            )
        }
    }

    private fun executeOpenAppAction(routine: Routine, action: RoutineAction): ExecutionResult {
        val packageName = action.targetPackageName
        if (packageName.isBlank()) {
            return ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.OPEN_APP,
                status = ExecutionStatus.FAILED,
                message = "No target app package specified."
            )
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ExecutionResult(
                    routineId = routine.id,
                    routineName = routine.name,
                    actionType = ActionType.OPEN_APP,
                    status = ExecutionStatus.SUCCESS,
                    message = "Launched app ${action.targetAppLabel.ifBlank { packageName }}."
                )
            } else {
                ExecutionResult(
                    routineId = routine.id,
                    routineName = routine.name,
                    actionType = ActionType.OPEN_APP,
                    status = ExecutionStatus.UNSUPPORTED,
                    message = "Target app $packageName is not installed or has no launcher activity."
                )
            }
        } catch (e: Exception) {
            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.OPEN_APP,
                status = ExecutionStatus.FAILED,
                message = "Failed to launch $packageName: ${e.message}"
            )
        }
    }

    private fun executeDndAction(routine: Routine, action: RoutineAction): ExecutionResult {
        if (!permissionManager.hasDndAccess()) {
            return ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.DO_NOT_DISTURB,
                status = ExecutionStatus.PERMISSION_REQUIRED,
                message = "Notification Policy Access is required to change DND mode. Please grant in Settings."
            )
        }

        return try {
            val filter = when (action.dndMode) {
                DndMode.PRIORITY_ONLY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                DndMode.TOTAL_SILENCE -> NotificationManager.INTERRUPTION_FILTER_NONE
                DndMode.ALARMS_ONLY -> NotificationManager.INTERRUPTION_FILTER_ALARMS
                DndMode.OFF -> NotificationManager.INTERRUPTION_FILTER_ALL
            }
            notificationManager?.setInterruptionFilter(filter)

            val modeName = when (action.dndMode) {
                DndMode.PRIORITY_ONLY -> "Priority Only"
                DndMode.TOTAL_SILENCE -> "Total Silence"
                DndMode.ALARMS_ONLY -> "Alarms Only"
                DndMode.OFF -> "Off (Normal)"
            }

            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.DO_NOT_DISTURB,
                status = ExecutionStatus.SUCCESS,
                message = "Do Not Disturb set to $modeName."
            )
        } catch (e: Exception) {
            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.DO_NOT_DISTURB,
                status = ExecutionStatus.FAILED,
                message = "Failed to set DND filter: ${e.message}"
            )
        }
    }

    private fun executeBatterySaverGuideAction(routine: Routine, action: RoutineAction): ExecutionResult {
        return try {
            val intent = permissionManager.getBatterySaverSettingsIntent()
            context.startActivity(intent)

            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.BATTERY_SAVER_GUIDE,
                status = ExecutionStatus.SUCCESS,
                message = "Directed to Battery Saver settings (direct toggle restricted to system/privileged ROM apps)."
            )
        } catch (e: Exception) {
            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.BATTERY_SAVER_GUIDE,
                status = ExecutionStatus.FAILED,
                message = "Could not open Battery Saver settings: ${e.message}"
            )
        }
    }

    private fun executeOpenSettingAction(routine: Routine, action: RoutineAction): ExecutionResult {
        return try {
            val intent = Intent(action.settingTarget.actionIntent).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.OPEN_SETTING,
                status = ExecutionStatus.SUCCESS,
                message = "Opened ${action.settingTarget.displayName}."
            )
        } catch (e: Exception) {
            ExecutionResult(
                routineId = routine.id,
                routineName = routine.name,
                actionType = ActionType.OPEN_SETTING,
                status = ExecutionStatus.FAILED,
                message = "Could not open ${action.settingTarget.displayName}: ${e.message}"
            )
        }
    }
}
