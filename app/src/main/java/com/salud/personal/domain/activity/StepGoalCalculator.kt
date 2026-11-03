package com.salud.personal.domain.activity

/**
 * Calculadora y estimaciones del objetivo diario de pasos.
 */
object StepGoalCalculator {

    const val DEFAULT_STEP_GOAL = 10_000

    // Cadencia promedio de caminata tranquila/moderada (pasos por minuto)
    const val AVERAGE_STEPS_PER_MINUTE = 100

    /**
     * Calcula los pasos restantes hasta la meta. Si ya se alcanzó, devuelve 0.
     */
    fun calculateRemainingSteps(currentSteps: Int, goal: Int = DEFAULT_STEP_GOAL): Int {
        val safeCurrent = currentSteps.coerceAtLeast(0)
        val safeGoal = goal.coerceAtLeast(1)
        return (safeGoal - safeCurrent).coerceAtLeast(0)
    }

    /**
     * Calcula el porcentaje de avance hacia la meta (0.0 a 1.0+).
     */
    fun calculateProgressRatio(currentSteps: Int, goal: Int = DEFAULT_STEP_GOAL): Float {
        val safeCurrent = currentSteps.coerceAtLeast(0)
        val safeGoal = goal.coerceAtLeast(1)
        return safeCurrent.toFloat() / safeGoal.toFloat()
    }

    /**
     * Estima los minutos de caminata necesarios para completar los pasos restantes.
     */
    fun estimateMinutesRemaining(
        remainingSteps: Int,
        stepsPerMinute: Int = AVERAGE_STEPS_PER_MINUTE
    ): Int {
        val safeRemaining = remainingSteps.coerceAtLeast(0)
        val safeCadence = stepsPerMinute.coerceAtLeast(1)
        if (safeRemaining == 0) return 0
        return kotlin.math.ceil(safeRemaining.toDouble() / safeCadence).toInt()
    }
}
