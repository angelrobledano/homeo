package com.salud.personal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salud.personal.domain.model.DailyActivitySummary
import java.time.LocalDate

@Entity(
    tableName = "daily_activity",
    indices = [
        Index(value = ["sourceDevice", "date"], unique = true)
    ]
)
data class DailyActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    val steps: Int,
    val distanceMeters: Int,
    val caloriesBurned: Int,
    val sourceDevice: String
)

fun DailyActivityEntity.toDomain(): DailyActivitySummary {
    return DailyActivitySummary(
        date = LocalDate.parse(date),
        steps = steps,
        distanceMeters = distanceMeters,
        caloriesBurned = caloriesBurned,
        sourceDevice = sourceDevice
    )
}

fun DailyActivitySummary.toEntity(): DailyActivityEntity {
    return DailyActivityEntity(
        date = date.toString(),
        steps = steps,
        distanceMeters = distanceMeters,
        caloriesBurned = caloriesBurned,
        sourceDevice = sourceDevice
    )
}
