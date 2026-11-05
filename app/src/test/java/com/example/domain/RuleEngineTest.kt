package com.example.domain

import com.example.domain.engine.RoutineValidator
import com.example.domain.engine.RuleEngine
import com.example.domain.model.ActionType
import com.example.domain.model.MatchCondition
import com.example.domain.model.Routine
import com.example.domain.model.RoutineAction
import com.example.domain.model.RoutineCondition
import com.example.domain.model.RoutineValidationResult
import com.example.domain.model.SystemEvaluationContext
import com.example.domain.model.TriggerType
import com.example.domain.model.WifiTriggerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    @Test
    fun `matchCondition ALL is satisfied only when all enabled conditions match`() {
        val scheduleCond = RoutineCondition(
            id = "c1",
            type = TriggerType.SCHEDULE,
            hour = 8,
            minute = 30,
            daysOfWeek = listOf(1, 2, 3, 4, 5) // Mon-Fri
        )
        val batteryCond = RoutineCondition(
            id = "c2",
            type = TriggerType.BATTERY,
            batteryThreshold = 30,
            triggerWhenCharging = false
        )

        val routine = Routine(
            id = 1,
            name = "Morning Low Battery Routine",
            matchMode = MatchCondition.ALL,
            conditions = listOf(scheduleCond, batteryCond),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION))
        )

        // Case 1: Both match (Monday 8:30, battery 20%)
        val ctxBothMatch = SystemEvaluationContext(
            currentHour = 8,
            currentMinute = 30,
            currentDayOfWeek = 1,
            batteryPercentage = 20
        )
        val resultBoth = RuleEngine.evaluateConditions(routine, ctxBothMatch)
        assertTrue(resultBoth.isSatisfied)
        assertEquals(2, resultBoth.matchedConditionIds.size)

        // Case 2: Only schedule matches, battery is 80% (>30%)
        val ctxOnlySchedule = SystemEvaluationContext(
            currentHour = 8,
            currentMinute = 30,
            currentDayOfWeek = 1,
            batteryPercentage = 80
        )
        val resultOnlySchedule = RuleEngine.evaluateConditions(routine, ctxOnlySchedule)
        assertFalse(resultOnlySchedule.isSatisfied)
        assertEquals(1, resultOnlySchedule.matchedConditionIds.size)
    }

    @Test
    fun `matchCondition ANY is satisfied when any enabled condition matches`() {
        val batteryCond = RoutineCondition(
            id = "c1",
            type = TriggerType.BATTERY,
            batteryThreshold = 15
        )
        val wifiCond = RoutineCondition(
            id = "c2",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED
        )

        val routine = Routine(
            id = 2,
            name = "Any Condition Routine",
            matchMode = MatchCondition.ANY,
            conditions = listOf(batteryCond, wifiCond),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION))
        )

        // Case: Battery is 80% (fails) but Wi-Fi is connected (matches)
        val context = SystemEvaluationContext(
            batteryPercentage = 80,
            isWifiConnected = true
        )
        val result = RuleEngine.evaluateConditions(routine, context)
        assertTrue(result.isSatisfied)
        assertEquals(listOf("c2"), result.matchedConditionIds)
    }

    @Test
    fun `disabled condition is ignored during evaluation`() {
        val enabledBattery = RoutineCondition(
            id = "c1",
            type = TriggerType.BATTERY,
            batteryThreshold = 50,
            isEnabled = true
        )
        val disabledSchedule = RoutineCondition(
            id = "c2",
            type = TriggerType.SCHEDULE,
            hour = 12,
            minute = 0,
            daysOfWeek = listOf(6, 7),
            isEnabled = false // disabled
        )

        val routine = Routine(
            id = 3,
            name = "Partial Disabled Routine",
            matchMode = MatchCondition.ALL,
            conditions = listOf(enabledBattery, disabledSchedule),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION))
        )

        // Evaluation context where schedule would fail (Monday 10:00) but battery is 40% (passes)
        val context = SystemEvaluationContext(
            currentHour = 10,
            currentMinute = 0,
            currentDayOfWeek = 1,
            batteryPercentage = 40
        )

        val result = RuleEngine.evaluateConditions(routine, context)
        // Should pass because disabled condition is ignored
        assertTrue(result.isSatisfied)
    }

    @Test
    fun `disabled routine should never execute`() {
        val routine = Routine(
            id = 4,
            name = "Disabled Routine",
            isEnabled = false,
            conditions = listOf(RoutineCondition(type = TriggerType.BATTERY, batteryThreshold = 100)),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION))
        )

        val context = SystemEvaluationContext(batteryPercentage = 50)
        assertFalse(RuleEngine.shouldExecute(routine, context))
    }

    @Test
    fun `duplicate execution is prevented within cooldown window`() {
        val routine = Routine(
            id = 5,
            name = "Cooldown Test Routine",
            isEnabled = true,
            conditions = listOf(RoutineCondition(type = TriggerType.BATTERY, batteryThreshold = 50)),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION)),
            lastExecutedAt = 1000000L
        )

        // 2 minutes after last run (within 5-min cooldown)
        val contextWithinCooldown = SystemEvaluationContext(
            currentTimestamp = 1000000L + (2 * 60 * 1000L),
            batteryPercentage = 40,
            isManualInvocation = false
        )
        assertFalse(RuleEngine.shouldExecute(routine, contextWithinCooldown))

        // 10 minutes after last run (outside cooldown)
        val contextAfterCooldown = SystemEvaluationContext(
            currentTimestamp = 1000000L + (10 * 60 * 1000L),
            batteryPercentage = 40,
            isManualInvocation = false
        )
        assertTrue(RuleEngine.shouldExecute(routine, contextAfterCooldown))
    }

    @Test
    fun `manual invocation bypasses cooldown and automatic conditions`() {
        val routine = Routine(
            id = 6,
            name = "Manual Run Routine",
            isEnabled = true,
            conditions = listOf(RoutineCondition(type = TriggerType.BATTERY, batteryThreshold = 10)),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION)),
            lastExecutedAt = 5000L
        )

        // Battery is 90% (would fail auto) and timestamp is 1 second later (would fail cooldown)
        val manualContext = SystemEvaluationContext(
            currentTimestamp = 6000L,
            batteryPercentage = 90,
            isManualInvocation = true
        )
        assertTrue(RuleEngine.shouldExecute(routine, manualContext))
    }

    @Test
    fun `routine validator rejects invalid routine configurations`() {
        // Blank name
        val invalidName = Routine(
            id = 0,
            name = "   ",
            conditions = listOf(RoutineCondition(type = TriggerType.MANUAL)),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION))
        )
        assertTrue(RoutineValidator.validate(invalidName) is RoutineValidationResult.Invalid)

        // No conditions
        val noConditions = Routine(
            id = 0,
            name = "Valid Name",
            conditions = emptyList(),
            actions = listOf(RoutineAction(type = ActionType.NOTIFICATION))
        )
        assertTrue(RoutineValidator.validate(noConditions) is RoutineValidationResult.Invalid)

        // No actions
        val noActions = Routine(
            id = 0,
            name = "Valid Name",
            conditions = listOf(RoutineCondition(type = TriggerType.MANUAL)),
            actions = emptyList()
        )
        assertTrue(RoutineValidator.validate(noActions) is RoutineValidationResult.Invalid)

        // Valid routine
        val validRoutine = Routine(
            id = 0,
            name = "Bedtime Routine",
            conditions = listOf(RoutineCondition(type = TriggerType.SCHEDULE, hour = 22, minute = 30, daysOfWeek = listOf(1, 2))),
            actions = listOf(RoutineAction(type = ActionType.DO_NOT_DISTURB))
        )
        assertTrue(RoutineValidator.validate(validRoutine) is RoutineValidationResult.Valid)
    }

    // ==========================================
    // Phase 2.2 Wi-Fi Trigger Correctness Tests
    // ==========================================

    @Test
    fun `wifi condition matches when connected to correct specific SSID`() {
        val condition = RoutineCondition(
            id = "w1",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED,
            targetSsid = "Pixel_Home_5G"
        )
        val context = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "Pixel_Home_5G"
        )
        assertTrue(RuleEngine.evaluateSingleCondition(condition, context))
    }

    @Test
    fun `wifi condition fails when connected to a different SSID`() {
        val condition = RoutineCondition(
            id = "w2",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED,
            targetSsid = "Pixel_Home_5G"
        )
        val context = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "CoffeeShop_Guest"
        )
        assertFalse(RuleEngine.evaluateSingleCondition(condition, context))
    }

    @Test
    fun `wifi condition fails when SSID is null or redacted`() {
        val condition = RoutineCondition(
            id = "w3",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED,
            targetSsid = "Pixel_Home_5G"
        )

        // Null SSID
        val contextNull = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = null
        )
        assertFalse(RuleEngine.evaluateSingleCondition(condition, contextNull))

        // Redacted unknown SSID
        val contextUnknown = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "<unknown ssid>"
        )
        assertFalse(RuleEngine.evaluateSingleCondition(condition, contextUnknown))

        // Quoted unknown SSID
        val contextQuotedUnknown = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "\"<unknown ssid>\""
        )
        assertFalse(RuleEngine.evaluateSingleCondition(condition, contextQuotedUnknown))

        // Blank/0x placeholder
        val contextHex = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "0x"
        )
        assertFalse(RuleEngine.evaluateSingleCondition(condition, contextHex))
    }

    @Test
    fun `wifi condition fails when wifi is disconnected despite SSID populated`() {
        val condition = RoutineCondition(
            id = "w4",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED,
            targetSsid = "Pixel_Home_5G"
        )
        val context = SystemEvaluationContext(
            isWifiConnected = false,
            currentWifiSsid = "Pixel_Home_5G"
        )
        assertFalse(RuleEngine.evaluateSingleCondition(condition, context))
    }

    @Test
    fun `wifi disconnected condition succeeds when disconnected`() {
        val condition = RoutineCondition(
            id = "w5",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.DISCONNECTED
        )
        val contextDisconnected = SystemEvaluationContext(isWifiConnected = false)
        assertTrue(RuleEngine.evaluateSingleCondition(condition, contextDisconnected))

        val contextConnected = SystemEvaluationContext(isWifiConnected = true)
        assertFalse(RuleEngine.evaluateSingleCondition(condition, contextConnected))
    }

    @Test
    fun `wifi condition correctly normalizes SSID quotes and whitespace`() {
        val condition = RoutineCondition(
            id = "w6",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED,
            targetSsid = "  \"Akita_Lab\"  "
        )
        // System returning raw quoted SSID
        val context = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "\"Akita_Lab\""
        )
        assertTrue(RuleEngine.evaluateSingleCondition(condition, context))
    }

    @Test
    fun `wifi condition without target SSID matches any connected wifi`() {
        val condition = RoutineCondition(
            id = "w7",
            type = TriggerType.WIFI,
            wifiState = WifiTriggerState.CONNECTED,
            targetSsid = null
        )
        // Even if SSID is redacted/unknown, generic "connected to wifi" matches
        val context = SystemEvaluationContext(
            isWifiConnected = true,
            currentWifiSsid = "<unknown ssid>"
        )
        assertTrue(RuleEngine.evaluateSingleCondition(condition, context))
    }
}
