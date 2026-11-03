package com.salud.personal.data.ble.renpho

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QnScalePacketParserTest {

    private val parser = QnScalePacketParser()

    @Test
    fun parse_stabilizedPacketWithImpedance_returnsValidMeasurement() {
        // Weight: 75.00 kg -> 7500 (0x1D4C)
        // Status: 0x01 (bit 0 = 1 -> stabilized)
        // Impedance: 500 ohms -> 0x01F4
        val rawData = byteArrayOf(
            0x02.toByte(),               // Byte 0: Header
            0x1D.toByte(), 0x4C.toByte(), // Bytes 1-2: Weight (7500 -> 75.00 kg)
            0x01.toByte(),               // Byte 3: Status (stabilized)
            0x01.toByte(), 0xF4.toByte()  // Bytes 4-5: Impedance (500 ohms)
        )

        val result = parser.parse(rawData)

        assertNotNull(result)
        assertEquals(75.00, result!!.weightKg, 0.001)
        assertTrue(result.isStabilized)
        assertEquals(500.0, result.impedanceOhms ?: 0.0, 0.001)
    }

    @Test
    fun parse_unstabilizedPacket_returnsIsStabilizedFalse() {
        // Weight: 68.45 kg -> 6845 (0x1ABD)
        // Status: 0x00 (bit 0 = 0 -> in motion / unstabilized)
        // Impedance: 480 ohms -> 0x01E0
        val rawData = byteArrayOf(
            0x02.toByte(),
            0x1A.toByte(), 0xBD.toByte(),
            0x00.toByte(),
            0x01.toByte(), 0xE0.toByte()
        )

        val result = parser.parse(rawData)

        assertNotNull(result)
        assertEquals(68.45, result!!.weightKg, 0.001)
        assertFalse(result.isStabilized)
        assertEquals(480.0, result.impedanceOhms ?: 0.0, 0.001)
    }

    @Test
    fun parse_packetWithStatusFlagsOtherBitsSet_evaluatesBitZeroCorrectly() {
        // Bit 0 = 0 even if higher bits set (e.g. 0xFE = 11111110b)
        val unstabilizedData = byteArrayOf(
            0x01.toByte(),
            0x1D.toByte(), 0x4C.toByte(),
            0xFE.toByte(),
            0x01.toByte(), 0xF4.toByte()
        )
        val unstabilizedResult = parser.parse(unstabilizedData)
        assertNotNull(unstabilizedResult)
        assertFalse(unstabilizedResult!!.isStabilized)

        // Bit 0 = 1 when higher bits set (e.g. 0xFF = 11111111b)
        val stabilizedData = byteArrayOf(
            0x01.toByte(),
            0x1D.toByte(), 0x4C.toByte(),
            0xFF.toByte(),
            0x01.toByte(), 0xF4.toByte()
        )
        val stabilizedResult = parser.parse(stabilizedData)
        assertNotNull(stabilizedResult)
        assertTrue(stabilizedResult!!.isStabilized)
    }

    @Test
    fun parse_zeroImpedance_returnsNullImpedance() {
        // Weight: 80.00 kg -> 8000 (0x1F40)
        // Status: 0x01
        // Impedance: 0 ohms (0x0000)
        val rawData = byteArrayOf(
            0x02.toByte(),
            0x1F.toByte(), 0x40.toByte(),
            0x01.toByte(),
            0x00.toByte(), 0x00.toByte()
        )

        val result = parser.parse(rawData)

        assertNotNull(result)
        assertEquals(80.00, result!!.weightKg, 0.001)
        assertTrue(result.isStabilized)
        assertNull(result.impedanceOhms)
    }

    @Test
    fun parse_corruptedOrShortPackets_returnsNull() {
        assertNull("Empty byte array should return null", parser.parse(byteArrayOf()))
        assertNull("1 byte packet should return null", parser.parse(byteArrayOf(0x02)))
        assertNull("2 byte packet should return null", parser.parse(byteArrayOf(0x02, 0x1D)))
        assertNull("3 byte packet should return null", parser.parse(byteArrayOf(0x02, 0x1D, 0x4C)))
        assertNull("4 byte packet should return null", parser.parse(byteArrayOf(0x02, 0x1D, 0x4C, 0x01)))
        assertNull("5 byte packet should return null", parser.parse(byteArrayOf(0x02, 0x1D, 0x4C, 0x01, 0x01)))
    }

    @Test
    fun parse_zeroWeight_returnsNull() {
        val rawData = byteArrayOf(
            0x02.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x01.toByte(),
            0x01.toByte(), 0xF4.toByte()
        )

        val result = parser.parse(rawData)

        assertNull("Zero weight should return null", result)
    }

    @Test
    fun parse_longerPacket_parsesFirstSixBytesSuccessfully() {
        // Scale might send extra checksum or manufacturer bytes at the end
        val rawData = byteArrayOf(
            0x02.toByte(),
            0x1D.toByte(), 0x4C.toByte(),
            0x01.toByte(),
            0x01.toByte(), 0xF4.toByte(),
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()
        )

        val result = parser.parse(rawData)

        assertNotNull(result)
        assertEquals(75.00, result!!.weightKg, 0.001)
        assertTrue(result.isStabilized)
        assertEquals(500.0, result.impedanceOhms ?: 0.0, 0.001)
    }
}
