package com.salud.personal.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Test

class StepGoalCalculatorTest {

    @Test
    fun calculateRemainingSteps_withPartialSteps_returnsDifference() {
        val remaining = StepGoalCalculator.calculateRemainingSteps(currentSteps = 6500, goal = 10000)
        assertEquals(3500, remaining)
    }

    @Test
    fun calculateRemainingSteps_whenGoalReachedOrExceeded_returnsZero() {
        assertEquals(0, StepGoalCalculator.calculateRemainingSteps(10000, 10000))
        assertEquals(0, StepGoalCalculator.calculateRemainingSteps(12500, 10000))
    }

    @Test
    fun calculateRemainingSteps_withNegativeInput_treatsAsZero() {
        assertEquals(10000, StepGoalCalculator.calculateRemainingSteps(-50, 10000))
    }

    @Test
    fun calculateProgressRatio_returnsAccurateRatio() {
        val ratio = StepGoalCalculator.calculateProgressRatio(5000, 10000)
        assertEquals(0.5f, ratio, 0.001f)

        val ratioOver = StepGoalCalculator.calculateProgressRatio(15000, 10000)
        assertEquals(1.5f, ratioOver, 0.001f)
    }

    @Test
    fun estimateMinutesRemaining_calculatesCorrectMinutesCeiled() {
        // 3500 pasos / 100 pasos/min = 35 minutos
        assertEquals(35, StepGoalCalculator.estimateMinutesRemaining(3500, 100))
        // 3501 pasos / 100 pasos/min = 36 minutos (redondeo hacia arriba)
        assertEquals(36, StepGoalCalculator.estimateMinutesRemaining(3501, 100))
        // 0 pasos restantes = 0 minutos
        assertEquals(0, StepGoalCalculator.estimateMinutesRemaining(0, 100))
    }
}
