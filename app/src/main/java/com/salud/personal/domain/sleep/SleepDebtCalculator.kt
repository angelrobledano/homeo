package com.salud.personal.domain.sleep

import com.salud.personal.data.local.entity.SleepSessionEntity
import kotlin.math.min
import kotlin.math.roundToInt

enum class SleepDebtStatus {
    NONE,      // <= 15 min
    LOW,       // 16 .. 60 min
    MODERATE,  // 61 .. 120 min
    HIGH       // > 120 min
}

data class CumulativeSleepDebt(
    val baselineNeedHours: Double,
    val extraNeedTrimpHours: Double,
    val totalSleepNeedHours: Double,
    val totalDebtMinutes: Int,
    val dailyCompensationMinutes: Int,
    val adjustedBedtimeNeedHours: Double,
    val status: SleepDebtStatus,
    val analyzedNights: Int
)

/**
 * Calculador fisiológico de deuda de sueño acumulada en una ventana móvil de 7 días.
 *
 * Fundamento fisiológico (estilo Whoop / Oura):
 * 1. Cada noche con déficit respecto a la necesidad biológica suma minutos a la deuda.
 * 2. Las noches con excedente reducen parcialmente la deuda pendiente.
 * 3. La compensación sugerida para la noche siguiente se limita a un máximo saludable
 *    (45 minutos) para evitar desajustes del ritmo circadiano y desvelo forzado.
 */
object SleepDebtCalculator {

    const val DEFAULT_BASELINE_NEED_HOURS = 7.75
    const val MAX_DAILY_COMPENSATION_MINUTES = 45

    fun calculate7DayDebt(
        recentSleeps: List<SleepSessionEntity>,
        baselineNeedHours: Double = DEFAULT_BASELINE_NEED_HOURS,
        recentTrimp: Double = 0.0
    ): CumulativeSleepDebt {
        // Ventana de hasta las últimas 7 noches ordenadas por fecha reciente
        val last7Sleeps = recentSleeps
            .sortedByDescending { it.date }
            .take(7)

        val targetNightMinutes = (baselineNeedHours * 60).roundToInt()
        var runningDebtMinutes = 0

        for (session in last7Sleeps) {
            val diffMinutes = targetNightMinutes - session.totalSleepMinutes
            runningDebtMinutes += diffMinutes
        }

        // Si el usuario durmió de más, la deuda no puede ser negativa
        val netDebtMinutes = runningDebtMinutes.coerceAtLeast(0)

        // Compensación sugerida para hoy: pagar entre 1/3 y la totalidad de la deuda, tope 45 min
        val rawCompensation = if (netDebtMinutes <= 15) {
            0
        } else if (netDebtMinutes <= 45) {
            netDebtMinutes
        } else {
            (netDebtMinutes / 3).coerceAtLeast(30)
        }
        val dailyCompensationMinutes = min(rawCompensation, MAX_DAILY_COMPENSATION_MINUTES)

        val status = when {
            netDebtMinutes <= 15 -> SleepDebtStatus.NONE
            netDebtMinutes <= 60 -> SleepDebtStatus.LOW
            netDebtMinutes <= 120 -> SleepDebtStatus.MODERATE
            else -> SleepDebtStatus.HIGH
        }

        val extraNeedTrimpHours = when {
            recentTrimp > 150.0 -> 0.50
            recentTrimp > 80.0 -> 0.25
            else -> 0.0
        }

        val totalSleepNeedHours = baselineNeedHours + extraNeedTrimpHours
        val adjustedBedtimeNeedHours = totalSleepNeedHours + (dailyCompensationMinutes / 60.0)

        return CumulativeSleepDebt(
            baselineNeedHours = baselineNeedHours,
            extraNeedTrimpHours = extraNeedTrimpHours,
            totalSleepNeedHours = totalSleepNeedHours,
            totalDebtMinutes = netDebtMinutes,
            dailyCompensationMinutes = dailyCompensationMinutes,
            adjustedBedtimeNeedHours = adjustedBedtimeNeedHours,
            status = status,
            analyzedNights = last7Sleeps.size
        )
    }
}
