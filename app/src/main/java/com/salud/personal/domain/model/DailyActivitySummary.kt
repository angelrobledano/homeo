package com.salud.personal.domain.model

import java.time.LocalDate

data class DailyActivitySummary(
    val date: LocalDate,
    val steps: Int,
    val distanceMeters: Int,
    val caloriesBurned: Int,
    val sourceDevice: String
) {
    init {
        require(steps >= 0) {
            "Los pasos no pueden ser negativos (recibido: $steps)"
        }
        require(distanceMeters >= 0) {
            "La distancia no puede ser negativa (recibido: $distanceMeters)"
        }
        require(caloriesBurned >= 0) {
            "Las calorías no pueden ser negativas (recibido: $caloriesBurned)"
        }
        require(sourceDevice.isNotBlank()) {
            "El dispositivo de origen no puede estar vacío"
        }
    }
}
