package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ExecutionLog
import com.example.domain.model.ExecutionStatus

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val routineName: String,
    val timestamp: Long,
    val status: String,
    val details: String,
    val actionResultsSummary: String
) {
    fun toDomain(): ExecutionLog {
        return ExecutionLog(
            id = id,
            routineId = routineId,
            routineName = routineName,
            timestamp = timestamp,
            status = try {
                ExecutionStatus.valueOf(status)
            } catch (_: Exception) {
                ExecutionStatus.FAILED
            },
            details = details,
            actionResultsSummary = actionResultsSummary
        )
    }

    companion object {
        fun fromDomain(log: ExecutionLog): ExecutionLogEntity {
            return ExecutionLogEntity(
                id = log.id,
                routineId = log.routineId,
                routineName = log.routineName,
                timestamp = log.timestamp,
                status = log.status.name,
                details = log.details,
                actionResultsSummary = log.actionResultsSummary
            )
        }
    }
}
