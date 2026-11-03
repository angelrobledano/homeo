package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.DailyActivityEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DailyActivityEntity>): List<Long>

    @Query("SELECT * FROM daily_activity WHERE date = :date AND sourceDevice = :sourceDevice LIMIT 1")
    fun getActivityForDate(date: String, sourceDevice: String): Flow<DailyActivityEntity?>

    fun getActivityForDate(date: LocalDate, sourceDevice: String): Flow<DailyActivityEntity?> {
        return getActivityForDate(date.toString(), sourceDevice)
    }

    @Query("SELECT * FROM daily_activity WHERE date = :date AND sourceDevice = :sourceDevice LIMIT 1")
    suspend fun getActivityForDateSync(date: String, sourceDevice: String): DailyActivityEntity?

    suspend fun getActivityForDateSync(date: LocalDate, sourceDevice: String): DailyActivityEntity? {
        return getActivityForDateSync(date.toString(), sourceDevice)
    }

    @Query("SELECT * FROM daily_activity ORDER BY date DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    fun getAllDailyActivitiesFlow(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    suspend fun getAllDailyActivities(): List<DailyActivityEntity>

    @Query("SELECT COUNT(*) FROM daily_activity")
    suspend fun count(): Int

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll(): Int
}
