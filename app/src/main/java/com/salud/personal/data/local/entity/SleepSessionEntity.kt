package com.salud.personal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "sleep_sessions",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "startTime")
    val startTime: Instant,

    @ColumnInfo(name = "endTime")
    val endTime: Instant,

    @ColumnInfo(name = "deepSleepMinutes")
    val deepSleepMinutes: Int,

    @ColumnInfo(name = "lightSleepMinutes")
    val lightSleepMinutes: Int,

    @ColumnInfo(name = "remSleepMinutes")
    val remSleepMinutes: Int,

    @ColumnInfo(name = "awakeMinutes")
    val awakeMinutes: Int,

    @ColumnInfo(name = "totalSleepMinutes")
    val totalSleepMinutes: Int,

    @ColumnInfo(name = "sleepEfficiencyScore")
    val sleepEfficiencyScore: Int?, // 0..100

    @ColumnInfo(name = "sourceDevice")
    val sourceDevice: String = "Amazfit GTR 3"
)
