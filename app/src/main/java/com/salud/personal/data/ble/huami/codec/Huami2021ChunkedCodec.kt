package com.salud.personal.data.ble.huami.codec

import java.nio.ByteBuffer
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

/**
 * Reassembled Zepp OS 2021 chunked BLE message.
 */
data class DecodedMessage(
    val type: Short,
    val payload: ByteArray,
    val handle: Byte,
    val count: Byte
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecodedMessage

        if (type != other.type) return false
        if (!payload.contentEquals(other.payload)) return false
        if (handle != other.handle) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.toInt()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + handle
        result = 31 * result + count
        return result
    }
}

/**
 * Protocol flags for Huami 2021 chunked transport.
 */
object Huami2021Flags {
    const val FIRST: Byte = 0x01
    const val LAST: Byte = 0x02
    const val NEEDS_ACK: Byte = 0x04
    const val ENCRYPTED: Byte = 0x08
    const val HEADER_PREFIX: Byte = 0x03
}

/**
 * Encoder for Zepp OS 2021 chunked BLE transport packets.
 */
class Huami2021ChunkedEncoder(
    var mtu: Int = 23
) {
    private var writeHandle: Byte = 0
    private var sequenceNumber: Int = 0
    private var sessionKey: ByteArray? = null

    companion object {
        fun calcMaxWriteChunk(mtu: Int): Int = mtu - 3
    }

    fun setEncryptionParameters(sequenceNumber: Int, sessionKey: ByteArray) {
        this.sequenceNumber = sequenceNumber
        this.sessionKey = sessionKey.copyOf()
    }

    fun setMTU(mtu: Int) {
        this.mtu = mtu
    }

    fun reset() {
        writeHandle = 0
        sequenceNumber = 0
    }

    /**
     * Encodes a message type and payload into one or more BLE chunks.
     */
    fun encode(type: Short, data: ByteArray, encrypt: Boolean = false): List<ByteArray> {
        if (encrypt && sessionKey == null) {
            throw IllegalStateException("Cannot encrypt without session key")
        }

        writeHandle = (writeHandle + 1).toByte()

        val length = data.size
        val dataToSend: ByteArray
        if (encrypt) {
            val key = sessionKey!!
            val messageKey = ByteArray(16) { i -> (key[i].toInt() xor writeHandle.toInt()).toByte() }

            var encryptedLength = length + 8
            val overflow = encryptedLength % 16
            if (overflow > 0) {
                encryptedLength += (16 - overflow)
            }

            val encryptablePayload = ByteArray(encryptedLength)
            System.arraycopy(data, 0, encryptablePayload, 0, length)

            encryptablePayload[length] = (sequenceNumber and 0xff).toByte()
            encryptablePayload[length + 1] = ((sequenceNumber shr 8) and 0xff).toByte()
            encryptablePayload[length + 2] = ((sequenceNumber shr 16) and 0xff).toByte()
            encryptablePayload[length + 3] = ((sequenceNumber shr 24) and 0xff).toByte()
            sequenceNumber++

            val crc = CRC32()
            crc.update(encryptablePayload, 0, length + 4)
            val checksum = crc.value.toInt()

            encryptablePayload[length + 4] = (checksum and 0xff).toByte()
            encryptablePayload[length + 5] = ((checksum shr 8) and 0xff).toByte()
            encryptablePayload[length + 6] = ((checksum shr 16) and 0xff).toByte()
            encryptablePayload[length + 7] = ((checksum shr 24) and 0xff).toByte()

            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(messageKey, "AES"))
            dataToSend = cipher.doFinal(encryptablePayload)
        } else {
            dataToSend = data
        }

        val chunks = mutableListOf<ByteArray>()
        var remaining = dataToSend.size
        var count: Byte = 0
        var headerSize = 11

        do {
            val maxChunkLength = calcMaxWriteChunk(mtu) - headerSize
            val copyBytes = min(remaining, maxChunkLength)
            val chunk = ByteArray(copyBytes + headerSize)

            var flags = 0
            if (encrypt) {
                flags = flags or Huami2021Flags.ENCRYPTED.toInt()
            }
            if (count == 0.toByte()) {
                flags = flags or Huami2021Flags.FIRST.toInt()
                var idx = 5
                chunk[idx++] = (length and 0xff).toByte()
                chunk[idx++] = ((length shr 8) and 0xff).toByte()
                chunk[idx++] = ((length shr 16) and 0xff).toByte()
                chunk[idx++] = ((length shr 24) and 0xff).toByte()
                chunk[idx++] = (type.toInt() and 0xff).toByte()
                chunk[idx] = ((type.toInt() shr 8) and 0xff).toByte()
            }
            if (remaining <= maxChunkLength) {
                flags = flags or Huami2021Flags.LAST.toInt()
                flags = flags or Huami2021Flags.NEEDS_ACK.toInt()
            }

            chunk[0] = Huami2021Flags.HEADER_PREFIX
            chunk[1] = flags.toByte()
            chunk[2] = 0x00
            chunk[3] = writeHandle
            chunk[4] = count

            if (copyBytes > 0) {
                System.arraycopy(dataToSend, dataToSend.size - remaining, chunk, headerSize, copyBytes)
            }

            chunks.add(chunk)
            remaining -= copyBytes
            headerSize = 5
            count = (count + 1).toByte()
        } while (remaining > 0)

        return chunks
    }
}

