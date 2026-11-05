package com.example.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.domain.model.Routine
import com.example.domain.model.TriggerType
import com.example.receiver.RoutineAlarmReceiver
import com.example.worker.AutomationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AutomationScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val permissionManager = PermissionManager(context)

    companion object {
        const val PERIODIC_WORK_TAG = "albkestrel_periodic_automation"
        const val ACTION_ROUTINE_ALARM = "com.albkestrel.osautomation.ACTION_ROUTINE_ALARM"
        const val MAX_SCHEDULE_SLOTS = 10
        private const val TAG = "AutomationScheduler"

        /**
         * Deterministic request code calculation based on routine ID and slot index.
         */
        fun getAlarmRequestCode(routineId: Long, slot: Int): Int {
            return ((routineId * 31 + slot) and 0x7FFFFFFF).toInt()
        }

        /**
         * Deterministic Intent creation with explicit action and unique Data URI.
         * Ensures creation, updating, and cancellation use the EXACT same PendingIntent identity.
         */
        fun createAlarmIntent(context: Context, routineId: Long, slot: Int): Intent {
            return Intent(context, RoutineAlarmReceiver::class.java).apply {
                action = ACTION_ROUTINE_ALARM
                data = Uri.parse("albkestrel://routine/$routineId/slot/$slot")
                putExtra(RoutineAlarmReceiver.EXTRA_ROUTINE_ID, routineId)
                putExtra(RoutineAlarmReceiver.EXTRA_SLOT, slot)
            }
        }
    }

    /**
     * Schedules periodic WorkManager background evaluation.
     */
    fun schedulePeriodicWork() {
        try {
            val periodicRequest = PeriodicWorkRequestBuilder<AutomationWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // flex interval
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
        } catch (e: Exception) {
            Log.w(TAG, "WorkManager periodic scheduling skipped (expected in limited testing environments): ${e.message}")
        }
    }

    /**
     * Schedules AlarmManager for time-based schedule conditions in enabled routines.
     * Automatically cancels all previously scheduled alarm slots for this routine
     * to prevent orphaned or duplicate alarms when times change.
     */
    fun scheduleRoutineAlarm(routine: Routine) {
        // Cancel all existing slots first to guarantee clean update
        cancelRoutineAlarm(routine.id)

        if (!routine.isEnabled) return

        val scheduleConditions = routine.conditions.filter {
            it.isEnabled && it.type == TriggerType.SCHEDULE
        }

        scheduleConditions.forEachIndexed { slot, condition ->
            if (slot >= MAX_SCHEDULE_SLOTS) return@forEachIndexed

            val nextTriggerMillis = calculateNextTriggerTime(condition.hour, condition.minute, condition.daysOfWeek)
            if (nextTriggerMillis > System.currentTimeMillis()) {
                val intent = createAlarmIntent(context, routine.id, slot)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    getAlarmRequestCode(routine.id, slot),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleAlarmInternal(nextTriggerMillis, pendingIntent)
            }
        }
    }

    /**
     * Cancels all scheduled alarm slots for a routine using the exact same PendingIntent identity.
     */
    fun cancelRoutineAlarm(routineId: Long) {
        if (routineId <= 0) return
        val am = alarmManager ?: return

        for (slot in 0 until MAX_SCHEDULE_SLOTS) {
            val intent = createAlarmIntent(context, routineId, slot)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getAlarmRequestCode(routineId, slot),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                am.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun scheduleAlarmInternal(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val am = alarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (permissionManager.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    Log.i(TAG, "Exact alarm permission restricted; falling back to inexact idle alarm")
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm; falling back to inexact alarm: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to schedule inexact fallback alarm", fallbackEx)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scheduling alarm", e)
        }
    }

    fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeek: List<Int>): Long {
        if (daysOfWeek.isEmpty()) return 0L

        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check today through the next 7 days
        for (i in 0..7) {
            val checkCal = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            if (checkCal.timeInMillis > now.timeInMillis) {
                val calDay = checkCal.get(Calendar.DAY_OF_WEEK)
                val isoDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
                if (daysOfWeek.contains(isoDay)) {
                    return checkCal.timeInMillis
                }
            }
        }

        return candidate.timeInMillis + (24 * 60 * 60 * 1000L)
    }
}
