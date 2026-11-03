package com.salud.personal.data.local.converter

import androidx.room.TypeConverter
import com.salud.personal.domain.model.ProvenanceSource
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun fromProvenanceSource(source: ProvenanceSource?): String? {
        return source?.name
    }

    @TypeConverter
    fun toProvenanceSource(value: String?): ProvenanceSource? {
        return value?.let { name ->
            ProvenanceSource.entries.firstOrNull { it.name == name || it.identifier == name }
        }
    }

    @TypeConverter
    fun fromLocalDate(date: java.time.LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): java.time.LocalDate? {
        return value?.let { java.time.LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromWorkoutType(type: com.salud.personal.data.local.entity.WorkoutType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toWorkoutType(value: String?): com.salud.personal.data.local.entity.WorkoutType? {
        return value?.let { com.salud.personal.data.local.entity.WorkoutType.valueOf(it) }
    }
}
