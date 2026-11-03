package com.salud.personal.domain.vitals

import kotlin.math.roundToInt

enum class MetricBaselineStatus {
    IN_BASELINE,
    OUT_OF_BASELINE
}

data class MetricBaselineEvaluation(
    val name: String,
    val currentValue: String,
    val baselineRangeText: String,
    val status: MetricBaselineStatus,
    val alertMessage: String? = null
)

data class VitalsEvaluationResult(
    val metricsInBaseline: Int,
    val totalMetricsEvaluated: Int,
    val evaluations: List<MetricBaselineEvaluation>,
    val alerts: List<String>
) {
    val allInBaseline: Boolean
        get() = totalMetricsEvaluated > 0 && metricsInBaseline == totalMetricsEvaluated
}

object VitalsBaselineCalculator {

    fun evaluate(
        todayRhr: Int?,
        rhrHistory: List<Int>,
        todaySleepHours: Double?,
        sleepHistory: List<Double>,
        todayRestorativePct: Int?,
        restorativeHistory: List<Int>,
        todayWeightKg: Double?,
        weightHistory: List<Double>
    ): VitalsEvaluationResult {
        val evaluations = mutableListOf<MetricBaselineEvaluation>()
        val alerts = mutableListOf<String>()

        // 1. Resting Heart Rate (RHR)
        if (todayRhr != null) {
            val avg = if (rhrHistory.isNotEmpty()) rhrHistory.average() else todayRhr.toDouble()
            val minRange = (avg - 3.0).roundToInt().coerceAtLeast(35)
            val maxRange = (avg + 3.0).roundToInt()
            val inRange = todayRhr in minRange..maxRange
            val alert = when {
                todayRhr > maxRange -> "FC en reposo ${todayRhr - maxRange} bpm sobre el rango habitual (posible fatiga o estrés)"
                todayRhr < minRange -> "FC en reposo ${minRange - todayRhr} bpm bajo rango (recuperación muy profunda)"
                else -> null
            }
            if (alert != null && todayRhr > maxRange) alerts.add(alert)

            evaluations.add(
                MetricBaselineEvaluation(
                    name = "FC en reposo",
                    currentValue = "$todayRhr lpm",
                    baselineRangeText = "$minRange - $maxRange lpm",
                    status = if (inRange) MetricBaselineStatus.IN_BASELINE else MetricBaselineStatus.OUT_OF_BASELINE,
                    alertMessage = alert
                )
            )
        }

        // 2. Horas de Sueño
        if (todaySleepHours != null) {
            val avg = if (sleepHistory.isNotEmpty()) sleepHistory.average() else todaySleepHours
            val minRange = (avg - 0.75).coerceAtLeast(4.0)
            val maxRange = avg + 0.75
            val inRange = todaySleepHours in minRange..maxRange
            val alert = if (todaySleepHours < minRange) {
                val deficitMin = ((minRange - todaySleepHours) * 60).roundToInt()
                "Sueño ${deficitMin}m por debajo de tu rango base"
            } else null
            if (alert != null) alerts.add(alert)

            evaluations.add(
                MetricBaselineEvaluation(
                    name = "Horas de sueño",
                    currentValue = String.format("%.1fh", todaySleepHours),
                    baselineRangeText = "${String.format("%.1f", minRange)} - ${String.format("%.1f", maxRange)}h",
                    status = if (inRange) MetricBaselineStatus.IN_BASELINE else MetricBaselineStatus.OUT_OF_BASELINE,
                    alertMessage = alert
                )
            )
        }

        // 3. Sueño Restaurativo (% Profundo + REM)
        if (todayRestorativePct != null) {
            val avg = if (restorativeHistory.isNotEmpty()) restorativeHistory.average() else todayRestorativePct.toDouble()
            val minRange = (avg - 5.0).roundToInt().coerceAtLeast(20)
            val maxRange = (avg + 5.0).roundToInt().coerceAtMost(65).coerceAtLeast(minRange)
            val inRange = todayRestorativePct >= minRange
            val alert = if (todayRestorativePct < minRange) {
                "Sueño restaurativo ${minRange - todayRestorativePct}% menor a tu media habitual"
            } else null
            if (alert != null) alerts.add(alert)

            evaluations.add(
                MetricBaselineEvaluation(
                    name = "Sueño restaurativo",
                    currentValue = "$todayRestorativePct%",
                    baselineRangeText = "$minRange - $maxRange%",
                    status = if (inRange) MetricBaselineStatus.IN_BASELINE else MetricBaselineStatus.OUT_OF_BASELINE,
                    alertMessage = alert
                )
            )
        }

        // 4. Peso Corporal (con filtro de cordura fisiológica: 35 - 250 kg)
        val validWeightHistory = weightHistory.filter { it in 35.0..250.0 }
        if (todayWeightKg != null && todayWeightKg in 35.0..250.0) {
            val avg = if (validWeightHistory.isNotEmpty()) validWeightHistory.average() else todayWeightKg
            val minRange = avg - 1.0
            val maxRange = avg + 1.0
            val inRange = todayWeightKg in minRange..maxRange

            evaluations.add(
                MetricBaselineEvaluation(
                    name = "Peso corporal",
                    currentValue = String.format("%.1f kg", todayWeightKg),
                    baselineRangeText = "${String.format("%.1f", minRange)} - ${String.format("%.1f", maxRange)} kg",
                    status = if (inRange) MetricBaselineStatus.IN_BASELINE else MetricBaselineStatus.OUT_OF_BASELINE,
                    alertMessage = null
                )
            )
        }

        val inBaselineCount = evaluations.count { it.status == MetricBaselineStatus.IN_BASELINE }

        return VitalsEvaluationResult(
            metricsInBaseline = inBaselineCount,
            totalMetricsEvaluated = evaluations.size,
            evaluations = evaluations,
            alerts = alerts
        )
    }
}
