package com.salud.personal.domain.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepAnalysisCalculatorTest {

    @Test
    fun testRestorativeSleepCalculation() {
        // 105 min deep + 95 min rem = 200 min / 444 total = 45%
        val result = SleepAnalysisCalculator.calculateRestorativeSleep(
            deepMinutes = 105,
            remMinutes = 95,
            totalMinutes = 444
        )

        assertEquals(200, result.restorativeMinutes)
        assertEquals(45, result.percentage)
        assertEquals(SleepQualityTier.EXCELLENT, result.tier)
    }

    @Test
    fun testRestorativeSleepInsufficient() {
        val result = SleepAnalysisCalculator.calculateRestorativeSleep(
            deepMinutes = 40,
            remMinutes = 50,
            totalMinutes = 400
        )

        assertEquals(90, result.restorativeMinutes)
        assertEquals(23, result.percentage)
        assertEquals(SleepQualityTier.INSUFFICIENT, result.tier)
    }

    @Test
    fun testSleepNeedWithTrainingLoad() {
        // Base 7.75h + high recent TRIMP (>150) -> +0.5h = 8.25h
        val highLoadNeed = SleepAnalysisCalculator.calculateSleepNeed(
            baseHours = 7.75,
            recentTrimp = 180.0
        )
        assertEquals(8.25, highLoadNeed, 0.01)

        // Moderate TRIMP (80-150) -> +0.25h = 8.0h
        val modLoadNeed = SleepAnalysisCalculator.calculateSleepNeed(
            baseHours = 7.75,
            recentTrimp = 110.0
        )
        assertEquals(8.0, modLoadNeed, 0.01)

        // Rest day -> 7.75h
        val restNeed = SleepAnalysisCalculator.calculateSleepNeed(
            baseHours = 7.75,
            recentTrimp = 20.0
        )
        assertEquals(7.75, restNeed, 0.01)
    }

    @Test
    fun testSleepDebt() {
        val debt = SleepAnalysisCalculator.calculateSleepDebt(
            sleepNeedHours = 8.0,
            actualSleepHours = 6.5
        )
        assertEquals(1.5, debt, 0.01)

        val surplus = SleepAnalysisCalculator.calculateSleepDebt(
            sleepNeedHours = 7.5,
            actualSleepHours = 8.0
        )
        assertEquals(0.0, surplus, 0.01)
    }

    @Test
    fun testStageAssessment() {
        // Deep: 105 min / 444 total = 23.6% -> within 15-25%
        val deepQuality = SleepAnalysisCalculator.assessStage(
            stage = SleepStage.DEEP,
            minutes = 105,
            totalMinutes = 444
        )
        assertEquals(StageBalance.OPTIMAL, deepQuality)

        // Deep too low: 30 min / 444 total = 6.7%
        val lowDeep = SleepAnalysisCalculator.assessStage(
            stage = SleepStage.DEEP,
            minutes = 30,
            totalMinutes = 444
        )
        assertEquals(StageBalance.LOW, lowDeep)
    }
}
