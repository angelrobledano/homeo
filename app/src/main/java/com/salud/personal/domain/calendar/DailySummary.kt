package com.salud.personal.domain.calendar

import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import java.time.LocalDate

/**
 * Resumen integral de salud y condición física de un día específico del calendario.
 */
data class DailySummary(
    val date: LocalDate,
    val steps: Int = 0,
    val stepGoal: Int = 10_000,
    val distanceMeters: Int = 0,
    val caloriesBurned: Int = 0,
    val workouts: List<WorkoutSessionEntity> = emptyList(),
    val totalWorkoutTrimp: Double = 0.0,
    val totalWorkoutMinutes: Double = 0.0,
    val sleepSession: SleepSessionEntity? = null,
    val checkIn: DailyCheckInEntity? = null,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null
) {
    val hasData: Boolean
        get() = steps > 0 || workouts.isNotEmpty() || sleepSession != null || checkIn != null || weightKg != null
}

/**
 * Agregador determinista de métricas diarias procedentes de las distintas fuentes y tablas de la base de datos.
 */
object DailySummaryAggregator {

    fun aggregate(
        date: LocalDate,
        activity: DailyActivityEntity?,
        workouts: List<WorkoutSessionEntity>,
        sleep: SleepSessionEntity?,
        checkIn: DailyCheckInEntity?,
        weightKg: Double?,
        bodyFatPct: Double?
    ): DailySummary {
        val totalTrimp = workouts.sumOf { it.trimp ?: 0.0 }
        val totalMinutes = workouts.sumOf { it.durationMinutes }

        return DailySummary(
            date = date,
            steps = activity?.steps ?: 0,
            distanceMeters = activity?.distanceMeters ?: 0,
            caloriesBurned = activity?.caloriesBurned ?: 0,
            workouts = workouts,
            totalWorkoutTrimp = totalTrimp,
            totalWorkoutMinutes = totalMinutes,
            sleepSession = sleep,
            checkIn = checkIn,
            weightKg = weightKg,
            bodyFatPct = bodyFatPct
        )
    }
}
