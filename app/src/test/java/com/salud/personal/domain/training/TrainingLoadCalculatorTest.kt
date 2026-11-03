package com.salud.personal.domain.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingLoadCalculatorTest {

    @Test
    fun calculateZones_with190MaxHr_calculatesStandardRanges() {
        val zones = TrainingLoadCalculator.calculateZones(190)
        assertEquals(95..114, zones.z1Recovery)
        assertEquals(114..133, zones.z2Aerobic)
        assertEquals(133..152, zones.z3Tempo)
        assertEquals(152..171, zones.z4Threshold)
        assertEquals(171..190, zones.z5Anaerobic)
    }

    @Test
    fun calculateBanisterTrimp_validInputs_returnsExpectedValue() {
        // Sesión de 60 min, avg 150 bpm, reposo 50 bpm, max 190 bpm (Hombre)
        // DeltaHR = (150-50)/(190-50) = 100/140 = 0.714
        // Y = 0.64 * exp(1.92 * 0.714) = 0.64 * exp(1.37) ≈ 0.64 * 3.935 = 2.518
        // TRIMP = 60 * 0.714 * 2.518 ≈ 107.8
        val trimp = TrainingLoadCalculator.calculateBanisterTrimp(
            durationMinutes = 60.0,
            avgHr = 150.0,
            restHr = 50.0,
            maxHr = 190.0,
            gender = TrainingLoadCalculator.Gender.MALE
        )
        assertTrue(trimp in 100.0..115.0)
    }

    @Test
    fun calculateBanisterTrimp_zeroDurationOrInvalidHr_returnsZero() {
        assertEquals(0.0, TrainingLoadCalculator.calculateBanisterTrimp(0.0, 150.0, 50.0, 190.0), 0.01)
        assertEquals(0.0, TrainingLoadCalculator.calculateBanisterTrimp(60.0, 50.0, 190.0, 50.0), 0.01)
    }

    @Test
    fun evaluateWeeklyLoad_thresholds_returnAccurateLevels() {
        assertEquals(TrainingLoadCalculator.LoadLevel.RECOVERY, TrainingLoadCalculator.evaluateWeeklyLoad(120.0))
        assertEquals(TrainingLoadCalculator.LoadLevel.OPTIMAL, TrainingLoadCalculator.evaluateWeeklyLoad(300.0))
        assertEquals(TrainingLoadCalculator.LoadLevel.HIGH, TrainingLoadCalculator.evaluateWeeklyLoad(550.0))
        assertEquals(TrainingLoadCalculator.LoadLevel.EXTREME, TrainingLoadCalculator.evaluateWeeklyLoad(700.0))
    }
}
