package com.salud.personal.domain.training

import kotlin.math.exp

/**
 * Calculadora de carga de entrenamiento Banister TRIMP (Training Impulse)
 * y zonas de frecuencia cardíaca.
 */
object TrainingLoadCalculator {

    enum class Gender { MALE, FEMALE }

    data class HeartRateZones(
        val z1Recovery: ClosedRange<Int>,
        val z2Aerobic: ClosedRange<Int>,
        val z3Tempo: ClosedRange<Int>,
        val z4Threshold: ClosedRange<Int>,
        val z5Anaerobic: ClosedRange<Int>
    )

    /**
     * Calcula las 5 zonas de FC basadas en FC máx.
     * Z1: 50-60%, Z2: 60-70%, Z3: 70-80%, Z4: 80-90%, Z5: 90-100%
     */
    fun calculateZones(maxHr: Int): HeartRateZones {
        val safeMax = maxHr.coerceIn(100, 240)
        return HeartRateZones(
            z1Recovery = (safeMax * 0.50).toInt()..(safeMax * 0.60).toInt(),
            z2Aerobic = (safeMax * 0.60).toInt()..(safeMax * 0.70).toInt(),
            z3Tempo = (safeMax * 0.70).toInt()..(safeMax * 0.80).toInt(),
            z4Threshold = (safeMax * 0.80).toInt()..(safeMax * 0.90).toInt(),
            z5Anaerobic = (safeMax * 0.90).toInt()..safeMax
        )
    }

    /**
     * Banister TRIMP = Duración (min) * DeltaHR * Y
     * donde:
     * DeltaHR = (avgHr - restHr) / (maxHr - restHr)
     * Y = b * exp(a * DeltaHR)
     * Para hombres: b = 0.64, a = 1.92
     * Para mujeres: b = 0.86, a = 1.67
     */
    fun calculateBanisterTrimp(
        durationMinutes: Double,
        avgHr: Double,
        restHr: Double,
        maxHr: Double,
        gender: Gender = Gender.MALE
    ): Double {
        if (durationMinutes <= 0 || maxHr <= restHr) return 0.0
        val clampedAvgHr = avgHr.coerceIn(restHr, maxHr)
        val deltaHr = (clampedAvgHr - restHr) / (maxHr - restHr)

        val (b, a) = when (gender) {
            Gender.MALE -> Pair(0.64, 1.92)
            Gender.FEMALE -> Pair(0.86, 1.67)
        }

        val trimp = durationMinutes * deltaHr * b * exp(a * deltaHr)
        return kotlin.math.round(trimp * 10.0) / 10.0
    }

    enum class LoadLevel(
        val title: String,
        val rationale: String
    ) {
        RECOVERY(
            title = "Mantenimiento / Descarga",
            rationale = "Estímulo ligero, ideal para semanas de descarga o recuperación activa."
        ),
        OPTIMAL(
            title = "Carga Óptima",
            rationale = "Rango estimulante ideal de adaptación cardiovascular y supercompensación."
        ),
        HIGH(
            title = "Sobrecarga Alta",
            rationale = "Volumen o intensidad exigente. Prioriza descanso y nutrición."
        ),
        EXTREME(
            title = "Sobrecarga Excesiva",
            rationale = "Carga desproporcionada. Riesgo de fatiga aguda o sobreentrenamiento."
        )
    }

    /**
     * Evalúa la carga semanal de entrenamiento (TRIMP 7 días).
     * < 150 pts: Mantenimiento / Descarga
     * 150 - 450 pts: Zona óptima de adaptación cardiovascular
     * 450 - 650 pts: Sobrecarga funcional
     * > 650 pts: Sobrecarga excesiva
     */
    fun evaluateWeeklyLoad(weeklyTrimp: Double): LoadLevel {
        return when {
            weeklyTrimp < 150.0 -> LoadLevel.RECOVERY
            weeklyTrimp <= 450.0 -> LoadLevel.OPTIMAL
            weeklyTrimp <= 650.0 -> LoadLevel.HIGH
            else -> LoadLevel.EXTREME
        }
    }
}
