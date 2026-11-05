package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.local.converter.RoutineJsonConverter
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.MatchCondition
import com.example.domain.model.Routine

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val icon: String,
    val isEnabled: Boolean,
    val matchMode: String,
    val conditionsJson: String,
    val actionsJson: String,
    val lastExecutedAt: Long?,
    val lastExecutionStatus: String?,
    val createdAt: Long
) {
    fun toDomain(): Routine {
        return Routine(
            id = id,
            name = name,
            description = description,
            icon = icon,
            isEnabled = isEnabled,
            matchMode = try {
                MatchCondition.valueOf(matchMode)
            } catch (_: Exception) {
                MatchCondition.ALL
            },
            conditions = RoutineJsonConverter.jsonToConditions(conditionsJson),
            actions = RoutineJsonConverter.jsonToActions(actionsJson),
            lastExecutedAt = lastExecutedAt,
            lastExecutionStatus = lastExecutionStatus?.let {
                try { ExecutionStatus.valueOf(it) } catch (_: Exception) { null }
            },
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(routine: Routine): RoutineEntity {
            return RoutineEntity(
                id = routine.id,
                name = routine.name,
                description = routine.description,
                icon = routine.icon,
                isEnabled = routine.isEnabled,
                matchMode = routine.matchMode.name,
                conditionsJson = RoutineJsonConverter.conditionsToJson(routine.conditions),
                actionsJson = RoutineJsonConverter.actionsToJson(routine.actions),
                lastExecutedAt = routine.lastExecutedAt,
                lastExecutionStatus = routine.lastExecutionStatus?.name,
                createdAt = routine.createdAt
            )
        }
    }
}
