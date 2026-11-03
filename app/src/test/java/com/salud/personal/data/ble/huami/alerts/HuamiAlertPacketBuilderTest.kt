package com.salud.personal.data.ble.huami.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class HuamiAlertPacketBuilderTest {

    @Test
    fun buildAlertPacket_createsValidPayloadWithLengths() {
        val title = "Meta Pasos"
        val body = "Faltan 2.100 pasos"

        val packet = HuamiAlertPacketBuilder.buildAlertPacket(title, body)

        assertEquals(0x05, packet[0].toInt())
        assertEquals(title.toByteArray().size, packet[1].toInt())

        val bodyOffset = 2 + title.toByteArray().size
        assertEquals(body.toByteArray().size, packet[bodyOffset].toInt())
    }
}
