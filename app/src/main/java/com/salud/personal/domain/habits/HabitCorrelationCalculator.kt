package com.salud.personal.domain.habits

import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import kotlin.math.roundToInt

enum class HabitType(
    val title: String,
    val icon: String,
    val description: String
) {
    LATE_MEAL("Cena tardía", "🌙", "Cenar <2h antes de dormir"),
    LATE_CAFFEINE("Cafeína tarde", "☕", "Café o estimulantes después de 17h"),
    ALCOHOL("Alcohol", "🍷", "Consumo de bebidas alcohólicas"),
    HIGH_STRESS("Estrés elevado", "⚡", "Tensión o sobrecarga mental"),
    SUPPLEMENTS("Suplementación", "💊", "Magnesio, creatina u otros"),
    SCREEN_BEFORE_BED("Pantallas en cama", "📱", "Uso de móvil/pantalla antes de dormir")
}

enum class HabitImpact {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

data class HabitInsight(
    val habit: HabitType,
    val sampleCount: Int,
    val recoveryDelta: Int,         // Ej: -15% o +10%
    val rhrDelta: Double,           // Ej: +4.2 bpm
    val restorativeSleepDeltaPct: Int?, // Ej: -12%
    val impact: HabitImpact,
    val insightSummary: String,
    val isStatisticallyReliable: Boolean = true
)

object HabitCorrelationCalculator {

    const val CORRELATION_DISCLAIMER =
        "Las tendencias de hábitos reflejan correlaciones observadas en tu diario personal; no implican causalidad directa ni constituyen consejo médico."

    fun computeInsights(
        checkIns: List<DailyCheckInEntity>,
        sleepSessions: List<SleepSessionEntity>
    ): List<HabitInsight> {
        if (checkIns.isEmpty()) return emptyList()

        val sleepByDate = sleepSessions.associateBy { it.date }
        val insights = mutableListOf<HabitInsight>()

        for (habit in HabitType.values()) {
            val isHabitActive: (DailyCheckInEntity) -> Boolean = when (habit) {
                HabitType.LATE_MEAL -> { checkIn -> checkIn.lateMeal }
                HabitType.LATE_CAFFEINE -> { checkIn -> checkIn.lateCaffeine }
                HabitType.ALCOHOL -> { checkIn -> checkIn.alcohol }
                HabitType.HIGH_STRESS -> { checkIn -> checkIn.highStress }
                HabitType.SUPPLEMENTS -> { checkIn -> checkIn.supplements }
                HabitType.SCREEN_BEFORE_BED -> { checkIn -> checkIn.screenBeforeBed }
            }

            val activeList = checkIns.filter { isHabitActive(it) }
            val inactiveList = checkIns.filter { !isHabitActive(it) }

            if (activeList.isEmpty() || inactiveList.isEmpty()) {
                continue
            }

            // 1. Recovery Score Delta
            val activeRecScores = activeList.mapNotNull { it.calculatedRecoveryScore }
            val inactiveRecScores = inactiveList.mapNotNull { it.calculatedRecoveryScore }
            val recDelta = if (activeRecScores.isNotEmpty() && inactiveRecScores.isNotEmpty()) {
                (activeRecScores.average() - inactiveRecScores.average()).roundToInt()
            } else 0

            // 2. Resting Heart Rate Delta
            val activeRhr = activeList.mapNotNull { it.restingHeartRate }
            val inactiveRhr = inactiveList.mapNotNull { it.restingHeartRate }
            val rhrDelta = if (activeRhr.isNotEmpty() && inactiveRhr.isNotEmpty()) {
                ((activeRhr.average() - inactiveRhr.average()) * 10).roundToInt() / 10.0
            } else 0.0

            // 3. Restorative sleep delta
            val activeRestorative = activeList.mapNotNull { checkIn ->
                sleepByDate[checkIn.date]?.let { s ->
                    val total = s.totalSleepMinutes
                    if (total > 0) ((s.deepSleepMinutes + s.remSleepMinutes).toDouble() / total * 100).roundToInt() else null
                }
            }
            val inactiveRestorative = inactiveList.mapNotNull { checkIn ->
                sleepByDate[checkIn.date]?.let { s ->
                    val total = s.totalSleepMinutes
                    if (total > 0) ((s.deepSleepMinutes + s.remSleepMinutes).toDouble() / total * 100).roundToInt() else null
                }
            }
            val restorativeDelta = if (activeRestorative.isNotEmpty() && inactiveRestorative.isNotEmpty()) {
                (activeRestorative.average() - inactiveRestorative.average()).roundToInt()
            } else null

            // Determine impact
            val impact = when {
                recDelta <= -5 || rhrDelta >= 3.0 || (restorativeDelta != null && restorativeDelta <= -5) -> HabitImpact.NEGATIVE
                recDelta >= 5 || rhrDelta <= -2.0 || (restorativeDelta != null && restorativeDelta >= 5) -> HabitImpact.POSITIVE
                else -> HabitImpact.NEUTRAL
            }

            val isReliable = activeList.size >= 3 && inactiveList.size >= 3
            val reliabilitySuffix = if (!isReliable) " (Tendencia preliminar, N=${activeList.size} d)" else ""

            val summary = when (impact) {
                HabitImpact.NEGATIVE -> {
                    when {
                        rhrDelta >= 2.0 -> "Eleva tu FC en reposo +${rhrDelta} lpm y reduce tu recuperación en ${-recDelta}%$reliabilitySuffix"
                        restorativeDelta != null && restorativeDelta < 0 -> "Reduce tu sueño reparador en ${-restorativeDelta}%$reliabilitySuffix"
                        else -> "Disminuye tu recuperación matutina en ${-recDelta}%$reliabilitySuffix"
                    }
                }
                HabitImpact.POSITIVE -> {
                    when {
                        restorativeDelta != null && restorativeDelta > 0 -> "Aumenta tu sueño restaurativo en +${restorativeDelta}%$reliabilitySuffix"
                        else -> "Mejora tu recuperación general en +${recDelta}%$reliabilitySuffix"
                    }
                }
                HabitImpact.NEUTRAL -> "Sin impacto estadístico significativo en tu recuperación actual$reliabilitySuffix"
            }

            insights.add(
                HabitInsight(
                    habit = habit,
                    sampleCount = activeList.size,
                    recoveryDelta = recDelta,
                    rhrDelta = rhrDelta,
                    restorativeSleepDeltaPct = restorativeDelta,
                    impact = impact,
                    insightSummary = summary,
                    isStatisticallyReliable = isReliable
                )
            )
        }

        return insights.sortedBy { it.impact == HabitImpact.NEGATIVE }
    }
}
