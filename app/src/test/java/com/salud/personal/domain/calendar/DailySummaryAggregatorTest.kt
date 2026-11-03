package com.salud.personal.domain.calendar

import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DailySummaryAggregatorTest {

    @Test
    fun testAggregateCompleteDay() {
        val date = LocalDate.of(2026, 11, 1)

        val activity = DailyActivityEntity(
            date = date.toString(),
            steps = 8450,
            distanceMeters = 6200,
            caloriesBurned = 320,
            sourceDevice = "Amazfit GTR 3"
        )

        val now = Instant.now()
        val workouts = listOf(
            WorkoutSessionEntity(
                type = WorkoutType.CROSSFIT_WOD,
                name = "Murph",
                startTime = now.minusSeconds(3600),
                endTime = now,
                durationMinutes = 45.0,
                avgHeartRate = 152,
                maxHeartRate = 178,
                trimp = 42.5,
                caloriesBurned = 380,
                rpe = 8,
                notes = null
            )
        )

        val sleep = SleepSessionEntity(
            date = date,
            startTime = now.minusSeconds(8 * 3600),
            endTime = now,
            deepSleepMinutes = 110,
            lightSleepMinutes = 230,
            remSleepMinutes = 90,
            awakeMinutes = 20,
            totalSleepMinutes = 430,
            sleepEfficiencyScore = 90,
            sourceDevice = "Amazfit GTR 3"
        )

        val checkIn = DailyCheckInEntity(
            date = date,
            energyRating = 4,
            sleepQualityRating = 5,
            muscleSorenessRating = 2,
            readinessRating = 4,
            sleepHours = 7.2,
            restingHeartRate = 51,
            calculatedRecoveryScore = 86
        )

        val summary = DailySummaryAggregator.aggregate(
            date = date,
            activity = activity,
            workouts = workouts,
            sleep = sleep,
            checkIn = checkIn,
            weightKg = 74.8,
            bodyFatPct = 15.2
        )

        assertEquals(date, summary.date)
        assertEquals(8450, summary.steps)
        assertEquals(6200, summary.distanceMeters)
        assertEquals(320, summary.caloriesBurned)
        assertEquals(1, summary.workouts.size)
        assertEquals(42.5, summary.totalWorkoutTrimp, 0.01)
        assertEquals(45.0, summary.totalWorkoutMinutes, 0.01)
        assertNotNull(summary.sleepSession)
        assertEquals(430, summary.sleepSession?.totalSleepMinutes)
        assertEquals(86, summary.checkIn?.calculatedRecoveryScore)
        assertEquals(74.8, summary.weightKg!!, 0.01)
        assertEquals(15.2, summary.bodyFatPct!!, 0.01)
        assertTrue(summary.hasData)
    }

    @Test
    fun testEmptyDayHasDataFalse() {
        val date = LocalDate.of(2026, 11, 2)
        val summary = DailySummaryAggregator.aggregate(
            date = date,
            activity = null,
            workouts = emptyList(),
            sleep = null,
            checkIn = null,
            weightKg = null,
            bodyFatPct = null
        )

        assertEquals(0, summary.steps)
        assertEquals(0, summary.workouts.size)
        assertEquals(false, summary.hasData)
    }
}
