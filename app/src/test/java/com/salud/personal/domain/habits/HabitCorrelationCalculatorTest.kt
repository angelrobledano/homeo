package com.salud.personal.domain.habits

import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HabitCorrelationCalculatorTest {

    @Test
    fun testAlcoholNegativeImpactCorrelation() {
        val date1 = LocalDate.of(2026, 11, 1)
        val date2 = LocalDate.of(2026, 11, 2)
        val date3 = LocalDate.of(2026, 11, 3)
        val date4 = LocalDate.of(2026, 11, 4)

        val checkIns = listOf(
            // 2 days with alcohol
            DailyCheckInEntity(date = date1, energyRating = 2, sleepQualityRating = 2, muscleSorenessRating = 3, readinessRating = 2, sleepHours = 6.0, restingHeartRate = 62, calculatedRecoveryScore = 38, alcohol = true),
            DailyCheckInEntity(date = date2, energyRating = 2, sleepQualityRating = 2, muscleSorenessRating = 3, readinessRating = 2, sleepHours = 6.5, restingHeartRate = 60, calculatedRecoveryScore = 42, alcohol = true),
            // 2 days without alcohol
            DailyCheckInEntity(date = date3, energyRating = 4, sleepQualityRating = 4, muscleSorenessRating = 2, readinessRating = 4, sleepHours = 7.5, restingHeartRate = 54, calculatedRecoveryScore = 80, alcohol = false),
            DailyCheckInEntity(date = date4, energyRating = 4, sleepQualityRating = 4, muscleSorenessRating = 2, readinessRating = 4, sleepHours = 7.8, restingHeartRate = 52, calculatedRecoveryScore = 84, alcohol = false)
        )

        val insights = HabitCorrelationCalculator.computeInsights(checkIns = checkIns, sleepSessions = emptyList())
        val alcoholInsight = insights.find { it.habit == HabitType.ALCOHOL }
        
        assertNotNull(alcoholInsight)
        // Recovery with alcohol: 40% vs without: 82% -> delta: -42%
        assertTrue(alcoholInsight!!.recoveryDelta < -30)
        // RHR with alcohol: 61 vs without: 53 -> delta: +8 bpm
        assertTrue(alcoholInsight.rhrDelta > 5.0)
        assertEquals(HabitImpact.NEGATIVE, alcoholInsight.impact)
    }

    @Test
    fun testSupplementsPositiveImpactCorrelation() {
        val checkIns = listOf(
            DailyCheckInEntity(date = LocalDate.of(2026, 11, 1), energyRating = 4, sleepQualityRating = 4, muscleSorenessRating = 2, readinessRating = 4, sleepHours = 7.5, restingHeartRate = 53, calculatedRecoveryScore = 82, supplements = true),
            DailyCheckInEntity(date = LocalDate.of(2026, 11, 2), energyRating = 3, sleepQualityRating = 3, muscleSorenessRating = 3, readinessRating = 3, sleepHours = 7.0, restingHeartRate = 57, calculatedRecoveryScore = 65, supplements = false)
        )

        val insights = HabitCorrelationCalculator.computeInsights(checkIns = checkIns, sleepSessions = emptyList())
        val supplementInsight = insights.find { it.habit == HabitType.SUPPLEMENTS }

        assertNotNull(supplementInsight)
        assertTrue(supplementInsight!!.recoveryDelta > 10)
        assertEquals(HabitImpact.POSITIVE, supplementInsight.impact)
    }
}
