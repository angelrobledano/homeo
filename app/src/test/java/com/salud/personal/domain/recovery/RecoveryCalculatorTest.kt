package com.salud.personal.domain.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCalculatorTest {

    @Test
    fun calculateRecovery_optimalConditions_returnsHighRecovery() {
        val res = RecoveryCalculator.calculateRecovery(
            currentRhr = 48,
            baselineRhr = 50,
            sleepHours = 8.0,
            sleepGoalHours = 8.0,
            subjectiveRating = 5
        )
        assertEquals(100, res.score)
        assertEquals(100, res.rhrFactorScore)
        assertEquals(100, res.sleepFactorScore)
        assertEquals(100, res.subjectiveFactorScore)
    }

    @Test
    fun calculateRecovery_poorSleep_lowersScoreAndIdentifiesDriver() {
        val res = RecoveryCalculator.calculateRecovery(
            currentRhr = 50,
            baselineRhr = 50,
            sleepHours = 4.0,
            sleepGoalHours = 8.0,
            subjectiveRating = 4
        )
        // RHR: 100 * 0.35 = 35
        // Sleep: 50 * 0.35 = 17.5
        // Subj: 80 * 0.30 = 24
        // Total = 76.5 -> 76
        assertTrue(res.score in 75..78)
        assertTrue(res.primaryDriver.contains("Sueño insuficiente"))
    }

    @Test
    fun calculateRecovery_elevatedRhr_lowersScoreAndIdentifiesDriver() {
        val res = RecoveryCalculator.calculateRecovery(
            currentRhr = 62, // +12 sobre base -> score 100 - 60 = 40
            baselineRhr = 50,
            sleepHours = 8.0,
            sleepGoalHours = 8.0,
            subjectiveRating = 4
        )
        assertTrue(res.score < 80)
        assertTrue(res.primaryDriver.contains("FC en reposo elevada"))
    }
}
