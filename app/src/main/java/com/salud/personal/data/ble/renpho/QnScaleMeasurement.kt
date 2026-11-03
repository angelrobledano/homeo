package com.salud.personal.data.ble.renpho

/**
 * Representa una medición cruda decodificada de la báscula RENPHO Elis 1 (protocolo QN-Scale).
 *
 * @property weightKg Peso medido en kilogramos con precisión de dos decimales.
 * @property isStabilized Indica si el peso ha sido bloqueado/estabilizado en la pantalla de la báscula.
 * @property impedanceOhms Resistencia bioeléctrica medida en ohmios, o null si la impedancia es cero o no está disponible.
 */
data class QnScaleMeasurement(
    val weightKg: Double,
    val isStabilized: Boolean,
    val impedanceOhms: Double?
)
