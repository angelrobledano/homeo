package com.salud.personal.domain.gps

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsLocationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

object GpsDistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Fórmula de Haversine para calcular la distancia ortodrómica en metros
     * entre dos coordenadas GPS.
     */
    fun calculateDistanceMeters(p1: GpsLocationPoint, p2: GpsLocationPoint): Double {
        val latDistance = Math.toRadians(p2.latitude - p1.latitude)
        val lonDistance = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calcula la distancia total acumulada de un track GPS.
     */
    fun calculateTotalDistance(points: List<GpsLocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += calculateDistanceMeters(points[i], points[i + 1])
        }
        return total
    }

    /**
     * Calcula el ritmo medio en min/km.
     */
    fun calculatePaceMinutesPerKm(distanceMeters: Double, durationSeconds: Long): Double {
        if (distanceMeters <= 50.0 || durationSeconds <= 0) return 0.0
        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationSeconds / 60.0
        return durationMinutes / distanceKm
    }
}
