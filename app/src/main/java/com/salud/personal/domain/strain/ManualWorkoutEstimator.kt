package com.salud.personal.domain.strain

import com.salud.personal.data.local.entity.WorkoutType
import kotlin.math.roundToInt

object ManualWorkoutEstimator {

    /**
     * Estima la carga de entrenamiento en puntos Banister TRIMP
     * a partir de la duración (minutos), el esfuerzo percibido RPE (1..10)
     * y el tipo de disciplina según el método Foster Session-RPE validado.
     */
    fun estimateTrimp(
        type: WorkoutType,
        durationMinutes: Int,
        rpe: Int
    ): Double {
        val safeDuration = durationMinutes.coerceIn(1, 480)
        val safeRpe = rpe.coerceIn(1, 10)

        // Factor de disciplina por demanda cardiovascular vs neural
        val typeMultiplier = when (type) {
            WorkoutType.CROSSFIT_WOD -> 0.22
            WorkoutType.ENDURANCE_CARDIO -> 0.20
            WorkoutType.STRENGTH -> 0.17
            WorkoutType.WALK -> 0.10
            WorkoutType.RECOVERY -> 0.08
        }

        // Foster sRPE = Duration * RPE
        val srpe = safeDuration * safeRpe.toDouble()
        val estimatedTrimp = srpe * typeMultiplier

        return (kotlin.math.round(estimatedTrimp * 10.0) / 10.0).coerceAtLeast(1.0)
    }

    /**
     * Estima las calorías quemadas aproximadas según el tipo y RPE
     */
    fun estimateCalories(
        type: WorkoutType,
        durationMinutes: Int,
        rpe: Int,
        bodyWeightKg: Double = 75.0
    ): Int {
        val baseMet = when (type) {
            WorkoutType.CROSSFIT_WOD -> 5.0 + (rpe * 0.6)
            WorkoutType.ENDURANCE_CARDIO -> 4.5 + (rpe * 0.7)
            WorkoutType.STRENGTH -> 3.5 + (rpe * 0.5)
            WorkoutType.WALK -> 2.5 + (rpe * 0.3)
            WorkoutType.RECOVERY -> 2.0
        }
        val hours = durationMinutes / 60.0
        val kcal = baseMet * bodyWeightKg * hours
        return kcal.roundToInt().coerceAtLeast(10)
    }
}
