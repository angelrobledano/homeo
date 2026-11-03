package com.salud.personal.data.ble.huami.codec

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Huami2021ChunkedCodecTest {

    private val testSessionKey = byteArrayOf(
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10
    )

    @Test
    fun unencryptedSingleChunk_encodeAndDecode() {
        val encoder = Huami2021ChunkedEncoder(mtu = 23)
        val decoder = Huami2021ChunkedDecoder()

        val payload = byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70)
        val type: Short = 0x0082

        val chunks = encoder.encode(type = type, data = payload, encrypt = false)
        assertEquals("Single chunk should be generated", 1, chunks.size)

        val chunk = chunks[0]
        // Header: byte 0 is 0x03
        assertEquals(0x03.toByte(), chunk[0])
        // Flags: 0x01 (FIRST) | 0x02 (LAST) | 0x04 (NEEDS_ACK) = 0x07
        assertEquals(0x07.toByte(), chunk[1])
        // Extended flags: byte 2 is 0x00
        assertEquals(0x00.toByte(), chunk[2])
        // Handle: 1
        assertEquals(0x01.toByte(), chunk[3])
        // Count: 0
        assertEquals(0x00.toByte(), chunk[4])

        // Length (4 bytes LE) = 7
        assertEquals(0x07.toByte(), chunk[5])
        assertEquals(0x00.toByte(), chunk[6])
        assertEquals(0x00.toByte(), chunk[7])
        assertEquals(0x00.toByte(), chunk[8])

        // Type (2 bytes LE) = 0x0082
        assertEquals(0x82.toByte(), chunk[9])
        assertEquals(0x00.toByte(), chunk[10])

        // Payload
        val chunkPayload = chunk.copyOfRange(11, chunk.size)
        assertArrayEquals(payload, chunkPayload)

        // Decode
        val decoded = decoder.decode(chunk)
        assertNotNull("Decoder should return DecodedMessage on last chunk", decoded)
        assertEquals(type, decoded!!.type)
        assertArrayEquals(payload, decoded.payload)
        assertEquals(0x01.toByte(), decoded.handle)
        assertEquals(0x00.toByte(), decoded.count)
    }

    @Test
    fun unencryptedMultiChunk_encodeAndDecode() {
        val encoder = Huami2021ChunkedEncoder(mtu = 23)
        val decoder = Huami2021ChunkedDecoder()

        // With MTU 23: max write chunk = 20.
        // First chunk header is 11 bytes, so payload in first chunk is 9 bytes.
        // Subsequent chunk header is 5 bytes, so payload in subsequent chunk is 15 bytes.
        // 25 bytes payload: Chunk 0 (9 bytes), Chunk 1 (15 bytes), Chunk 2 (1 byte).
        val payload = ByteArray(25) { it.toByte() }
        val type: Short = 0x0007

        val chunks = encoder.encode(type = type, data = payload, encrypt = false)
        assertEquals("Should split into 3 chunks", 3, chunks.size)

        // Chunk 0: FIRST (0x01), count 0
        assertEquals(0x03.toByte(), chunks[0][0])
        assertEquals(0x01.toByte(), chunks[0][1]) // FIRST only
        assertEquals(0x00.toByte(), chunks[0][2])
        assertEquals(0x01.toByte(), chunks[0][3]) // handle 1
        assertEquals(0x00.toByte(), chunks[0][4]) // count 0
        assertEquals(20, chunks[0].size) // 11 header + 9 payload

        // Chunk 1: intermediate, count 1
        assertEquals(0x03.toByte(), chunks[1][0])
        assertEquals(0x00.toByte(), chunks[1][1]) // neither first nor last
        assertEquals(0x00.toByte(), chunks[1][2])
        assertEquals(0x01.toByte(), chunks[1][3]) // handle 1
        assertEquals(0x01.toByte(), chunks[1][4]) // count 1
        assertEquals(20, chunks[1].size) // 5 header + 15 payload

        // Chunk 2: LAST (0x02) | NEEDS_ACK (0x04) = 0x06, count 2
        assertEquals(0x03.toByte(), chunks[2][0])
        assertEquals(0x06.toByte(), chunks[2][1])
        assertEquals(0x00.toByte(), chunks[2][2])
        assertEquals(0x01.toByte(), chunks[2][3]) // handle 1
        assertEquals(0x02.toByte(), chunks[2][4]) // count 2
        assertEquals(6, chunks[2].size) // 5 header + 1 payload

        // Feed to decoder
        val res0 = decoder.decode(chunks[0])
        assertNull("Intermediate chunk 0 must return null", res0)

        val res1 = decoder.decode(chunks[1])
        assertNull("Intermediate chunk 1 must return null", res1)

        val res2 = decoder.decode(chunks[2])
        assertNotNull("Last chunk must return reassembled DecodedMessage", res2)
        assertEquals(type, res2!!.type)
        assertArrayEquals(payload, res2.payload)
        assertEquals(0x01.toByte(), res2.handle)
        assertEquals(0x02.toByte(), res2.count)
    }

    @Test
    fun encryptedEncodeAndDecode_withKnownSessionKeyAndSequenceNumber() {
        val encoder = Huami2021ChunkedEncoder(mtu = 512)
        val decoder = Huami2021ChunkedDecoder()

        val initialSeq = 100
        encoder.setEncryptionParameters(sequenceNumber = initialSeq, sessionKey = testSessionKey)
        decoder.setEncryptionParameters(sessionKey = testSessionKey)

        val payload = "Amazfit GTR 3 Secret Health Data".toByteArray(Charsets.UTF_8)
        val type: Short = 0x0010

        val chunks = encoder.encode(type = type, data = payload, encrypt = true)
        assertEquals(1, chunks.size)

        val chunk = chunks[0]
        assertEquals(0x03.toByte(), chunk[0])
        // Flags: FIRST (0x01) | LAST (0x02) | NEEDS_ACK (0x04) | ENCRYPTED (0x08) = 0x0F
        assertEquals(0x0F.toByte(), chunk[1])
        assertEquals(0x00.toByte(), chunk[2])
        val handle = chunk[3]
        assertEquals(0x01.toByte(), handle)

        // Decode
        val decoded = decoder.decode(chunk)
        assertNotNull("Decoder must successfully decrypt and return message", decoded)
        assertEquals(type, decoded!!.type)
        assertArrayEquals(payload, decoded.payload)
        assertEquals(handle, decoded.handle)

        // Test second message to verify sequence number and handle increment
        val payload2 = "Second Payload".toByteArray(Charsets.UTF_8)
        val chunks2 = encoder.encode(type = type, data = payload2, encrypt = true)
        val chunk2 = chunks2[0]
        assertEquals(0x02.toByte(), chunk2[3]) // handle incremented to 2

        val decoded2 = decoder.decode(chunk2)
        assertNotNull("Second message must decrypt properly", decoded2)
        assertArrayEquals(payload2, decoded2!!.payload)
        assertEquals(0x02.toByte(), decoded2.handle)
    }

    @Test
    fun encryptedMultiChunk_encodeAndDecode() {
        val encoder = Huami2021ChunkedEncoder(mtu = 30) // smaller MTU forcing multi-chunk encrypted
        val decoder = Huami2021ChunkedDecoder()

        encoder.setEncryptionParameters(sequenceNumber = 1, sessionKey = testSessionKey)
        decoder.setEncryptionParameters(sessionKey = testSessionKey)

        val payload = ByteArray(40) { (it * 3).toByte() }
        val type: Short = 0x0005

        val chunks = encoder.encode(type = type, data = payload, encrypt = true)
        assertTrue("Should produce multiple chunks", chunks.size > 1)

        var finalResult: DecodedMessage? = null
        for (i in chunks.indices) {
            val result = decoder.decode(chunks[i])
            if (i < chunks.size - 1) {
                assertNull("Chunk $i should return null", result)
            } else {
                finalResult = result
            }
        }

        assertNotNull("Final chunk must return decoded message", finalResult)
        assertEquals(type, finalResult!!.type)
        assertArrayEquals(payload, finalResult.payload)
    }

    @Test
    fun crc32CorruptionRejection_onEncryptedChunks() {
        val encoder = Huami2021ChunkedEncoder(mtu = 512)
        val decoder = Huami2021ChunkedDecoder()

        encoder.setEncryptionParameters(sequenceNumber = 10, sessionKey = testSessionKey)
        decoder.setEncryptionParameters(sessionKey = testSessionKey)

        val payload = "Important Metric Data".toByteArray(Charsets.UTF_8)
        val type: Short = 0x0082

        val chunks = encoder.encode(type = type, data = payload, encrypt = true)
        val corruptedChunk = chunks[0].copyOf()

        // Corrupt an encrypted payload byte (header is 11 bytes, corrupt byte at index 15)
        corruptedChunk[15] = (corruptedChunk[15].toInt() xor 0xFF).toByte()

        val decoded = decoder.decode(corruptedChunk)
        assertNull("Decoder must reject chunk with corrupted CRC32 / payload", decoded)
    }

    @Test
    fun outOfOrderOrNon0x03ChunkRejection() {
        val decoder = Huami2021ChunkedDecoder()

        // 1. Non 0x03 starting byte
        val invalidHeaderChunk = byteArrayOf(0x04, 0x07, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x55)
        assertNull("Non-0x03 header byte must be rejected", decoder.decode(invalidHeaderChunk))

        // 2. Out-of-order chunk: intermediate chunk (count = 1) arrives without first chunk
        val intermediateWithoutFirst = byteArrayOf(0x03, 0x00, 0x00, 0x01, 0x01, 0xAA.toByte(), 0xBB.toByte())
        assertNull("Intermediate chunk before first chunk must be rejected", decoder.decode(intermediateWithoutFirst))

        // 3. Multi-chunk sequence where chunk 1 is skipped (chunk 0 then chunk 2)
        val encoder = Huami2021ChunkedEncoder(mtu = 23)
        val payload = ByteArray(25) { it.toByte() }
        val chunks = encoder.encode(type = 0x0001, data = payload, encrypt = false)
        assertEquals(3, chunks.size)

        assertNull(decoder.decode(chunks[0])) // chunk 0 (count 0)
        // Feed chunk 2 (count 2) skipping chunk 1 (count 1)
        assertNull("Skipping chunk 1 must be rejected", decoder.decode(chunks[2]))

        // 4. Chunk with unexpected handle during reassembly
        decoder.reset()
        val encoder2 = Huami2021ChunkedEncoder(mtu = 23)
        val chunksA = encoder2.encode(type = 0x0001, data = payload, encrypt = false)
        assertNull(decoder.decode(chunksA[0])) // handle = 1

        val fakeChunkWrongHandle = chunksA[1].copyOf()
        fakeChunkWrongHandle[3] = 0x09.toByte() // change handle from 1 to 9
        assertNull("Chunk with wrong handle during reassembly must be rejected", decoder.decode(fakeChunkWrongHandle))
    }

    @Test
    fun missingSessionKey_throwsOrFails() {
        val encoder = Huami2021ChunkedEncoder()
        var threw = false
        try {
            encoder.encode(type = 0x0001, data = byteArrayOf(1, 2, 3), encrypt = true)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue("Encoding encrypted payload without session key must throw IllegalStateException", threw)

        val decoder = Huami2021ChunkedDecoder()
        // Provide an encrypted chunk to decoder without session key
        encoder.setEncryptionParameters(1, testSessionKey)
        val encryptedChunks = encoder.encode(type = 0x0001, data = byteArrayOf(1, 2, 3), encrypt = true)
        assertNull("Decoder without session key must fail to decrypt and return null", decoder.decode(encryptedChunks[0]))
    }
}
