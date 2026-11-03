package com.salud.personal.domain.summary

import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PeriodSummaryCalculatorTest {

    private val zoneId = ZoneId.systemDefault()

    private fun createWorkout(date: LocalDate, trimp: Double, durationMin: Double): WorkoutSessionEntity {
        val start = date.atTime(10, 0).atZone(zoneId).toInstant()
        val end = start.plusSeconds((durationMin * 60).toLong())
        return WorkoutSessionEntity(
            type = WorkoutType.CROSSFIT_WOD,
            name = "Test WOD",
            startTime = start,
            endTime = end,
            durationMinutes = durationMin,
            avgHeartRate = 150,
            maxHeartRate = 175,
            trimp = trimp,
            caloriesBurned = 500,
            rpe = 8,
            notes = null
        )
    }

    private fun createActivity(date: LocalDate, steps: Int): DailyActivityEntity {
        return DailyActivityEntity(
            date = date.toString(),
            steps = steps,
            distanceMeters = (steps * 0.75).toInt(),
            caloriesBurned = 2000,
            sourceDevice = "GTR 3"
        )
    }

    private fun createCheckIn(date: LocalDate, recovery: Int): DailyCheckInEntity {
        return DailyCheckInEntity(
            date = date,
            energyRating = 4,
            sleepQualityRating = 4,
            muscleSorenessRating = 2,
            readinessRating = 4,
            sleepHours = 8.0,
            restingHeartRate = 50,
            calculatedRecoveryScore = recovery
        )
    }

    @Test
    fun computeSummary_forWeek_correctlyAggregatesMetricsAndIdentifiesPeaks() {
        // Reference date: Wednesday Nov 4, 2026.
        // Monday is Nov 2, Sunday is Nov 8.
        val refDate = LocalDate.of(2026, 11, 4)

        val workouts = listOf(
            createWorkout(LocalDate.of(2026, 11, 2), trimp = 120.0, durationMin = 60.0), // Monday
            createWorkout(LocalDate.of(2026, 11, 4), trimp = 150.0, durationMin = 75.0), // Wednesday (Peak)
            createWorkout(LocalDate.of(2026, 11, 15), trimp = 200.0, durationMin = 90.0) // Next week (should be excluded)
        )

        val activities = listOf(
            createActivity(LocalDate.of(2026, 11, 2), steps = 10000),
            createActivity(LocalDate.of(2026, 11, 3), steps = 8000),
            createActivity(LocalDate.of(2026, 11, 4), steps = 12000)
        )

        val checkIns = listOf(
            createCheckIn(LocalDate.of(2026, 11, 2), recovery = 75),
            createCheckIn(LocalDate.of(2026, 11, 3), recovery = 88) // Peak recovery
        )

        val summary = PeriodSummaryCalculator.computeSummary(
            periodType = PeriodType.WEEK,
            referenceDate = refDate,
            workouts = workouts,
            activities = activities,
            sleeps = emptyList(),
            checkIns = checkIns,
            weights = emptyList()
        )

        assertEquals(LocalDate.of(2026, 11, 2), summary.startDate)
        assertEquals(LocalDate.of(2026, 11, 8), summary.endDate)
        assertEquals(2, summary.totalWorkouts)
        assertEquals(270.0, summary.totalTrimp, 0.01)
        assertEquals(135.0, summary.totalDurationMinutes, 0.01)
        assertEquals(30000, summary.totalSteps)
        assertEquals(82, summary.avgRecoveryScore) // (75 + 88) / 2.0 = 81.5 -> 82
        assertEquals(LocalDate.of(2026, 11, 4), summary.peakStrainDay)
        assertEquals(150.0, summary.peakStrainValue ?: 0.0, 0.01)
        assertEquals(LocalDate.of(2026, 11, 3), summary.peakRecoveryDay)
        assertEquals(88, summary.peakRecoveryValue)
    }

    @Test
    fun computeSummary_forMonth_handlesMonthBounds() {
        val refDate = LocalDate.of(2026, 11, 15) // November
        val summary = PeriodSummaryCalculator.computeSummary(
            periodType = PeriodType.MONTH,
            referenceDate = refDate,
            workouts = emptyList(),
            activities = emptyList(),
            sleeps = emptyList(),
            checkIns = emptyList(),
            weights = emptyList()
        )

        assertEquals(LocalDate.of(2026, 11, 1), summary.startDate)
        assertEquals(LocalDate.of(2026, 11, 30), summary.endDate)
    }

    @Test
    fun computeSummary_forYear_handlesFullYear() {
        val refDate = LocalDate.of(2026, 7, 20)
        val summary = PeriodSummaryCalculator.computeSummary(
            periodType = PeriodType.YEAR,
            referenceDate = refDate,
            workouts = emptyList(),
            activities = emptyList(),
            sleeps = emptyList(),
            checkIns = emptyList(),
            weights = emptyList()
        )

        assertEquals(LocalDate.of(2026, 1, 1), summary.startDate)
        assertEquals(LocalDate.of(2026, 12, 31), summary.endDate)
    }
}
