package com.salud.personal.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachHeuristicEngineTest {

    @Test
    fun generateRecommendation_lowRecovery_suggestsActiveRecovery() {
        val snapshot = UserHealthSnapshot(
            stepsToday = 3000,
            recoveryScore = 28,
            restingHeartRate = 60,
            sleepHours = 5.2,
            recent7DaysTrimp = 420.0,
            lastWorkoutType = "CROSSFIT_WOD",
            weightTrendKgDelta = 0.1
        )

        val rec = CoachHeuristicEngine.generateRecommendation(snapshot)
        assertTrue(rec.actionHeadline.contains("recuperación activa") || rec.actionHeadline.contains("descarga"))
        assertTrue(rec.supportingData.contains("28%"))
        assertTrue(rec.keyFactors.any { it.isLimiting })
    }

    @Test
    fun generateRecommendation_highRecovery_suggestsIntenseTraining() {
        val snapshot = UserHealthSnapshot(
            stepsToday = 8000,
            recoveryScore = 85,
            restingHeartRate = 48,
            sleepHours = 8.1,
            recent7DaysTrimp = 180.0,
            lastWorkoutType = "REST",
            weightTrendKgDelta = -0.3
        )

        val rec = CoachHeuristicEngine.generateRecommendation(snapshot)
        assertTrue(rec.actionHeadline.contains("Óptimo") || rec.actionHeadline.contains("intenso"))
        assertTrue(rec.keyFactors.none { it.isLimiting })
    }

    @Test
    fun answerQuestion_strengthOrCardio_answersBasedOnRecovery() {
        val highSnapshot = UserHealthSnapshot(
            stepsToday = 6000,
            recoveryScore = 80,
            restingHeartRate = 49,
            sleepHours = 8.0,
            recent7DaysTrimp = 150.0,
            lastWorkoutType = null,
            weightTrendKgDelta = null
        )
        val answerHigh = CoachHeuristicEngine.answerQuestion("¿Fuerza o cardio hoy?", highSnapshot)
        assertTrue(answerHigh.contains("80%"))
        assertTrue(answerHigh.contains("SNC") || answerHigh.contains("pesado") || answerHigh.contains("fuerza"))

        val lowSnapshot = highSnapshot.copy(recoveryScore = 25)
        val answerLow = CoachHeuristicEngine.answerQuestion("¿Fuerza o cardio hoy?", lowSnapshot)
        assertTrue(answerLow.contains("25%"))
        assertTrue(answerLow.contains("Evita") || answerLow.contains("movilidad"))
    }

    @Test
    fun answerQuestion_sleepDebt_providesActionableAdvice() {
        val snapshot = UserHealthSnapshot(
            stepsToday = 4000,
            recoveryScore = 45,
            restingHeartRate = 54,
            sleepHours = 5.5,
            recent7DaysTrimp = 200.0,
            lastWorkoutType = null,
            weightTrendKgDelta = null
        )
        val answer = CoachHeuristicEngine.answerQuestion("¿Cómo compenso la falta de sueño?", snapshot)
        assertTrue(answer.contains("5.5h"))
        assertTrue(answer.contains("siesta") || answer.contains("adenosina"))
    }

    @Test
    fun generateIntelligence_highRecovery_generatesStructuredPrescription() {
        val snapshot = UserHealthSnapshot(
            stepsToday = 7500,
            recoveryScore = 82,
            restingHeartRate = 48,
            sleepHours = 8.0,
            recent7DaysTrimp = 220.0,
            lastWorkoutType = "WALK",
            weightTrendKgDelta = -0.2,
            sleepNeedHours = 7.75,
            sleepDebtHours = 0.0,
            targetStrainMin = 12.0,
            targetStrainMax = 15.0,
            rpeRecommendation = 8,
            targetBedtime = "23:15"
        )

        val intel = CoachHeuristicEngine.generateIntelligence(snapshot)
        assertTrue(intel.prescription.title.contains("Carga") || intel.prescription.title.contains("Rendimiento"))
        assertTrue(intel.prescription.recommendedRpe.contains("8 - 9"))
        assertEquals("7h 45m", intel.sleepPlan.sleepNeedFormatted)
        assertEquals("23:15", intel.sleepPlan.targetBedtime)
        assertEquals(4, intel.quickFaqs.size)
        assertTrue(intel.quickFaqs.any { it.id == "train_today" })
    }

    @Test
    fun generateIntelligence_acwrSpike_flagsOverreaching() {
        val snapshot = UserHealthSnapshot(
            stepsToday = 5000,
            recoveryScore = 55,
            restingHeartRate = 52,
            sleepHours = 7.0,
            recent7DaysTrimp = 500.0,
            lastWorkoutType = "CROSSFIT_WOD",
            weightTrendKgDelta = 0.0,
            acwrRatio = 1.65
        )

        val intel = CoachHeuristicEngine.generateIntelligence(snapshot)
        assertTrue(intel.loadGuard.isOverreaching)
        assertTrue(intel.loadGuard.statusText.contains("Sobrecarga"))
    }
}
