package com.salud.personal.ui.dashboard

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object DashboardFormatting {

    fun formatWeight(weightKg: Double?): String {
        return if (weightKg == null) {
            "-- kg"
        } else {
            String.format(Locale.US, "%.2f kg", weightKg)
        }
    }

    fun formatDelta(deltaKg: Double?): String {
        if (deltaKg == null) return "-- kg"
        return when {
            deltaKg > 0.0001 -> String.format(Locale.US, "+%.2f kg", deltaKg)
            deltaKg < -0.0001 -> String.format(Locale.US, "%.2f kg", deltaKg)
            else -> "0.00 kg"
        }
    }

    fun formatPercentage(percentage: Float?): String {
        return if (percentage == null) {
            "--%"
        } else {
            String.format(Locale.US, "%.1f%%", percentage)
        }
    }

    fun formatTimestamp(
        instant: Instant?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (instant == null) return "--"
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("es", "ES"))
            .withZone(zoneId)
        return formatter.format(instant)
    }

    fun formatSteps(steps: Int?): String {
        if (steps == null) return "--"
        return String.format(Locale("es", "ES"), "%,d", steps)
    }

    fun formatDistance(distanceMeters: Int?): String {
        if (distanceMeters == null) return "-- km"
        val km = distanceMeters / 1000.0
        return String.format(Locale.US, "%.2f km", km)
    }

    fun formatCalories(caloriesBurned: Int?): String {
        if (caloriesBurned == null) return "-- kcal"
        return "$caloriesBurned kcal"
    }

    fun formatBpm(bpm: Int?): String {
        if (bpm == null) return "-- bpm"
        return "$bpm bpm"
    }

    fun formatTime(
        instant: Instant?,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (instant == null) return "--:--"
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES"))
            .withZone(zoneId)
        return formatter.format(instant)
    }
}
