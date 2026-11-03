package com.salud.personal.domain.trends

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calculadora matemática de tendencias a medio y largo plazo:
 * 1. Acute:Chronic Workload Ratio (ACWR): carga aguda (7d) vs crónica (28d).
 * 2. Trend Weight: Media móvil exponencial (EMA) para aislar retención de agua y glucógeno (estilo MacroFactor).
 * 3. Historial y línea base de frecuencia cardíaca en reposo a 30 días.
 * 4. Comparador de rendimiento "Semana actual vs Semana anterior".
 */
object TrendsCalculator {

    enum class AcwrRiskCategory {
        INSUFFICIENT_DATA,// Sin historial de entrenamientos para calcular ratio
        UNDER_TRAINING,   // Ratio < 0.8: posible pérdida de adaptación
        OPTIMAL_SWEETSPOT,// Ratio 0.8..1.3: zona óptima de ganancia
        HIGH_RISK,        // Ratio 1.3..1.5: precaución
        VERY_HIGH_RISK    // Ratio > 1.5: zona de peligro / alto riesgo de lesión
    }

    data class AcwrResult(
        val ratio: Double,
        val acuteLoad7d: Double,
        val chronicLoad28d: Double,
        val riskCategory: AcwrRiskCategory
    )

    /**
     * Calcula el ACWR (Acute:Chronic Workload Ratio).
     * @param acuteLoadSum Carga total TRIMP de los últimos 7 días.
     * @param chronicLoadSum Carga total TRIMP del periodo crónico (hasta 28 días).
     * @param historyDaysAvailable Días reales de datos disponibles en el historial. Requiere mínimo 14 días para rigor estadístico.
     */
    fun calculateAcwr(
        acuteLoadSum: Double,
        chronicLoadSum: Double,
        historyDaysAvailable: Int = 28
    ): AcwrResult {
        if ((acuteLoadSum <= 0.0 && chronicLoadSum <= 0.0) || historyDaysAvailable < 14) {
            return AcwrResult(
                ratio = 0.0,
                acuteLoad7d = acuteLoadSum,
                chronicLoad28d = chronicLoadSum,
                riskCategory = AcwrRiskCategory.INSUFFICIENT_DATA
            )
        }

        val weeksAvailable = (historyDaysAvailable / 7.0).coerceIn(2.0, 4.0)
        val safeChronicAvgWeekly = (chronicLoadSum / weeksAvailable).coerceAtLeast(10.0)
        val ratio = acuteLoadSum / safeChronicAvgWeekly

        val category = when {
            ratio < 0.8 -> AcwrRiskCategory.UNDER_TRAINING
            ratio <= 1.3 -> AcwrRiskCategory.OPTIMAL_SWEETSPOT
            ratio <= 1.5 -> AcwrRiskCategory.HIGH_RISK
            else -> AcwrRiskCategory.VERY_HIGH_RISK
        }

        return AcwrResult(
            ratio = kotlin.math.round(ratio * 100.0) / 100.0,
            acuteLoad7d = acuteLoadSum,
            chronicLoad28d = chronicLoadSum,
            riskCategory = category
        )
    }

    /**
     * Calcula la media móvil simple de una serie temporal (ej. peso).
     */
    fun calculateMovingAverage(values: List<Double>, windowSize: Int): List<Double> {
        if (values.isEmpty() || windowSize <= 0) return emptyList()
        val result = mutableListOf<Double>()
        for (i in values.indices) {
            val start = (i - windowSize + 1).coerceAtLeast(0)
            val sublist = values.subList(start, i + 1)
            result.add(sublist.average())
        }
        return result
    }

    // --- Trend Weight (EMA / Hacker's Diet / MacroFactor) ---

    data class WeightPoint(
        val date: LocalDate,
        val weightKg: Double
    )

    data class TrendWeightPoint(
        val date: LocalDate,
        val rawWeightKg: Double,
        val trendWeightKg: Double,
        val waterNoiseKg: Double
    )

    data class WeightTrendResult(
        val currentTrendKg: Double,
        val currentRawKg: Double,
        val waterNoiseKg: Double,
        val weeklyRateKg: Double?,
        val points: List<TrendWeightPoint>
    )

    /**
     * Calcula la curva de peso real amortiguando las fluctuaciones diarias de agua y glucógeno (EMA).
     * Trend_0 = Raw_0
     * Trend_t = Trend_{t-1} + alpha * (Raw_t - Trend_{t-1})
     */
    fun calculateExponentialTrendWeight(
        weights: List<WeightPoint>,
        alpha: Double = 0.10
    ): WeightTrendResult {
        if (weights.isEmpty()) {
            return WeightTrendResult(0.0, 0.0, 0.0, null, emptyList())
        }

        // Orden cronológico
        val sorted = weights.sortedBy { it.date }
        val points = mutableListOf<TrendWeightPoint>()

        var currentTrend = sorted.first().weightKg
        for (wp in sorted) {
            currentTrend += alpha * (wp.weightKg - currentTrend)
            val noise = wp.weightKg - currentTrend
            points.add(
                TrendWeightPoint(
                    date = wp.date,
                    rawWeightKg = kotlin.math.round(wp.weightKg * 100.0) / 100.0,
                    trendWeightKg = kotlin.math.round(currentTrend * 100.0) / 100.0,
                    waterNoiseKg = kotlin.math.round(noise * 100.0) / 100.0
                )
            )
        }

        val lastPoint = points.last()
        // Tasa de cambio semanal (requiere al menos 3 días de diferencia temporal para tener sentido fisiológico)
        val weeklyRate = if (points.size >= 2) {
            val first = points.first()
            val daysDiff = ChronoUnit.DAYS.between(first.date, lastPoint.date)
            if (daysDiff >= 3) {
                val totalDelta = lastPoint.trendWeightKg - first.trendWeightKg
                kotlin.math.round(((totalDelta / daysDiff) * 7.0) * 100.0) / 100.0
            } else {
                null
            }
        } else {
            null
        }

        return WeightTrendResult(
            currentTrendKg = lastPoint.trendWeightKg,
            currentRawKg = lastPoint.rawWeightKg,
            waterNoiseKg = lastPoint.waterNoiseKg,
            weeklyRateKg = weeklyRate,
            points = points
        )
    }

