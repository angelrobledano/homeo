package com.salud.personal.data.ble.huami.alerts

import java.nio.charset.StandardCharsets

object HuamiAlertPacketBuilder {

    /**
     * Construye un paquete de alerta BLE para Amazfit GTR 3 (Zepp OS).
     * @param title Título breve (ej. "Preparación: 82%")
     * @param body Cuerpo descriptivo sin texto superfluo
     */
    fun buildAlertPacket(title: String, body: String): ByteArray {
        val titleBytes = title.take(30).toByteArray(StandardCharsets.UTF_8)
        val bodyBytes = body.take(120).toByteArray(StandardCharsets.UTF_8)

        // Formato estándar de alerta Huami/Zepp OS:
        // [0x05 (tipo alerta simple), longitud título, ...título..., longitud cuerpo, ...cuerpo...]
        val result = ByteArray(2 + titleBytes.size + 1 + bodyBytes.size)
        result[0] = 0x05 // Alert Type
        result[1] = titleBytes.size.toByte()
        System.arraycopy(titleBytes, 0, result, 2, titleBytes.size)

        val bodyOffset = 2 + titleBytes.size
        result[bodyOffset] = bodyBytes.size.toByte()
        System.arraycopy(bodyBytes, 0, result, bodyOffset + 1, bodyBytes.size)

        return result
    }
}
