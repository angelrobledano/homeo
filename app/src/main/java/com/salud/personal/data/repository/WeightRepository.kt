package com.salud.personal.data.repository

import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.toDomain
import com.salud.personal.data.local.entity.toEntity
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import kotlin.math.abs

interface WeightDaoInterface {
    suspend fun insert(entity: WeightMeasurementEntity): Long
    suspend fun getLatest(): WeightMeasurementEntity?
    suspend fun getAll(): List<WeightMeasurementEntity>
    fun observeLatest(): Flow<WeightMeasurementEntity?>
    fun observeAll(): Flow<List<WeightMeasurementEntity>>
}

class WeightRepository(private val dao: WeightDaoInterface) {

    val latestMeasurement: Flow<WeightMeasurement?> = dao.observeLatest().map { it?.toDomain() }
    val allMeasurements: Flow<List<WeightMeasurement>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun saveMeasurement(measurement: WeightMeasurement): Boolean {
        val latestEntity = dao.getLatest()
        if (latestEntity != null) {
            val latest = latestEntity.toDomain()
            val sameSource = latest.source == measurement.source
            val durationSeconds = abs(Duration.between(latest.measuredAt, measurement.measuredAt).seconds)
            val withinTimeWindow = durationSeconds <= 10
            val weightDifference = abs(latest.weightKg - measurement.weightKg)
            val isDuplicateWeight = weightDifference < 0.1

            if (sameSource && withinTimeWindow && isDuplicateWeight) {
                // Pesada duplicada accidental dentro de la ventana de 10 segundos
                return false
            }
        }

        dao.insert(measurement.toEntity())
        return true
    }

    suspend fun getLatestMeasurement(): WeightMeasurement? {
        return dao.getLatest()?.toDomain()
    }

    suspend fun getAllMeasurements(): List<WeightMeasurement> {
        return dao.getAll().map { it.toDomain() }
    }
}
