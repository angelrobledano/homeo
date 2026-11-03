package com.salud.personal.domain.strain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrainTargetCalculatorTest {

    @Test
    fun testHighRecoveryStrainTarget() {
        // Recovery 85% (Green) -> Target between 12.0 and 16.0
        val target = StrainTargetCalculator.calculateTarget(recoveryScore = 85)
        assertEquals(StrainCapacityTier.HIGH, target.tier)
        assertTrue(target.minStrain in 12.0..13.0)
        assertTrue(target.maxStrain in 15.0..17.0)
    }

    @Test
    fun testModerateRecoveryStrainTarget() {
        // Recovery 55% (Yellow) -> Target between 8.0 and 12.0
        val target = StrainTargetCalculator.calculateTarget(recoveryScore = 55)
        assertEquals(StrainCapacityTier.MODERATE, target.tier)
        assertTrue(target.minStrain in 8.0..10.0)
        assertTrue(target.maxStrain in 11.0..13.0)
    }

    @Test
    fun testLowRecoveryStrainTarget() {
        // Recovery 25% (Red) -> Target for active rest (< 8.0)
        val target = StrainTargetCalculator.calculateTarget(recoveryScore = 25)
        assertEquals(StrainCapacityTier.LOW, target.tier)
        assertTrue(target.minStrain in 4.0..5.0)
        assertTrue(target.maxStrain in 7.0..8.0)
    }

    @Test
    fun testStrainStatusAssessment() {
        val target = StrainTargetRange(minStrain = 10.0, maxStrain = 14.0, tier = StrainCapacityTier.MODERATE)
        
        val under = StrainTargetCalculator.assessStatus(currentStrain = 6.5, target = target)
        assertEquals(StrainStatus.BELOW_TARGET, under)

        val optimal = StrainTargetCalculator.assessStatus(currentStrain = 12.0, target = target)
        assertEquals(StrainStatus.OPTIMAL, optimal)

        val over = StrainTargetCalculator.assessStatus(currentStrain = 15.5, target = target)
        assertEquals(StrainStatus.OVER_STRAIN, over)
    }

    @Test
    fun testDailyStrainAccumulation() {
        // Solo pasos (8000 pasos, sin entreno): ~4.9 pts
        val strainStepsOnly = StrainTargetCalculator.calculateDayStrain(steps = 8000, workoutTrimpSum = 0.0)
        assertTrue("Strain solo pasos debe estar en 4.0..5.5", strainStepsOnly in 4.0..5.5)

        // Entreno moderado (80 TRIMP + 8000 pasos): ~12.8 pts (zona moderada/óptima)
        val strainModerate = StrainTargetCalculator.calculateDayStrain(steps = 8000, workoutTrimpSum = 80.0)
        assertTrue("Strain moderado debe estar en 11.5..14.0", strainModerate in 11.5..14.0)

        // Entreno muy duro (180 TRIMP + 10000 pasos): ~17.4 pts
        val strainHard = StrainTargetCalculator.calculateDayStrain(steps = 10000, workoutTrimpSum = 180.0)
        assertTrue("Strain duro debe estar en 16.0..18.5", strainHard in 16.0..18.5)

        // Carga extrema no desborda escala 21.0
        val strainExtreme = StrainTargetCalculator.calculateDayStrain(steps = 40000, workoutTrimpSum = 800.0)
        assertTrue("Strain extremo debe estar acotado <= 21.0", strainExtreme <= 21.0)
    }
}
