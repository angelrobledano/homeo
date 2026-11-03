package com.salud.personal.domain.strain

import kotlin.math.min
import kotlin.math.roundToInt

enum class StrainCapacityTier {
    HIGH,      // Recovery >= 67% (Ready for high strain)
    MODERATE,  // Recovery 34-66% (Standard maintenance)
    LOW        // Recovery <= 33% (Active recovery / minimal strain)
}

enum class StrainStatus {
    BELOW_TARGET,  // Need more exertion to hit optimal adaptation
    OPTIMAL,       // Inside recommended target window
    OVER_STRAIN    // Exceeding optimal capacity (fatigue risk)
}

data class StrainTargetRange(
    val minStrain: Double,
    val maxStrain: Double,
    val tier: StrainCapacityTier
) {
    val displayRange: String
        get() = "${String.format("%.1f", minStrain)} - ${String.format("%.1f", maxStrain)} pts"
}

object StrainTargetCalculator {

    /**
     * Calcula el rango objetivo de esfuerzo diario recomendado según la recuperación matutina (1-100%).
     */
    fun calculateTarget(recoveryScore: Int): StrainTargetRange {
        val clampedScore = recoveryScore.coerceIn(1, 100)
        return when {
            clampedScore >= 67 -> {
                // Rango óptimo alto: 12.0 - 16.0 pts
                val min = 12.0 + (clampedScore - 67) * 0.03
                val max = 15.0 + (clampedScore - 67) * 0.05
                StrainTargetRange(
                    minStrain = (min * 10).roundToInt() / 10.0,
                    maxStrain = (max * 10).roundToInt() / 10.0,
                    tier = StrainCapacityTier.HIGH
                )
            }
            clampedScore >= 34 -> {
                // Rango moderado: 8.0 - 12.0 pts
                val min = 8.0 + (clampedScore - 34) * 0.06
                val max = 11.0 + (clampedScore - 34) * 0.06
                StrainTargetRange(
                    minStrain = (min * 10).roundToInt() / 10.0,
                    maxStrain = (max * 10).roundToInt() / 10.0,
                    tier = StrainCapacityTier.MODERATE
                )
            }
            else -> {
                // Rango bajo / descarga: 4.0 - 7.5 pts
                val min = 4.0 + (clampedScore - 1) * 0.03
                val max = 7.0 + (clampedScore - 1) * 0.03
                StrainTargetRange(
                    minStrain = (min * 10).roundToInt() / 10.0,
                    maxStrain = (max * 10).roundToInt() / 10.0,
                    tier = StrainCapacityTier.LOW
                )
            }
        }
    }

    /**
     * Evalúa si el esfuerzo acumulado actual está por debajo, en rango o en exceso respecto al objetivo.
     */
    fun assessStatus(currentStrain: Double, target: StrainTargetRange): StrainStatus {
        return when {
            currentStrain < target.minStrain -> StrainStatus.BELOW_TARGET
            currentStrain > target.maxStrain -> StrainStatus.OVER_STRAIN
            else -> StrainStatus.OPTIMAL
        }
    }

    /**
     * Acumula el esfuerzo total del día combinando el movimiento basal (pasos) y el esfuerzo de entrenamientos (TRIMP).
     * Modela la saturación cardiovascular en la escala estándar 0.0 a 21.0 pts mediante una función logarítmica/exponencial,
     * garantizando que una sesión exigente (100-180 TRIMP) se ubique con rigor en el rango objetivo (10-17 pts)
     * sin desbordar la escala ni sumar unidades incompatibles linealmente.
     */
    fun calculateDayStrain(steps: Int, workoutTrimpSum: Double): Double {
        val safeSteps = steps.coerceAtLeast(0)
        val safeTrimp = workoutTrimpSum.coerceAtLeast(0.0)
        val effectiveLoad = safeTrimp + (safeSteps * 0.004)
        if (effectiveLoad <= 0.0) return 0.0

        val strain = 21.0 * (1.0 - kotlin.math.exp(-effectiveLoad / 120.0))
        return (strain * 10.0).roundToInt() / 10.0
    }
}
