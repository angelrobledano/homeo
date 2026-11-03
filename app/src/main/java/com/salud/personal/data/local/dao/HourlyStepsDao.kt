package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.HourlyStepsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HourlyStepsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: HourlyStepsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<HourlyStepsEntity>)

    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    fun getHourlyStepsForDate(date: LocalDate): Flow<List<HourlyStepsEntity>>

    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    suspend fun getHourlyStepsForDateOnce(date: LocalDate): List<HourlyStepsEntity>

    @Query("SELECT SUM(steps) FROM hourly_steps WHERE date = :date")
    suspend fun getTotalStepsForDate(date: LocalDate): Int?

    @Query("DELETE FROM hourly_steps WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: LocalDate): Int

    @Query("DELETE FROM hourly_steps WHERE source_device != 'Amazfit GTR 3' AND date IN (SELECT DISTINCT date FROM hourly_steps WHERE source_device = 'Amazfit GTR 3')")
    suspend fun cleanDuplicatePhoneSteps(): Int
}
