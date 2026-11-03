package com.salud.personal.domain.trends

import org.junit.Assert.assertEquals
import org.junit.Test

class TrendsCalculatorTest {

    @Test
    fun calculateAcwr_zeroLoad_returnsInsufficientData() {
        val res = TrendsCalculator.calculateAcwr(0.0, 0.0)
        assertEquals(0.0, res.ratio, 0.01)
        assertEquals(TrendsCalculator.AcwrRiskCategory.INSUFFICIENT_DATA, res.riskCategory)
    }

    @Test
    fun calculateAcwr_insufficientHistoryDays_returnsInsufficientData() {
        // Con solo 7 días de datos, no se debe calcular un ratio crónico de 28 días falso
        val res = TrendsCalculator.calculateAcwr(acuteLoadSum = 250.0, chronicLoadSum = 250.0, historyDaysAvailable = 7)
        assertEquals(0.0, res.ratio, 0.01)
        assertEquals(TrendsCalculator.AcwrRiskCategory.INSUFFICIENT_DATA, res.riskCategory)
    }

    @Test
    fun calculateAcwr_sweetspot_returnsOptimal() {
        // Aguda 300 TRIMP en 7d, crónica 1200 TRIMP en 28d (promedio 300/sem)
        // Ratio = 300 / 300 = 1.0 -> SWEETSPOT
        val res = TrendsCalculator.calculateAcwr(300.0, 1200.0)
        assertEquals(1.0, res.ratio, 0.01)
        assertEquals(TrendsCalculator.AcwrRiskCategory.OPTIMAL_SWEETSPOT, res.riskCategory)
    }

    @Test
    fun calculateAcwr_spikeInLoad_returnsVeryHighRisk() {
        // Aguda 600 TRIMP en 7d, crónica 1200 TRIMP en 28d (promedio 300/sem)
        // Ratio = 600 / 300 = 2.0 -> VERY_HIGH_RISK
        val res = TrendsCalculator.calculateAcwr(600.0, 1200.0)
        assertEquals(2.0, res.ratio, 0.01)
        assertEquals(TrendsCalculator.AcwrRiskCategory.VERY_HIGH_RISK, res.riskCategory)
    }

    @Test
    fun calculateMovingAverage_simpleSeries_calculatesRollingMean() {
        val weights = listOf(75.0, 75.5, 76.0, 75.5)
        val ma = TrendsCalculator.calculateMovingAverage(weights, windowSize = 2)
        // i=0: 75.0
        // i=1: (75.0+75.5)/2 = 75.25
        // i=2: (75.5+76.0)/2 = 75.75
        // i=3: (76.0+75.5)/2 = 75.75
        assertEquals(75.0, ma[0], 0.01)
        assertEquals(75.25, ma[1], 0.01)
        assertEquals(75.75, ma[2], 0.01)
    }

    @Test
    fun calculateExponentialTrendWeight_emptyList_returnsZeroResult() {
        val result = TrendsCalculator.calculateExponentialTrendWeight(emptyList())
        assertEquals(0.0, result.currentTrendKg, 0.01)
        assertEquals(0.0, result.currentRawKg, 0.01)
        assertEquals(0.0, result.waterNoiseKg, 0.01)
        org.junit.Assert.assertNull(result.weeklyRateKg)
        assertEquals(0, result.points.size)
    }

    @Test
    fun calculateExponentialTrendWeight_fluctuatingWeights_smoothsWaterNoise() {
        // Simulación: Peso base 75.0, un día come pizza y retiene 1.5kg de agua (76.5), luego baja a 74.8
        val dates = listOf(
            java.time.LocalDate.of(2026, 11, 1),
            java.time.LocalDate.of(2026, 11, 2),
            java.time.LocalDate.of(2026, 11, 3)
        )
        val rawWeights = listOf(
            TrendsCalculator.WeightPoint(dates[0], 75.0),
            TrendsCalculator.WeightPoint(dates[1], 76.5), // +1.5kg pico de agua
            TrendsCalculator.WeightPoint(dates[2], 74.8)
        )

        val result = TrendsCalculator.calculateExponentialTrendWeight(rawWeights, alpha = 0.10)
        // Día 1: Trend = 75.0, water noise = 0.0
        // Día 2: Trend = 75.0 + 0.1*(76.5 - 75.0) = 75.15, water noise = 76.5 - 75.15 = +1.35
        // Día 3: Trend = 75.15 + 0.1*(74.8 - 75.15) = 75.115, water noise = 74.8 - 75.115 = -0.315
        assertEquals(75.12, result.currentTrendKg, 0.02)
        assertEquals(74.8, result.currentRawKg, 0.01)
        assertEquals(-0.32, result.waterNoiseKg, 0.05)
        assertEquals(3, result.points.size)
        assertEquals(1.35, result.points[1].waterNoiseKg, 0.05)
    }

    @Test
    fun calculateRhrTrend_evaluatesBaselineAndStatus() {
        val today = java.time.LocalDate.of(2026, 11, 3)
        // 5 días de RHR promedio 55 bpm
        val history = (0..4).map { i ->
            TrendsCalculator.DailyRhrPoint(today.minusDays(4L - i), 55)
        }.toMutableList()

        // Último día elevado a 61 bpm (+6 bpm alerta fatiga)
        history.add(TrendsCalculator.DailyRhrPoint(today, 61))

        val result = TrendsCalculator.calculateRhrTrend(history)
        assertEquals(61, result.latestRhr)
        assertEquals(56.0, result.baselineRhr, 0.1) // Media = (55*5 + 61) / 6 = 56.0
        assertEquals(5.0, result.deltaFromBaseline, 0.1) // 61 - 56.0 = +5.0
        assertEquals(TrendsCalculator.RhrStatus.ELEVATED, result.status)
    }

    @Test
    fun calculateWeeklyComparison_calculatesDeltasSafely() {
        val comp = TrendsCalculator.calculateWeeklyComparison(
            currentWeekTrimp = 350.0,
            previousWeekTrimp = 300.0,
            currentSteps = listOf(8000, 10000, 9000), // media = 9000
            previousSteps = listOf(7000, 7500, 8000), // media = 7500
            currentSleepMinutes = listOf(480, 450), // media = 465
            previousSleepMinutes = listOf(420, 420), // media = 420
            currentRhrs = listOf(56, 54), // media = 55.0
            previousRhrs = listOf(58, 56)  // media = 57.0
        )

        // TRIMP: 350 vs 300 = +16%
        assertEquals(16, comp.trimpDeltaPct)
        // Pasos: 9000 vs 7500 = +20%
        assertEquals(9000, comp.currentAvgSteps)
        assertEquals(7500, comp.previousAvgSteps)
        assertEquals(20, comp.stepsDeltaPct)
        // Sueño: 465 vs 420 = +45 min
        assertEquals(45, comp.sleepDeltaMinutes)
        // RHR: 55.0 vs 57.0 = -2.0 bpm
        assertEquals(55.0, comp.currentAvgRhr!!, 0.01)
        assertEquals(-2.0, comp.rhrDeltaBpm!!, 0.01)
    }
}
