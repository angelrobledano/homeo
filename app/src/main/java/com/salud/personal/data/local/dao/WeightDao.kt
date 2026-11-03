package com.salud.personal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.repository.WeightDaoInterface
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao : WeightDaoInterface {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insert(entity: WeightMeasurementEntity): Long

    @Query("SELECT * FROM weight_measurements ORDER BY measuredAtEpochMilli DESC LIMIT 1")
    override suspend fun getLatest(): WeightMeasurementEntity?

    @Query("SELECT * FROM weight_measurements ORDER BY measuredAtEpochMilli DESC LIMIT 1")
    override fun observeLatest(): Flow<WeightMeasurementEntity?>

    @Query("SELECT * FROM weight_measurements ORDER BY measuredAtEpochMilli DESC")
    override suspend fun getAll(): List<WeightMeasurementEntity>

    @Query("SELECT * FROM weight_measurements ORDER BY measuredAtEpochMilli DESC")
    override fun observeAll(): Flow<List<WeightMeasurementEntity>>
}
