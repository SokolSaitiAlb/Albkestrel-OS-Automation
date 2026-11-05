package com.example.domain.engine

import com.example.domain.model.ActionType
import com.example.domain.model.Routine
import com.example.domain.model.RoutineValidationResult
import com.example.domain.model.TriggerType

object RoutineValidator {

    fun validate(routine: Routine): RoutineValidationResult {
        val errors = mutableListOf<String>()

        if (routine.name.trim().isBlank()) {
            errors.add("Routine name cannot be empty.")
        } else if (routine.name.length > 50) {
            errors.add("Routine name must be under 50 characters.")
        }

        if (routine.conditions.isEmpty()) {
            errors.add("Add at least one trigger condition.")
        } else {
            val enabledConditions = routine.conditions.filter { it.isEnabled }
            if (enabledConditions.isEmpty()) {
                errors.add("At least one trigger condition must be enabled.")
            }
            for (c in enabledConditions) {
                when (c.type) {
                    TriggerType.SCHEDULE -> {
                        if (c.daysOfWeek.isEmpty()) {
                            errors.add("Schedule trigger must have at least one day selected.")
                        }
                        if (c.hour !in 0..23 || c.minute !in 0..59) {
                            errors.add("Schedule trigger has invalid time (${c.hour}:${c.minute}).")
                        }
                    }
                    TriggerType.BATTERY -> {
                        if (!c.triggerWhenCharging && c.batteryThreshold !in 1..100) {
                            errors.add("Battery threshold must be between 1% and 100%.")
                        }
                    }
                    TriggerType.MANUAL, TriggerType.WIFI -> {
                        // valid
                    }
                }
            }
        }

        if (routine.actions.isEmpty()) {
            errors.add("Add at least one action to execute.")
        } else {
            for (a in routine.actions) {
                when (a.type) {
                    ActionType.NOTIFICATION -> {
                        if (a.notificationTitle.trim().isBlank()) {
                            errors.add("Notification action requires a title.")
                        }
                    }
                    ActionType.OPEN_APP -> {
                        if (a.targetPackageName.trim().isBlank()) {
                            errors.add("Launch App action requires selecting an installed app.")
                        }
                    }
                    ActionType.DO_NOT_DISTURB, ActionType.BATTERY_SAVER_GUIDE, ActionType.OPEN_SETTING -> {
                        // Always valid
                    }
                }
            }
        }

        return if (errors.isEmpty()) {
            RoutineValidationResult.Valid
        } else {
            RoutineValidationResult.Invalid(errors)
        }
    }
}
