package com.salud.personal.domain.gps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsDistanceCalculatorTest {

    @Test
    fun calculateDistanceMeters_madridPoints_calculatesRealisticDistance() {
        // Puerta del Sol -> Plaza Mayor en Madrid (~350-400 metros)
        val sol = GpsLocationPoint(40.4168, -3.7038)
        val plazaMayor = GpsLocationPoint(40.4154, -3.7074)

        val distance = GpsDistanceCalculator.calculateDistanceMeters(sol, plazaMayor)
        assertTrue(distance in 320.0..420.0)
    }

    @Test
    fun calculateTotalDistance_multiPointRoute_sumsLegs() {
        val p1 = GpsLocationPoint(40.0, -3.0)
        val p2 = GpsLocationPoint(40.01, -3.0) // ~1110 metros
        val p3 = GpsLocationPoint(40.02, -3.0) // ~1110 metros

        val total = GpsDistanceCalculator.calculateTotalDistance(listOf(p1, p2, p3))
        assertTrue(total in 2100.0..2300.0)
    }

    @Test
    fun calculatePaceMinutesPerKm_calculatesAccurately() {
        // 5000 metros en 1500 segundos (25 min) = 5.0 min/km
        val pace = GpsDistanceCalculator.calculatePaceMinutesPerKm(5000.0, 1500L)
        assertEquals(5.0, pace, 0.05)
    }
}