    // --- RHR 30-Day Trend & Baseline ---

    data class DailyRhrPoint(
        val date: LocalDate,
        val rhr: Int
    )

    enum class RhrStatus {
        EXCELLENT, // rhr <= baseline - 3
        NORMAL,    // baseline - 2 .. baseline + 2
        ELEVATED,  // baseline + 3 .. baseline + 5
        HIGH_ALERT // rhr >= baseline + 6
    }

    data class RhrTrendResult(
        val latestRhr: Int?,
        val baselineRhr: Double,
        val deltaFromBaseline: Double,
        val status: RhrStatus,
        val points30d: List<DailyRhrPoint>
    )

    /**
     * Calcula la línea base a 30 días de FC en reposo y el estado de recuperación del último valor.
     */
    fun calculateRhrTrend(dailyRhrs: List<DailyRhrPoint>): RhrTrendResult {
        if (dailyRhrs.isEmpty()) {
            return RhrTrendResult(
                latestRhr = null,
                baselineRhr = 0.0,
                deltaFromBaseline = 0.0,
                status = RhrStatus.NORMAL,
                points30d = emptyList()
            )
        }

        val sorted = dailyRhrs.sortedBy { it.date }
        val baseline = sorted.map { it.rhr }.average()
        val latest = sorted.last().rhr
        val delta = latest - baseline

        val status = when {
            delta <= -3.0 -> RhrStatus.EXCELLENT
            delta in -2.0..2.0 -> RhrStatus.NORMAL
            delta in 2.01..5.0 -> RhrStatus.ELEVATED
            else -> RhrStatus.HIGH_ALERT
        }

        return RhrTrendResult(
            latestRhr = latest,
            baselineRhr = kotlin.math.round(baseline * 10.0) / 10.0,
            deltaFromBaseline = kotlin.math.round(delta * 10.0) / 10.0,
            status = status,
            points30d = sorted
        )
    }

    // --- Comparador "Semana actual vs Semana anterior" ---

    data class WeeklyComparison(
        val currentWeekTrimp: Double,
        val previousWeekTrimp: Double,
        val trimpDeltaPct: Int,
        val currentAvgSteps: Int,
        val previousAvgSteps: Int,
        val stepsDeltaPct: Int,
        val currentAvgSleepMinutes: Int,
        val previousAvgSleepMinutes: Int,
        val sleepDeltaMinutes: Int,
        val currentAvgRhr: Double?,
        val previousAvgRhr: Double?,
        val rhrDeltaBpm: Double?
    )

    fun calculateWeeklyComparison(
        currentWeekTrimp: Double,
        previousWeekTrimp: Double,
        currentSteps: List<Int>,
        previousSteps: List<Int>,
        currentSleepMinutes: List<Int>,
        previousSleepMinutes: List<Int>,
        currentRhrs: List<Int>,
        previousRhrs: List<Int>
    ): WeeklyComparison {
        val trimpDelta = if (previousWeekTrimp > 0.0) {
            (((currentWeekTrimp - previousWeekTrimp) / previousWeekTrimp) * 100.0).toInt()
        } else {
            0
        }

        val curAvgSteps = if (currentSteps.isNotEmpty()) currentSteps.average().toInt() else 0
        val prevAvgSteps = if (previousSteps.isNotEmpty()) previousSteps.average().toInt() else 0
        val stepsDelta = if (prevAvgSteps > 0) {
            (((curAvgSteps - prevAvgSteps).toDouble() / prevAvgSteps) * 100.0).toInt()
        } else {
            0
        }

        val curAvgSleep = if (currentSleepMinutes.isNotEmpty()) currentSleepMinutes.average().toInt() else 0
        val prevAvgSleep = if (previousSleepMinutes.isNotEmpty()) previousSleepMinutes.average().toInt() else 0
        val sleepDelta = curAvgSleep - prevAvgSleep

        val curAvgRhr = if (currentRhrs.isNotEmpty()) {
            kotlin.math.round(currentRhrs.average() * 10.0) / 10.0
        } else null

        val prevAvgRhr = if (previousRhrs.isNotEmpty()) {
            kotlin.math.round(previousRhrs.average() * 10.0) / 10.0
        } else null

        val rhrDelta = if (curAvgRhr != null && prevAvgRhr != null) {
            kotlin.math.round((curAvgRhr - prevAvgRhr) * 10.0) / 10.0
        } else null

        return WeeklyComparison(
            currentWeekTrimp = currentWeekTrimp,
            previousWeekTrimp = previousWeekTrimp,
            trimpDeltaPct = trimpDelta,
            currentAvgSteps = curAvgSteps,
            previousAvgSteps = prevAvgSteps,
            stepsDeltaPct = stepsDelta,
            currentAvgSleepMinutes = curAvgSleep,
            previousAvgSleepMinutes = prevAvgSleep,
            sleepDeltaMinutes = sleepDelta,
            currentAvgRhr = curAvgRhr,
            previousAvgRhr = prevAvgRhr,
            rhrDeltaBpm = rhrDelta
        )
    }
}
