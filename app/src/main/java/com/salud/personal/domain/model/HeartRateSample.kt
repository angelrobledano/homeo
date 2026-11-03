package com.salud.personal.domain.model

import java.time.Instant

data class HeartRateSample(
    val timestamp: Instant,
    val bpm: Int,
    val sourceDevice: String
) {
    init {
        require(bpm in 20..300) {
            "El ritmo cardíaco debe estar entre 20 y 300 bpm (recibido: $bpm)"
        }
        require(sourceDevice.isNotBlank()) {
            "El dispositivo de origen no puede estar vacío"
        }
    }
}
