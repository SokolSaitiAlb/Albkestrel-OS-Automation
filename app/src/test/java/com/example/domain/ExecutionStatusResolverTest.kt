package com.example.domain

import com.example.domain.engine.ExecutionStatusResolver
import com.example.domain.model.ActionType
import com.example.domain.model.ExecutionResult
import com.example.domain.model.ExecutionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecutionStatusResolverTest {

    @Test
    fun `empty action results resolves to SKIPPED`() {
        val result = ExecutionStatusResolver.resolveOverallStatus(emptyList())
        assertEquals(ExecutionStatus.SKIPPED, result)
    }

    @Test
    fun `all successful actions resolve to SUCCESS`() {
        val results = listOf(
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SUCCESS,
                message = "Notification displayed"
            ),
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.DO_NOT_DISTURB,
                status = ExecutionStatus.SUCCESS,
                message = "DND enabled"
            )
        )
        val overall = ExecutionStatusResolver.resolveOverallStatus(results)
        assertEquals(ExecutionStatus.SUCCESS, overall)
    }

    @Test
    fun `mixed outcome with permission required resolves to PERMISSION_REQUIRED`() {
        val results = listOf(
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SUCCESS,
                message = "Notification posted"
            ),
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.DO_NOT_DISTURB,
                status = ExecutionStatus.PERMISSION_REQUIRED,
                message = "Notification Policy Access missing"
            )
        )
        val overall = ExecutionStatusResolver.resolveOverallStatus(results)
        // Must NEVER report partial completion as full SUCCESS
        assertEquals(ExecutionStatus.PERMISSION_REQUIRED, overall)
    }

    @Test
    fun `mixed outcome with failure resolves to FAILED`() {
        val results = listOf(
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SUCCESS,
                message = "Notification posted"
            ),
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.OPEN_APP,
                status = ExecutionStatus.FAILED,
                message = "ActivityNotFoundException"
            )
        )
        val overall = ExecutionStatusResolver.resolveOverallStatus(results)
        assertEquals(ExecutionStatus.FAILED, overall)
    }

    @Test
    fun `mixed outcome with unsupported action resolves to UNSUPPORTED`() {
        val results = listOf(
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SUCCESS,
                message = "Notification posted"
            ),
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.OPEN_APP,
                status = ExecutionStatus.UNSUPPORTED,
                message = "App package not installed"
            )
        )
        val overall = ExecutionStatusResolver.resolveOverallStatus(results)
        // Unsupported operations must never produce success
        assertEquals(ExecutionStatus.UNSUPPORTED, overall)
    }

    @Test
    fun `all skipped actions resolve to SKIPPED`() {
        val results = listOf(
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SKIPPED,
                message = "Inapplicable"
            )
        )
        val overall = ExecutionStatusResolver.resolveOverallStatus(results)
        assertEquals(ExecutionStatus.SKIPPED, overall)
    }

    @Test
    fun `mixed success and skipped actions resolve to SUCCESS`() {
        val results = listOf(
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.NOTIFICATION,
                status = ExecutionStatus.SUCCESS,
                message = "Notification posted"
            ),
            ExecutionResult(
                routineId = 1,
                routineName = "Test",
                actionType = ActionType.DO_NOT_DISTURB,
                status = ExecutionStatus.SKIPPED,
                message = "Skipped"
            )
        )
        val overall = ExecutionStatusResolver.resolveOverallStatus(results)
        assertEquals(ExecutionStatus.SUCCESS, overall)
    }
}
