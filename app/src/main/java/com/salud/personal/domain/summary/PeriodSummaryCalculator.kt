package com.salud.personal.domain.summary

import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

object PeriodSummaryCalculator {

    private val spanishLocale = Locale("es", "ES")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", spanishLocale)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", spanishLocale)

    /**
     * Calcula los límites [startDate, endDate] para el tipo de periodo y fecha de referencia.
     */
    fun calculateBounds(periodType: PeriodType, referenceDate: LocalDate): Pair<LocalDate, LocalDate> {
        return when (periodType) {
            PeriodType.DAY -> referenceDate to referenceDate

            PeriodType.WEEK -> {
                val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                monday to sunday
            }

            PeriodType.MONTH -> {
                val firstDay = referenceDate.withDayOfMonth(1)
                val lastDay = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
                firstDay to lastDay
            }

            PeriodType.QUARTER -> {
                val month = referenceDate.monthValue
                val firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1
                val firstDay = LocalDate.of(referenceDate.year, firstMonthOfQuarter, 1)
                val lastMonth = firstMonthOfQuarter + 2
                val lastDay = LocalDate.of(referenceDate.year, lastMonth, 1).with(TemporalAdjusters.lastDayOfMonth())
                firstDay to lastDay
            }

            PeriodType.YEAR -> {
                val firstDay = LocalDate.of(referenceDate.year, 1, 1)
                val lastDay = LocalDate.of(referenceDate.year, 12, 31)
                firstDay to lastDay
            }
        }
    }

    /**
     * Genera el título y subtítulo explicativo del periodo seleccionado.
     */
    fun formatTitles(periodType: PeriodType, startDate: LocalDate, endDate: LocalDate): Pair<String, String> {
        return when (periodType) {
            PeriodType.DAY -> {
                val title = startDate.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanishLocale))
                    .replaceFirstChar { it.uppercase() }
                title to "Balance de 24 horas"
            }
            PeriodType.WEEK -> {
                val weekOfYear = startDate.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                val rangeStr = "${startDate.format(dayMonthFormatter)} - ${endDate.format(dayMonthFormatter)}"
                "Semana $weekOfYear" to rangeStr
            }
            PeriodType.MONTH -> {
                val title = startDate.format(monthFormatter).replaceFirstChar { it.uppercase() }
                title to "${startDate.format(dayMonthFormatter)} - ${endDate.format(dayMonthFormatter)}"
            }
            PeriodType.QUARTER -> {
                val quarterNum = (startDate.monthValue - 1) / 3 + 1
                "Trimestre $quarterNum · ${startDate.year}" to "${startDate.format(dayMonthFormatter)} - ${endDate.format(dayMonthFormatter)}"
            }
            PeriodType.YEAR -> {
                "Año ${startDate.year}" to "1 ene - 31 dic"
            }
        }
    }

    /**
     * Calcula el balance ejecutivo completo para el periodo.
     */
    fun computeSummary(
        periodType: PeriodType,
        referenceDate: LocalDate,
        workouts: List<WorkoutSessionEntity>,
        activities: List<DailyActivityEntity>,
        sleeps: List<SleepSessionEntity>,
        checkIns: List<DailyCheckInEntity>,
        weights: List<WeightMeasurementEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PeriodSummary {
        val (startDate, endDate) = calculateBounds(periodType, referenceDate)
        val (title, subtitle) = formatTitles(periodType, startDate, endDate)

        // 1. Filtrar entrenamientos en el rango
        val periodWorkouts = workouts.filter {
            val date = it.startTime.atZone(zoneId).toLocalDate()
            date in startDate..endDate
        }
        val totalWorkouts = periodWorkouts.size
        val totalDurationMinutes = periodWorkouts.sumOf { it.durationMinutes }
        val totalTrimp = periodWorkouts.sumOf { it.trimp ?: 0.0 }

        // Pico de esfuerzo (día con mayor TRIMP)
        val workoutsByDate = periodWorkouts.groupBy { it.startTime.atZone(zoneId).toLocalDate() }
        val dayTrimpMap = workoutsByDate.mapValues { entry -> entry.value.sumOf { it.trimp ?: 0.0 } }
        val peakStrainEntry = dayTrimpMap.maxByOrNull { it.value }

        // 2. Filtrar actividades
        val periodActivities = activities.filter {
            val actDate = try { LocalDate.parse(it.date) } catch (e: Exception) { null }
            actDate != null && actDate in startDate..endDate
        }
        val totalSteps = periodActivities.sumOf { it.steps }
        val daysWithSteps = periodActivities.size.coerceAtLeast(1)
        val avgStepsPerDay = totalSteps / daysWithSteps

        // 3. Filtrar sueño
        val periodSleeps = sleeps.filter { it.date in startDate..endDate }
        val avgSleepHours = if (periodSleeps.isNotEmpty()) {
            val avgMin = periodSleeps.map { it.totalSleepMinutes }.average()
            kotlin.math.round((avgMin / 60.0) * 10.0) / 10.0
        } else null

        // 4. Filtrar check-ins y recuperación
        val periodCheckIns = checkIns.filter { it.date in startDate..endDate }
        val validRecoveries = periodCheckIns.mapNotNull { it.calculatedRecoveryScore }
        val avgRecovery = if (validRecoveries.isNotEmpty()) {
            validRecoveries.average().roundToInt()
        } else null

        val peakRecovery = periodCheckIns
            .filter { it.calculatedRecoveryScore != null }
            .maxByOrNull { it.calculatedRecoveryScore ?: 0 }

        // 5. Zonas de adaptación (días óptimos vs sobrecarga según TRIMP diario)
        var optimalDays = 0
        var overStrainDays = 0
        dayTrimpMap.values.forEach { dayTrimp ->
            when {
                dayTrimp in 60.0..180.0 -> optimalDays++
                dayTrimp > 180.0 -> overStrainDays++
            }
        }

        // 6. Variación neta de peso entre primer y último registro del periodo
        val periodWeights = weights
            .filter {
                val weightDate = java.time.Instant.ofEpochMilli(it.measuredAtEpochMilli).atZone(zoneId).toLocalDate()
                weightDate in startDate..endDate
            }
            .sortedBy { it.measuredAtEpochMilli }
        val weightDeltaKg = if (periodWeights.size >= 2) {
            val delta = periodWeights.last().weightKg - periodWeights.first().weightKg
            kotlin.math.round(delta * 10.0) / 10.0
        } else null

        return PeriodSummary(
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            title = title,
            subtitle = subtitle,
            totalWorkouts = totalWorkouts,
            totalDurationMinutes = totalDurationMinutes,
            totalTrimp = totalTrimp,
            avgRecoveryScore = avgRecovery,
            avgSleepHours = avgSleepHours,
            totalSteps = totalSteps,
            avgStepsPerDay = avgStepsPerDay,
            weightDeltaKg = weightDeltaKg,
            optimalStrainDays = optimalDays,
            overStrainDays = overStrainDays,
            peakStrainDay = peakStrainEntry?.key,
            peakStrainValue = peakStrainEntry?.value,
            peakRecoveryDay = peakRecovery?.date,
            peakRecoveryValue = peakRecovery?.calculatedRecoveryScore
        )
    }
}
