package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SleepSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: SleepSessionEntity): Long

    @Query("SELECT * FROM sleep_sessions WHERE date = :date")
    fun observeSleepForDate(date: LocalDate): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE date = :date")
    suspend fun getSleepForDate(date: LocalDate): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions ORDER BY date DESC LIMIT :limit")
    fun getRecentSleep(limit: Int): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions ORDER BY date DESC")
    fun getAllSleep(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions ORDER BY date DESC")
    suspend fun getAllSleepList(): List<SleepSessionEntity>

    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAll(): Int
}
