package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AlbkestrelDatabase
import com.example.data.repository.RoutineRepositoryImpl
import com.example.domain.engine.ExecutionStatusResolver
import com.example.domain.engine.RuleEngine
import com.example.domain.model.ExecutionStatus
import com.example.platform.ActionExecutor
import com.example.platform.AutomationScheduler
import com.example.platform.SystemStateMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutineAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_SLOT = "extra_slot"
        private const val TAG = "RoutineAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        if (routineId <= 0) {
            Log.w(TAG, "Received alarm with invalid routine ID: $routineId")
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AlbkestrelDatabase.getInstance(appContext)
                val repository = RoutineRepositoryImpl(database.routineDao(), database.executionLogDao())
                val systemMonitor = SystemStateMonitor(appContext)
                val actionExecutor = ActionExecutor(appContext)
                val scheduler = AutomationScheduler(appContext)

                val routine = repository.getRoutineById(routineId)
                if (routine == null) {
                    Log.d(TAG, "Routine $routineId not found; canceling any orphan alarms")
                    scheduler.cancelRoutineAlarm(routineId)
                    return@launch
                }

                if (!routine.isEnabled) {
                    Log.d(TAG, "Routine $routineId is disabled; skipping execution and clearing alarms")
                    scheduler.cancelRoutineAlarm(routineId)
                    return@launch
                }

                val evalContext = systemMonitor.getCurrentEvaluationContext(isManual = false)
                val shouldRun = RuleEngine.shouldExecute(routine, evalContext)

                if (shouldRun) {
                    val actionResults = actionExecutor.executeActions(routine)
                    val overallStatus = ExecutionStatusResolver.resolveOverallStatus(actionResults)

                    val details = "Scheduled alarm executed at ${String.format("%02d:%02d", evalContext.currentHour, evalContext.currentMinute)}"
                    val actionsSummary = actionResults.joinToString(" | ") { "${it.actionType.name}: ${it.status.name}" }

                    repository.recordExecution(
                        routineId = routine.id,
                        status = overallStatus,
                        details = details,
                        actionsSummary = actionsSummary
                    )
                }

                // Reschedule next occurrence for this enabled routine
                scheduler.scheduleRoutineAlarm(routine)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing routine alarm for ID $routineId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
