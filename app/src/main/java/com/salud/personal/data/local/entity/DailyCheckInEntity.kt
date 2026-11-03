package com.salud.personal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "daily_checkins",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class DailyCheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "energyRating")
    val energyRating: Int, // 1..5

    @ColumnInfo(name = "sleepQualityRating")
    val sleepQualityRating: Int, // 1..5

    @ColumnInfo(name = "muscleSorenessRating")
    val muscleSorenessRating: Int, // 1..5

    @ColumnInfo(name = "readinessRating")
    val readinessRating: Int, // 1..5

    @ColumnInfo(name = "sleepHours")
    val sleepHours: Double,

    @ColumnInfo(name = "restingHeartRate")
    val restingHeartRate: Int?,

    @ColumnInfo(name = "calculatedRecoveryScore")
    val calculatedRecoveryScore: Int?,

    // Diario de hábitos estilo WHOOP Journal
    @ColumnInfo(name = "lateMeal", defaultValue = "0")
    val lateMeal: Boolean = false,

    @ColumnInfo(name = "lateCaffeine", defaultValue = "0")
    val lateCaffeine: Boolean = false,

    @ColumnInfo(name = "alcohol", defaultValue = "0")
    val alcohol: Boolean = false,

    @ColumnInfo(name = "highStress", defaultValue = "0")
    val highStress: Boolean = false,

    @ColumnInfo(name = "supplements", defaultValue = "0")
    val supplements: Boolean = false,

    @ColumnInfo(name = "screenBeforeBed", defaultValue = "0")
    val screenBeforeBed: Boolean = false
)
