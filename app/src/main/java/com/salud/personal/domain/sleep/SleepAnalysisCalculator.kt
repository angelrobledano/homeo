package com.salud.personal.domain.sleep

import kotlin.math.roundToInt

enum class SleepQualityTier {
    EXCELLENT,
    GOOD,
    INSUFFICIENT
}

enum class SleepStage {
    DEEP,
    REM,
    LIGHT,
    AWAKE
}

enum class StageBalance {
    LOW,
    OPTIMAL,
    HIGH
}

data class RestorativeSleepResult(
    val restorativeMinutes: Int,
    val percentage: Int,
    val tier: SleepQualityTier
)

/**
 * Calculador fisiológico para el estudio y análisis de sueño.
 * Evalúa sueño restaurativo (profundo + REM), deuda acumulada respecto a carga de entreno
 * y balance porcentual de cada fase del sueño.
 */
object SleepAnalysisCalculator {

    /**
     * Calcula los minutos y porcentaje de sueño restaurativo (profundo + REM).
     * El sueño restaurativo es el indicador clave de recuperación física y neural en atletas.
     */
    fun calculateRestorativeSleep(
        deepMinutes: Int,
        remMinutes: Int,
        totalMinutes: Int
    ): RestorativeSleepResult {
        val restorative = deepMinutes + remMinutes
        val percentage = if (totalMinutes > 0) {
            ((restorative.toDouble() / totalMinutes) * 100).roundToInt()
        } else {
            0
        }

        val tier = when {
            percentage >= 40 -> SleepQualityTier.EXCELLENT
            percentage >= 30 -> SleepQualityTier.GOOD
            else -> SleepQualityTier.INSUFFICIENT
        }

        return RestorativeSleepResult(
            restorativeMinutes = restorative,
            percentage = percentage,
            tier = tier
        )
    }

    /**
     * Calcula la necesidad fisiológica de sueño ajustada por el esfuerzo cardiovascular reciente.
     * Cargas elevadas de entrenamiento requieren mayor tiempo de sueño para reparación tisular y del SNC.
     */
    fun calculateSleepNeed(
        baseHours: Double = 7.75,
        recentTrimp: Double = 0.0
    ): Double {
        val extraHours = when {
            recentTrimp > 150.0 -> 0.50  // +30 minutos si hubo alta carga
            recentTrimp > 80.0 -> 0.25   // +15 minutos si hubo carga moderada
            else -> 0.0
        }
        return baseHours + extraHours
    }

    /**
     * Calcula la deuda de sueño acumulada en horas.
     */
    fun calculateSleepDebt(
        sleepNeedHours: Double,
        actualSleepHours: Double
    ): Double {
        return (sleepNeedHours - actualSleepHours).coerceAtLeast(0.0)
    }

    /**
     * Evalúa si una fase específica del sueño se encuentra en el rango saludable recomendado.
     */
    fun assessStage(
        stage: SleepStage,
        minutes: Int,
        totalMinutes: Int
    ): StageBalance {
        if (totalMinutes <= 0) return StageBalance.LOW
        val ratio = minutes.toDouble() / totalMinutes

        return when (stage) {
            SleepStage.DEEP -> when {
                ratio < 0.15 -> StageBalance.LOW
                ratio <= 0.25 -> StageBalance.OPTIMAL
                else -> StageBalance.HIGH
            }
            SleepStage.REM -> when {
                ratio < 0.20 -> StageBalance.LOW
                ratio <= 0.25 -> StageBalance.OPTIMAL
                else -> StageBalance.HIGH
            }
            SleepStage.LIGHT -> when {
                ratio < 0.45 -> StageBalance.LOW
                ratio <= 0.55 -> StageBalance.OPTIMAL
                else -> StageBalance.HIGH
            }
            SleepStage.AWAKE -> when {
                ratio <= 0.10 -> StageBalance.OPTIMAL
                else -> StageBalance.HIGH
            }
        }
    }
}
