package com.salud.personal.domain.strain

import com.salud.personal.data.local.entity.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualWorkoutEstimatorTest {

    @Test
    fun estimateTrimp_crossFitSession60MinRpe8_calculatesHighTrimp() {
        // 60 min * 8 RPE = 480 sRPE * 0.22 = 105.6 TRIMP
        val trimp = ManualWorkoutEstimator.estimateTrimp(
            type = WorkoutType.CROSSFIT_WOD,
            durationMinutes = 60,
            rpe = 8
        )
        assertEquals(105.6, trimp, 0.5)
    }

    @Test
    fun estimateTrimp_strengthSession45MinRpe7_calculatesModerateTrimp() {
        // 45 min * 7 RPE = 315 sRPE * 0.17 = 53.6 TRIMP
        val trimp = ManualWorkoutEstimator.estimateTrimp(
            type = WorkoutType.STRENGTH,
            durationMinutes = 45,
            rpe = 7
        )
        assertEquals(53.6, trimp, 0.5)
    }

    @Test
    fun estimateTrimp_lightWalk30MinRpe3_calculatesLowTrimp() {
        // 30 min * 3 RPE = 90 sRPE * 0.10 = 9.0 TRIMP
        val trimp = ManualWorkoutEstimator.estimateTrimp(
            type = WorkoutType.WALK,
            durationMinutes = 30,
            rpe = 3
        )
        assertEquals(9.0, trimp, 0.5)
    }

    @Test
    fun estimateCalories_calculatesReasonableBurn() {
        val calories = ManualWorkoutEstimator.estimateCalories(
            type = WorkoutType.CROSSFIT_WOD,
            durationMinutes = 60,
            rpe = 8,
            bodyWeightKg = 75.0
        )
        // 60 min CrossFit RPE 8 should be ~600-800 kcal for 75kg athlete
        assertTrue("Calories burned should be between 600 and 800 kcal, was $calories", calories in 600..800)
    }
}
