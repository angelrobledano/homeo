package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HeartRateSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HeartRateSampleEntity>): List<Long>

    @Query("SELECT * FROM heart_rate_samples ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSample(): Flow<HeartRateSampleEntity?>

    @Query("SELECT * FROM heart_rate_samples WHERE timestamp BETWEEN :fromTimestamp AND :toTimestamp ORDER BY timestamp ASC")
    suspend fun getSamplesInRange(fromTimestamp: Long, toTimestamp: Long): List<HeartRateSampleEntity>

    suspend fun getSamplesInRange(from: Instant, to: Instant): List<HeartRateSampleEntity> {
        return getSamplesInRange(from.toEpochMilli(), to.toEpochMilli())
    }

    @Query("SELECT COUNT(*) FROM heart_rate_samples")
    suspend fun count(): Int

    @Query("SELECT * FROM heart_rate_samples ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSampleSync(): HeartRateSampleEntity?

    @Query("SELECT * FROM heart_rate_samples ORDER BY timestamp DESC")
    fun getAllSamples(): Flow<List<HeartRateSampleEntity>>

    @Query("SELECT * FROM heart_rate_samples ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSamplesList(limit: Int = 2000): List<HeartRateSampleEntity>

    @Query("DELETE FROM heart_rate_samples")
    suspend fun deleteAll(): Int
}
