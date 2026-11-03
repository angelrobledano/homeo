package com.salud.personal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class WorkoutType {
    CROSSFIT_WOD,
    ENDURANCE_CARDIO,
    WALK,
    STRENGTH,
    RECOVERY
}

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["type"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "type")
    val type: WorkoutType,

    @ColumnInfo(name = "name")
    val name: String, // ej. "Murph", "Caminata 5km", "Sentadillas pesadas"

    @ColumnInfo(name = "startTime")
    val startTime: Instant,

    @ColumnInfo(name = "endTime")
    val endTime: Instant,

    @ColumnInfo(name = "durationMinutes")
    val durationMinutes: Double,

    @ColumnInfo(name = "avgHeartRate")
    val avgHeartRate: Int?,

    @ColumnInfo(name = "maxHeartRate")
    val maxHeartRate: Int?,

    @ColumnInfo(name = "trimp")
    val trimp: Double?,

    @ColumnInfo(name = "caloriesBurned")
    val caloriesBurned: Int?,

    @ColumnInfo(name = "rpe")
    val rpe: Int, // 1 a 10 (esfuerzo percibido)

    @ColumnInfo(name = "notes")
    val notes: String?
)
