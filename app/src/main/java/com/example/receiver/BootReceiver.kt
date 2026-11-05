package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AlbkestrelDatabase
import com.example.data.repository.RoutineRepositoryImpl
import com.example.domain.engine.RoutineValidator
import com.example.domain.model.RoutineValidationResult
import com.example.platform.AutomationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val scheduler = AutomationScheduler(appContext)
                    scheduler.schedulePeriodicWork()

                    val database = AlbkestrelDatabase.getInstance(appContext)
                    val repository = RoutineRepositoryImpl(database.routineDao(), database.executionLogDao())

                    val enabledRoutines = repository.getEnabledRoutinesOnce()
                    var restoredCount = 0

                    for (routine in enabledRoutines) {
                        // Restore only enabled routines that pass schema validation
                        if (routine.isEnabled && RoutineValidator.validate(routine) is RoutineValidationResult.Valid) {
                            scheduler.scheduleRoutineAlarm(routine)
                            restoredCount++
                        } else {
                            Log.w(TAG, "Skipping invalid/disabled routine #${routine.id} during boot restore")
                            scheduler.cancelRoutineAlarm(routine.id)
                        }
                    }

                    Log.i(TAG, "Successfully restored $restoredCount enabled routines on boot completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring routines during boot recovery", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