/**
 * Decoder for Zepp OS 2021 chunked BLE transport packets.
 */
class Huami2021ChunkedDecoder {
    private var sessionKey: ByteArray? = null
    private var currentHandle: Byte? = null
    private var currentType: Short = 0
    private var currentLength: Int = 0
    private var isEncrypted: Boolean = false
    private var expectedCount: Byte = 0
    private var reassemblyBuffer: ByteBuffer? = null

    var lastHandle: Byte = 0
        private set

    var lastCount: Byte = 0
        private set

    var lastNeedsAck: Boolean = false
        private set

    fun setEncryptionParameters(sessionKey: ByteArray) {
        this.sessionKey = sessionKey.copyOf()
    }

    fun reset() {
        currentHandle = null
        currentType = 0
        currentLength = 0
        isEncrypted = false
        expectedCount = 0
        reassemblyBuffer = null
    }

    /**
     * Decodes an incoming BLE chunk.
     * Returns a [DecodedMessage] when the final chunk of a message arrives and verifies,
     * or null if intermediate or corrupted.
     */
    fun decode(chunk: ByteArray): DecodedMessage? {
        if (chunk.size < 5 || chunk[0] != Huami2021Flags.HEADER_PREFIX) {
            return null
        }

        val flags = chunk[1].toInt() and 0xff
        val encrypted = (flags and Huami2021Flags.ENCRYPTED.toInt()) != 0
        val firstChunk = (flags and Huami2021Flags.FIRST.toInt()) != 0
        val lastChunk = (flags and Huami2021Flags.LAST.toInt()) != 0
        val needsAck = (flags and Huami2021Flags.NEEDS_ACK.toInt()) != 0

        val handle = chunk[3]
        val count = chunk[4]

        lastHandle = handle
        lastCount = count
        lastNeedsAck = needsAck

        val headerSize: Int
        if (firstChunk) {
            if (chunk.size < 11 || count != 0.toByte()) {
                return null
            }
            if (currentHandle != null) {
                reset()
            }

            val len0 = chunk[5].toInt() and 0xff
            val len1 = chunk[6].toInt() and 0xff
            val len2 = chunk[7].toInt() and 0xff
            val len3 = chunk[8].toInt() and 0xff
            val fullLength = len0 or (len1 shl 8) or (len2 shl 16) or (len3 shl 24)

            if (fullLength < 0) {
                return null
            }
            currentLength = fullLength

            val type0 = chunk[9].toInt() and 0xff
            val type1 = chunk[10].toInt() and 0xff
            currentType = (type0 or (type1 shl 8)).toShort()

            var allocSize = fullLength
            if (encrypted) {
                var encLen = fullLength + 8
                val overflow = encLen % 16
                if (overflow > 0) {
                    encLen += (16 - overflow)
                }
                allocSize = encLen
            }

            reassemblyBuffer = ByteBuffer.allocate(allocSize)
            currentHandle = handle
            isEncrypted = encrypted
            expectedCount = 1
            headerSize = 11
        } else {
            if (currentHandle == null || handle != currentHandle || count != expectedCount) {
                reset()
                return null
            }
            expectedCount = (expectedCount + 1).toByte()
            headerSize = 5
        }

        val payloadBytesCount = chunk.size - headerSize
        val buffer = reassemblyBuffer ?: return null
        if (payloadBytesCount > buffer.remaining()) {
            reset()
            return null
        }
        buffer.put(chunk, headerSize, payloadBytesCount)

        if (lastChunk) {
            if (buffer.hasRemaining()) {
                reset()
                return null
            }

            val completeBuffer = buffer.array()
            val finalPayload: ByteArray
            if (isEncrypted) {
                val key = sessionKey ?: run {
                    reset()
                    return null
                }
                val messageKey = ByteArray(16) { idx -> (key[idx].toInt() xor handle.toInt()).toByte() }
                val decrypted = try {
                    val cipher = Cipher.getInstance("AES/ECB/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(messageKey, "AES"))
                    cipher.doFinal(completeBuffer)
                } catch (e: Exception) {
                    reset()
                    return null
                }

                if (decrypted.size < currentLength + 8) {
                    reset()
                    return null
                }

                val crc = CRC32()
                crc.update(decrypted, 0, currentLength + 4)
                val computedCrc = crc.value.toInt()

                val b0 = decrypted[currentLength + 4].toInt() and 0xff
                val b1 = decrypted[currentLength + 5].toInt() and 0xff
                val b2 = decrypted[currentLength + 6].toInt() and 0xff
                val b3 = decrypted[currentLength + 7].toInt() and 0xff
                val storedCrc = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)

                if (computedCrc != storedCrc) {
                    reset()
                    return null
                }

                finalPayload = decrypted.copyOfRange(0, currentLength)
            } else {
                finalPayload = completeBuffer
            }

            val result = DecodedMessage(
                type = currentType,
                payload = finalPayload,
                handle = handle,
                count = count
            )
            reset()
            return result
        }

        return null
    }
}
