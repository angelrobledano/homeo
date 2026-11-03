package com.salud.personal.domain.vitals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsBaselineCalculatorTest {

    @Test
    fun testAllVitalsInBaseline() {
        val rhrHistory = listOf(54, 55, 53, 56, 54, 55, 54)
        val sleepHistory = listOf(7.5, 7.8, 7.2, 8.0, 7.6, 7.4)
        val restorativeHistory = listOf(42, 45, 40, 44, 46)
        val weightHistory = listOf(78.5, 78.4, 78.6, 78.5)

        val result = VitalsBaselineCalculator.evaluate(
            todayRhr = 55,
            rhrHistory = rhrHistory,
            todaySleepHours = 7.6,
            sleepHistory = sleepHistory,
            todayRestorativePct = 43,
            restorativeHistory = restorativeHistory,
            todayWeightKg = 78.5,
            weightHistory = weightHistory
        )

        assertEquals(4, result.metricsInBaseline)
        assertEquals(4, result.totalMetricsEvaluated)
        assertTrue(result.allInBaseline)
        assertTrue(result.alerts.isEmpty())
    }

    @Test
    fun testElevatedRhrAlert() {
        // Average RHR is ~54, baseline is 51..57. Today is 63 bpm (+9 bpm).
        val rhrHistory = listOf(54, 55, 53, 56, 54, 55, 54)

        val result = VitalsBaselineCalculator.evaluate(
            todayRhr = 63,
            rhrHistory = rhrHistory,
            todaySleepHours = 7.5,
            sleepHistory = listOf(7.5, 7.5),
            todayRestorativePct = 40,
            restorativeHistory = listOf(40, 42),
            todayWeightKg = 80.0,
            weightHistory = listOf(80.0, 80.0)
        )

        assertEquals(3, result.metricsInBaseline)
        assertFalse(result.allInBaseline)
        assertEquals(1, result.alerts.size)
        assertTrue(result.alerts.first().contains("FC en reposo"))
    }
}
