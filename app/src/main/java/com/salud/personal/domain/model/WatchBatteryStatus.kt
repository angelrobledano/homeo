package com.salud.personal.domain.model

import java.time.Instant

data class WatchBatteryStatus(
    val percentage: Int,
    val isCharging: Boolean,
    val timestamp: Instant
) {
    init {
        require(percentage in 0..100) {
            "El porcentaje de batería debe estar entre 0 y 100 (recibido: $percentage)"
        }
    }
}
