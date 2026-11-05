package com.example.domain.engine

import com.example.domain.model.ConditionEvaluationResult
import com.example.domain.model.MatchCondition
import com.example.domain.model.Routine
import com.example.domain.model.RoutineCondition
import com.example.domain.model.SystemEvaluationContext
import com.example.domain.model.TriggerType
import com.example.domain.model.WifiTriggerState

object RuleEngine {

    // Default cooldown interval to prevent immediate repeat triggers (e.g. 5 minutes)
    const val DEFAULT_COOLDOWN_MILLIS: Long = 5 * 60 * 1000L

    /**
     * Normalizes an SSID string by trimming leading/trailing whitespace and surrounding quotes.
     * Returns null if the result is empty or represents an invalid SSID.
     */
    fun normalizeSsid(ssid: String?): String? {
        if (ssid == null) return null
        val unquoted = ssid.trim().removeSurrounding("\"").trim()
        return if (unquoted.isEmpty()) null else unquoted
    }

    /**
     * Checks if an SSID string represents Android's redacted or unknown SSID placeholder.
     */
    fun isUnknownOrRedactedSsid(ssid: String?): Boolean {
        if (ssid == null) return true
        val normalized = normalizeSsid(ssid) ?: return true
        return normalized.equals("<unknown ssid>", ignoreCase = true) ||
                normalized.equals("0x", ignoreCase = true) ||
                normalized.equals("02:00:00:00:00:00", ignoreCase = true)
    }

    /**
     * Determines whether a routine should execute given the current system context and history.
     */
    fun shouldExecute(
        routine: Routine,
        context: SystemEvaluationContext,
        cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS
    ): Boolean {
        if (!routine.isEnabled) return false

        // Check duplicate execution cooldown for automatic triggers
        if (!context.isManualInvocation) {
            val lastRun = routine.lastExecutedAt
            if (lastRun != null && (context.currentTimestamp - lastRun) < cooldownMillis) {
                return false
            }
        }

        val evalResult = evaluateConditions(routine, context)
        return evalResult.isSatisfied
    }

    /**
     * Evaluates conditions for a routine.
     */
    fun evaluateConditions(
        routine: Routine,
        context: SystemEvaluationContext
    ): ConditionEvaluationResult {
        // If manual trigger explicitly requested, always matches if routine has manual condition or is manual invocation
        if (context.isManualInvocation) {
            return ConditionEvaluationResult(
                isSatisfied = true,
                matchedConditionIds = emptyList(),
                reason = "Manually triggered by user"
            )
        }

        val enabledConditions = routine.conditions.filter { it.isEnabled }
        if (enabledConditions.isEmpty()) {
            return ConditionEvaluationResult(
                isSatisfied = false,
                matchedConditionIds = emptyList(),
                reason = "No enabled conditions configured"
            )
        }

        val matchedIds = mutableListOf<String>()

        for (condition in enabledConditions) {
            if (evaluateSingleCondition(condition, context)) {
                matchedIds.add(condition.id)
            }
        }

        val isSatisfied = when (routine.matchMode) {
            MatchCondition.ALL -> matchedIds.size == enabledConditions.size
            MatchCondition.ANY -> matchedIds.isNotEmpty()
        }

        val reason = when {
            isSatisfied -> "Matched ${matchedIds.size}/${enabledConditions.size} condition(s) via ${routine.matchMode}"
            else -> "Failed condition match: ${matchedIds.size}/${enabledConditions.size} satisfied (${routine.matchMode} required)"
        }

        return ConditionEvaluationResult(
            isSatisfied = isSatisfied,
            matchedConditionIds = matchedIds,
            reason = reason
        )
    }

    fun evaluateSingleCondition(
        condition: RoutineCondition,
        context: SystemEvaluationContext
    ): Boolean {
        if (!condition.isEnabled) return false

        return when (condition.type) {
            TriggerType.MANUAL -> {
                context.isManualInvocation
            }
            TriggerType.SCHEDULE -> {
                val dayMatches = condition.daysOfWeek.contains(context.currentDayOfWeek)
                val timeMatches = (context.currentHour == condition.hour) &&
                        (Math.abs(context.currentMinute - condition.minute) <= context.minuteTolerance)
                dayMatches && timeMatches
            }
            TriggerType.BATTERY -> {
                if (condition.triggerWhenCharging) {
                    context.isBatteryCharging
                } else {
                    context.batteryPercentage <= condition.batteryThreshold
                }
            }
            TriggerType.WIFI -> {
                val stateMatches = when (condition.wifiState) {
                    WifiTriggerState.CONNECTED -> context.isWifiConnected
                    WifiTriggerState.DISCONNECTED -> !context.isWifiConnected
                }
                if (!stateMatches) return false

                // If disconnected trigger, state alone is sufficient
                if (condition.wifiState == WifiTriggerState.DISCONNECTED) {
                    return true
                }

                // If connected trigger and a target SSID is specified, verify specific network match
                val targetSsidNormalized = normalizeSsid(condition.targetSsid)
                if (targetSsidNormalized != null) {
                    val currentSsidNormalized = normalizeSsid(context.currentWifiSsid)
                    // If current SSID is unavailable, redacted, or unknown, never treat as match!
                    if (currentSsidNormalized == null || isUnknownOrRedactedSsid(currentSsidNormalized)) {
                        false
                    } else {
                        currentSsidNormalized.equals(targetSsidNormalized, ignoreCase = false)
                    }
                } else {
                    // Connected to any Wi-Fi network without SSID restriction
                    true
                }
            }
        }
    }
}
