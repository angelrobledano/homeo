package com.salud.personal.domain.recovery

/**
 * Calculadora transparente y explicable de la puntuación de Recuperación (0-100%).
 * Combina:
 * 1. FC en reposo actual vs línea base (35%)
 * 2. Horas de sueño vs objetivo (35%)
 * 3. Sensaciones subjetivas del check-in matutino (30%)
 */
object RecoveryCalculator {

    data class RecoveryResult(
        val score: Int, // 0..100
        val rhrFactorScore: Int, // 0..100
        val sleepFactorScore: Int, // 0..100
        val subjectiveFactorScore: Int, // 0..100
        val primaryDriver: String
    )

    /**
     * Calcula la recuperación diaria ponderada.
     * @param currentRhr FC en reposo matutina (bpm)
     * @param baselineRhr FC en reposo media de los últimos 7 días (bpm)
     * @param sleepHours Horas de sueño reales
     * @param sleepGoalHours Objetivo de sueño (ej. 8.0)
     * @param subjectiveRating Puntuación subjetiva 1..5 (energía y sensaciones)
     */
    fun calculateRecovery(
        currentRhr: Int,
        baselineRhr: Int,
        sleepHours: Double,
        sleepGoalHours: Double = 8.0,
        subjectiveRating: Int = 3
    ): RecoveryResult {
        // 1. RHR factor: si RHR es menor o igual a baseline -> 100%. Por cada 1 bpm sobre baseline -> -5%
        val rhrDiff = currentRhr - baselineRhr
        val rhrScore = (100 - (rhrDiff * 5)).coerceIn(0, 100)

        // 2. Sleep factor: ratio de horas vs objetivo
        val safeSleepGoal = sleepGoalHours.coerceAtLeast(4.0)
        val sleepScore = ((sleepHours / safeSleepGoal) * 100).toInt().coerceIn(0, 100)

        // 3. Subjective factor: 1..5 normalizado a 20..100
        val safeRating = subjectiveRating.coerceIn(1, 5)
        val subjectiveScore = safeRating * 20

        // Ponderación: 35% RHR, 35% Sueño, 30% Subjetivo
        val total = (rhrScore * 0.35 + sleepScore * 0.35 + subjectiveScore * 0.30).toInt().coerceIn(0, 100)

        val driver = when {
            rhrScore <= sleepScore && rhrScore <= subjectiveScore -> "FC en reposo elevada (${currentRhr} bpm vs base ${baselineRhr})"
            sleepScore <= rhrScore && sleepScore <= subjectiveScore -> "Sueño insuficiente (${String.format(java.util.Locale.US, "%.1f", sleepHours)}h de ${sleepGoalHours.toInt()}h)"
            else -> "Sensación de fatiga subjetiva ($subjectiveRating/5)"
        }

        return RecoveryResult(
            score = total,
            rhrFactorScore = rhrScore,
            sleepFactorScore = sleepScore,
            subjectiveFactorScore = subjectiveScore,
            primaryDriver = driver
        )
    }
}
