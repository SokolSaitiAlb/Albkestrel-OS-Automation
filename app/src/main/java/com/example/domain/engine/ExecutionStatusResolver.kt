package com.example.domain.engine

import com.example.domain.model.ExecutionResult
import com.example.domain.model.ExecutionStatus

object ExecutionStatusResolver {

    /**
     * Resolves an honest aggregate ExecutionStatus from a list of action results.
     *
     * Truthful aggregation policy:
     * - Empty actions list -> SKIPPED (nothing was executed)
     * - If any action requires special/runtime permission -> PERMISSION_REQUIRED
     * - If any action failed with an exception or error -> FAILED
     * - If any action is unsupported on this platform -> UNSUPPORTED
     * - If all actions were skipped -> SKIPPED
     * - Only if all executed actions succeeded without failure/unsupported/permission_required -> SUCCESS
     */
    fun resolveOverallStatus(results: List<ExecutionResult>): ExecutionStatus {
        if (results.isEmpty()) return ExecutionStatus.SKIPPED

        val hasPermissionRequired = results.any { it.status == ExecutionStatus.PERMISSION_REQUIRED }
        val hasFailed = results.any { it.status == ExecutionStatus.FAILED }
        val hasUnsupported = results.any { it.status == ExecutionStatus.UNSUPPORTED }
        val allSkipped = results.all { it.status == ExecutionStatus.SKIPPED }
        val hasSuccess = results.any { it.status == ExecutionStatus.SUCCESS }

        return when {
            hasPermissionRequired -> ExecutionStatus.PERMISSION_REQUIRED
            hasFailed -> ExecutionStatus.FAILED
            hasUnsupported -> ExecutionStatus.UNSUPPORTED
            allSkipped -> ExecutionStatus.SKIPPED
            hasSuccess -> ExecutionStatus.SUCCESS
            else -> ExecutionStatus.FAILED
        }
    }
}
