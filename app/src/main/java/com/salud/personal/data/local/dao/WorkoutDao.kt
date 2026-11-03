package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(session: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    suspend fun getAllWorkoutsList(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :since ORDER BY startTime DESC")
    fun getWorkoutsSince(since: Instant): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE type = :type ORDER BY startTime DESC")
    fun getWorkoutsByType(type: WorkoutType): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT SUM(trimp) FROM workout_sessions WHERE startTime >= :since")
    suspend fun getTotalTrimpSince(since: Instant): Double?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getWorkoutById(id: Long): WorkoutSessionEntity?

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)
}
