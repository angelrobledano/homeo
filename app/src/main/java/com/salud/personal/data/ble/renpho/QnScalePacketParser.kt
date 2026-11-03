package com.salud.personal.data.ble.renpho

/**
 * Decodificador de paquetes Bluetooth Low Energy para básculas que usan el protocolo QN-Scale
 * (como la báscula RENPHO Elis 1 / serie ES-CS20M).
 *
 * Estructura del paquete esperado (mínimo 6 bytes):
 * - Byte 0: Cabecera / tipo de paquete.
 * - Bytes 1-2: Peso en Big-Endian (entero sin signo de 16 bits dividido por 100.0 para obtener kg).
 * - Byte 3: Status / flags (el bit 0 indica si la lectura está estabilizada).
 * - Bytes 4-5: Impedancia eléctrica en ohmios (entero sin signo de 16 bits Big-Endian).
 */
class QnScalePacketParser {

    /**
     * Parsea un array de bytes BLE crudos hacia una medición [QnScaleMeasurement].
     *
     * @param data Array de bytes recibido de la báscula BLE.
     * @return [QnScaleMeasurement] decodificada o null si el paquete es inválido,
     *         demasiado corto o contiene un peso no positivo.
     */
    fun parse(data: ByteArray): QnScaleMeasurement? {
        if (data.size < MIN_PACKET_LENGTH) {
            return null
        }

        val header = data[0].toInt() and 0xFF
        if (header != 0x02 && header != 0x01) {
            return null
        }

        val weightRaw = ((data[1].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)
        val weightKg = weightRaw / 100.0

        if (weightKg < 10.0 || weightKg > 300.0) {
            return null
        }

        val isStabilized = (data[3].toInt() and 0x01) == 0x01

        val impedanceRaw = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val impedanceOhms = if (impedanceRaw > 0) impedanceRaw.toDouble() else null

        return QnScaleMeasurement(
            weightKg = weightKg,
            isStabilized = isStabilized,
            impedanceOhms = impedanceOhms
        )
    }

    companion object {
        private const val MIN_PACKET_LENGTH = 6
    }
}
