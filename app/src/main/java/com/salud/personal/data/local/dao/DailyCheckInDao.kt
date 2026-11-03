package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.DailyCheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyCheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(checkIn: DailyCheckInEntity): Long

    @Query("SELECT * FROM daily_checkins WHERE date = :date")
    suspend fun getCheckInForDate(date: LocalDate): DailyCheckInEntity?

    @Query("SELECT * FROM daily_checkins WHERE date = :date")
    fun observeCheckInForDate(date: LocalDate): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_checkins ORDER BY date DESC")
    fun getAllCheckIns(): Flow<List<DailyCheckInEntity>>

    @Query("SELECT * FROM daily_checkins ORDER BY date DESC")
    suspend fun getAllCheckInsList(): List<DailyCheckInEntity>

    @Query("SELECT AVG(restingHeartRate) FROM daily_checkins WHERE restingHeartRate IS NOT NULL AND date >= :since")
    suspend fun getAverageRhrSince(since: LocalDate): Double?
}
