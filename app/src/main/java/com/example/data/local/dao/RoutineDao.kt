package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    fun getEnabledRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun getEnabledRoutinesOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutineById(id: Long): Flow<RoutineEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRoutineEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE routines SET lastExecutedAt = :timestamp, lastExecutionStatus = :status WHERE id = :id")
    suspend fun updateLastExecution(id: Long, timestamp: Long, status: String)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)

    @Query("SELECT COUNT(*) FROM routines WHERE isEnabled = 1")
    fun getActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun getCount(): Int
}
