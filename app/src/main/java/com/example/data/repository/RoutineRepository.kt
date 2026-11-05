package com.example.data.repository

import com.example.data.local.dao.ExecutionLogDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.entity.ExecutionLogEntity
import com.example.data.local.entity.RoutineEntity
import com.example.domain.model.ExecutionLog
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RoutineRepository {
    fun getAllRoutines(): Flow<List<Routine>>
    fun getEnabledRoutines(): Flow<List<Routine>>
    suspend fun getEnabledRoutinesOnce(): List<Routine>
    fun observeRoutineById(id: Long): Flow<Routine?>
    suspend fun getRoutineById(id: Long): Routine?
    suspend fun saveRoutine(routine: Routine): Long
    suspend fun deleteRoutine(id: Long)
    suspend fun duplicateRoutine(id: Long): Long?
    suspend fun setRoutineEnabled(id: Long, isEnabled: Boolean)
    suspend fun recordExecution(routineId: Long, status: ExecutionStatus, details: String, actionsSummary: String)
    fun getActiveCount(): Flow<Int>
    fun getRecentLogs(): Flow<List<ExecutionLog>>
    fun getLatestFiveLogs(): Flow<List<ExecutionLog>>
    suspend fun clearHistory()
}

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao,
    private val logDao: ExecutionLogDao
) : RoutineRepository {

    override fun getAllRoutines(): Flow<List<Routine>> {
        return routineDao.getAllRoutines().map { list -> list.map { it.toDomain() } }
    }

    override fun getEnabledRoutines(): Flow<List<Routine>> {
        return routineDao.getEnabledRoutines().map { list -> list.map { it.toDomain() } }
    }

    override fun observeRoutineById(id: Long): Flow<Routine?> {
        return routineDao.observeRoutineById(id).map { it?.toDomain() }
    }

    override suspend fun getEnabledRoutinesOnce(): List<Routine> {
        return routineDao.getEnabledRoutinesOnce().map { it.toDomain() }
    }

    override suspend fun getRoutineById(id: Long): Routine? {
        return routineDao.getRoutineById(id)?.toDomain()
    }

    override suspend fun saveRoutine(routine: Routine): Long {
        val entity = RoutineEntity.fromDomain(routine)
        return if (routine.id == 0L) {
            routineDao.insertRoutine(entity)
        } else {
            routineDao.updateRoutine(entity)
            routine.id
        }
    }

    override suspend fun deleteRoutine(id: Long) {
        routineDao.deleteRoutineById(id)
    }

    override suspend fun duplicateRoutine(id: Long): Long? {
        val original = routineDao.getRoutineById(id)?.toDomain() ?: return null
        val copy = original.copy(
            id = 0,
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            lastExecutedAt = null,
            lastExecutionStatus = null
        )
        return routineDao.insertRoutine(RoutineEntity.fromDomain(copy))
    }

    override suspend fun setRoutineEnabled(id: Long, isEnabled: Boolean) {
        routineDao.setRoutineEnabled(id, isEnabled)
    }

    override suspend fun recordExecution(
        routineId: Long,
        status: ExecutionStatus,
        details: String,
        actionsSummary: String
    ) {
        val timestamp = System.currentTimeMillis()
        val routine = routineDao.getRoutineById(routineId)?.toDomain()
        val routineName = routine?.name ?: "Routine #$routineId"

        routineDao.updateLastExecution(routineId, timestamp, status.name)

        logDao.insertLog(
            ExecutionLogEntity(
                routineId = routineId,
                routineName = routineName,
                timestamp = timestamp,
                status = status.name,
                details = details,
                actionResultsSummary = actionsSummary
            )
        )
    }

    override fun getActiveCount(): Flow<Int> = routineDao.getActiveCount()

    override fun getRecentLogs(): Flow<List<ExecutionLog>> {
        return logDao.getRecentLogs().map { list -> list.map { it.toDomain() } }
    }

    override fun getLatestFiveLogs(): Flow<List<ExecutionLog>> {
        return logDao.getLatestFiveLogs().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun clearHistory() {
        logDao.clearAllLogs()
    }
}
