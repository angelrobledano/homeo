package com.salud.personal.domain.model

import java.time.Instant

data class WeightMeasurement(
    val id: Long = 0L,
    val source: ProvenanceSource,
    val externalId: String? = null,
    val weightKg: Double,
    val bodyFatPercentage: Float? = null,
    val muscleMassKg: Double? = null,
    val impedanceOhms: Double? = null,
    val measuredAt: Instant,
    val importedAt: Instant
) {
    init {
        require(weightKg in 10.0..300.0) {
            "El peso debe estar entre 10.0 kg y 300.0 kg (recibido: $weightKg)"
        }
        bodyFatPercentage?.let {
            require(it in 3.0f..70.0f) {
                "El porcentaje de grasa corporal debe estar entre 3.0% y 70.0% (recibido: $it)"
            }
        }
        muscleMassKg?.let {
            require(it >= 5.0) {
                "La masa muscular no puede ser inferior a 5.0 kg (recibido: $it)"
            }
            require(it <= weightKg) {
                "La masa muscular ($it kg) no puede superar el peso total ($weightKg kg)"
            }
        }
        impedanceOhms?.let {
            require(it > 0.0) {
                "La impedancia debe ser positiva (recibido: $it)"
            }
        }
    }
}
