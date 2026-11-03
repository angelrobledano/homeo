package com.salud.personal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salud.personal.domain.model.HeartRateSample
import java.time.Instant

@Entity(
    tableName = "heart_rate_samples",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["sourceDevice", "timestamp"], unique = true)
    ]
)
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val bpm: Int,
    val sourceDevice: String
)

fun HeartRateSampleEntity.toDomain(): HeartRateSample {
    return HeartRateSample(
        timestamp = Instant.ofEpochMilli(timestamp),
        bpm = bpm,
        sourceDevice = sourceDevice
    )
}

fun HeartRateSample.toEntity(): HeartRateSampleEntity {
    return HeartRateSampleEntity(
        timestamp = timestamp.toEpochMilli(),
        bpm = bpm,
        sourceDevice = sourceDevice
    )
}
