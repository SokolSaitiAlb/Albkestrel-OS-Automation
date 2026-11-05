package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AlbkestrelDatabase
import com.example.data.repository.RoutineRepositoryImpl
import com.example.domain.engine.ExecutionStatusResolver
import com.example.domain.engine.RuleEngine
import com.example.domain.model.ExecutionStatus
import com.example.platform.ActionExecutor
import com.example.platform.SystemStateMonitor

class AutomationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AutomationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val database = AlbkestrelDatabase.getInstance(applicationContext)
            val repository = RoutineRepositoryImpl(database.routineDao(), database.executionLogDao())
            val systemMonitor = SystemStateMonitor(applicationContext)
            val actionExecutor = ActionExecutor(applicationContext)

            val context = systemMonitor.getCurrentEvaluationContext(isManual = false)
            val enabledRoutines = repository.getEnabledRoutinesOnce()

            for (routine in enabledRoutines) {
                val shouldRun = RuleEngine.shouldExecute(routine, context)
                if (shouldRun) {
                    val actionResults = actionExecutor.executeActions(routine)
                    val overallStatus = ExecutionStatusResolver.resolveOverallStatus(actionResults)

                    val details = "Evaluated via periodic background worker at ${context.batteryPercentage}% battery"
                    val actionsSummary = actionResults.joinToString(" | ") { "${it.actionType.name}: ${it.status.name}" }

                    repository.recordExecution(
                        routineId = routine.id,
                        status = overallStatus,
                        details = details,
                        actionsSummary = actionsSummary
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during periodic automation evaluation", e)
            Result.retry()
        }
    }
}
