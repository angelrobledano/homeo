package com.salud.personal.domain.summary

import java.time.LocalDate

enum class PeriodType(val displayName: String) {
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes"),
    QUARTER("Trimestre"),
    YEAR("Año")
}

data class PeriodSummary(
    val periodType: PeriodType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val title: String,
    val subtitle: String,
    val totalWorkouts: Int,
    val totalDurationMinutes: Double,
    val totalTrimp: Double,
    val avgRecoveryScore: Int?,
    val avgSleepHours: Double?,
    val totalSteps: Int,
    val avgStepsPerDay: Int,
    val weightDeltaKg: Double?,
    val optimalStrainDays: Int,
    val overStrainDays: Int,
    val peakStrainDay: LocalDate?,
    val peakStrainValue: Double?,
    val peakRecoveryDay: LocalDate?,
    val peakRecoveryValue: Int?
)
