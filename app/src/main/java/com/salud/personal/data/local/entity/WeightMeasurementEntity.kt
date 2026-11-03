package com.salud.personal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import java.time.Instant

@Entity(tableName = "weight_measurements")
data class WeightMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourceDevice: String,
    val externalId: String? = null,
    val weightKg: Double,
    val bodyFatPercentage: Float? = null,
    val muscleMassKg: Double? = null,
    val impedanceOhms: Double? = null,
    val measuredAtEpochMilli: Long,
    val importedAtEpochMilli: Long
)

fun WeightMeasurementEntity.toDomain(): WeightMeasurement {
    val parsedSource = ProvenanceSource.entries.firstOrNull {
        it.name == sourceDevice || it.identifier == sourceDevice
    } ?: ProvenanceSource.MANUAL_INPUT

    return WeightMeasurement(
        id = id,
        source = parsedSource,
        externalId = externalId,
        weightKg = weightKg,
        bodyFatPercentage = bodyFatPercentage,
        muscleMassKg = muscleMassKg,
        impedanceOhms = impedanceOhms,
        measuredAt = Instant.ofEpochMilli(measuredAtEpochMilli),
        importedAt = Instant.ofEpochMilli(importedAtEpochMilli)
    )
}

fun WeightMeasurement.toEntity(): WeightMeasurementEntity {
    return WeightMeasurementEntity(
        id = id,
        sourceDevice = source.name,
        externalId = externalId,
        weightKg = weightKg,
        bodyFatPercentage = bodyFatPercentage,
        muscleMassKg = muscleMassKg,
        impedanceOhms = impedanceOhms,
        measuredAtEpochMilli = measuredAt.toEpochMilli(),
        importedAtEpochMilli = importedAt.toEpochMilli()
    )
}
