package com.salud.personal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Registro de pasos por hora del día.
 * Permite renderizar histogramas horarios (mañana, tarde, noche) sin saturar memoria.
 */
@Entity(
    tableName = "hourly_steps",
    indices = [
        Index(value = ["source_device", "date", "hour"], unique = true),
        Index(value = ["date"])
    ]
)
data class HourlyStepsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "hour")
    val hour: Int, // 0..23

    @ColumnInfo(name = "steps")
    val steps: Int,

    @ColumnInfo(name = "source_device")
    val sourceDevice: String
)
