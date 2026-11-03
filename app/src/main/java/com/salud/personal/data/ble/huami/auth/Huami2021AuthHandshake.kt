package com.salud.personal.data.ble.huami.auth

import com.salud.personal.data.ble.huami.crypto.EcdhB163
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

enum class AuthResultStatus {
    SUCCESS,
    WRONG_KEY,
    ERROR
}

data class AuthChallengeResult(
    val responseCommand: ByteArray,
    val sessionKey: ByteArray,
    val sequenceNumber: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthChallengeResult

        if (!responseCommand.contentEquals(other.responseCommand)) return false
        if (!sessionKey.contentEquals(other.sessionKey)) return false
        if (sequenceNumber != other.sequenceNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = responseCommand.contentHashCode()
        result = 31 * result + sessionKey.contentHashCode()
        result = 31 * result + sequenceNumber
        return result
    }
}

/**
 * State machine and cryptographic helpers for Huami 2021 / Zepp OS authentication handshake.
 */
class Huami2021AuthHandshake(
    private val authKey: ByteArray,
    private val privateKey: ByteArray = EcdhB163.generatePrivateKey()
) {
    enum class State {
        INITIAL,
        WAITING_CHALLENGE,
        WAITING_RESPONSE,
        AUTHENTICATED,
        FAILED
    }

    var state: State = State.INITIAL
        private set

    var sessionKey: ByteArray? = null
        private set

    var sequenceNumber: Int? = null
        private set

    /**
     * Starts handshake by generating the 52-byte initialization command.
     */
    fun start(): ByteArray {
        val command = createInitCommand(privateKey)
        state = State.WAITING_CHALLENGE
        return command
    }

    /**
     * Handles the watch's challenge notification payload, deriving the session key, sequence number,
     * and generating the 33-byte double-AES response command.
     */
    fun handleChallenge(payload: ByteArray): ByteArray {
        check(state == State.WAITING_CHALLENGE) {
            "Cannot handle challenge in state $state"
        }
        val result = processChallenge(privateKey, authKey, payload)
        sessionKey = result.sessionKey
        sequenceNumber = result.sequenceNumber
        state = State.WAITING_RESPONSE
        return result.responseCommand
    }

    /**
     * Handles the watch's final auth response, returning status and updating state.
     */
    fun handleResponse(payload: ByteArray): AuthResultStatus {
        check(state == State.WAITING_RESPONSE) {
            "Cannot handle response in state $state"
        }
        val status = verifyResponse(payload)
        state = if (status == AuthResultStatus.SUCCESS) {
            State.AUTHENTICATED
        } else {
            State.FAILED
        }
        return status
    }

    fun reset() {
        state = State.INITIAL
        sessionKey = null
        sequenceNumber = null
    }

    companion object {
        const val CMD_PUB_KEY: Byte = 0x04
        const val CMD_SESSION_KEY: Byte = 0x05
        const val RESPONSE_BYTE: Byte = 0x10
        const val STATUS_SUCCESS: Byte = 0x01
        const val STATUS_WRONG_KEY: Byte = 0x25

        /**
         * Creates the 52-byte initial handshake packet:
         * [0x04, 0x02, 0x00, 0x02, <48 bytes public EC>]
         */
        fun createInitCommand(privateKey: ByteArray): ByteArray {
            val publicKey = EcdhB163.generatePublicKey(privateKey)
            val command = ByteArray(52)
            command[0] = CMD_PUB_KEY
            command[1] = 0x02
            command[2] = 0x00
            command[3] = 0x02
            System.arraycopy(publicKey, 0, command, 4, 48)
            return command
        }

        fun buildInitCommand(privateKey: ByteArray): ByteArray = createInitCommand(privateKey)

        /**
         * Processes the challenge payload sent by the watch.
         * Payload may be full 67-byte notification [0x10, 0x04, 0x01, <16 random>, <48 pubKey>]
         * or raw 64-byte [<16 random>, <48 pubKey>].
         *
         * Returns [AuthChallengeResult] containing:
         * - 33-byte command: [0x05, <16 bytes encryptedRandom1>, <16 bytes encryptedRandom2>]
         * - 16-byte sessionKey
         * - integer sequenceNumber
         */
        fun processChallenge(
            privateKey: ByteArray,
            authKey: ByteArray,
            payload: ByteArray
        ): AuthChallengeResult {
            require(authKey.size == 16) { "authKey must be 16 bytes, got ${authKey.size}" }

            val (remoteRandom, remotePublicKey) = when {
                payload.size >= 67 && payload[0] == RESPONSE_BYTE && payload[1] == CMD_PUB_KEY -> {
                    require(payload[2] == STATUS_SUCCESS) {
                        "Challenge payload indicated failure: status=0x${payload[2].toString(16)}"
                    }
                    val random = payload.copyOfRange(3, 19)
                    val pubKey = payload.copyOfRange(19, 67)
                    Pair(random, pubKey)
                }
                payload.size == 64 -> {
                    val random = payload.copyOfRange(0, 16)
                    val pubKey = payload.copyOfRange(16, 64)
                    Pair(random, pubKey)
                }
                else -> {
                    throw IllegalArgumentException("Invalid challenge payload size: ${payload.size}")
                }
            }

            val sharedEC = EcdhB163.generateSharedSecret(privateKey, remotePublicKey)
                ?: throw IllegalArgumentException("Failed to generate ECDH shared secret with remote public key")

            // Sequence number: little-endian uint32 from sharedEC[0..3]
            val sequenceNumber = (sharedEC[0].toInt() and 0xFF) or
                    ((sharedEC[1].toInt() and 0xFF) shl 8) or
                    ((sharedEC[2].toInt() and 0xFF) shl 16) or
                    ((sharedEC[3].toInt() and 0xFF) shl 24)

            // Session AES key: sharedEC[8..23] XOR authKey[0..15]
            val sessionKey = ByteArray(16)
            for (i in 0 until 16) {
                sessionKey[i] = (sharedEC[i + 8].toInt() xor authKey[i].toInt()).toByte()
            }

            // Double AES encryption:
            // encryptedRandom1 = AES_ECB(remoteRandom, authKey)
            // encryptedRandom2 = AES_ECB(remoteRandom, sessionKey)
            val encryptedRandom1 = encryptAes128Ecb(remoteRandom, authKey)
            val encryptedRandom2 = encryptAes128Ecb(remoteRandom, sessionKey)

            val command = ByteArray(33)
            command[0] = CMD_SESSION_KEY
            System.arraycopy(encryptedRandom1, 0, command, 1, 16)
            System.arraycopy(encryptedRandom2, 0, command, 17, 16)

            return AuthChallengeResult(
                responseCommand = command,
                sessionKey = sessionKey,
                sequenceNumber = sequenceNumber
            )
        }

        fun handleServerChallenge(
            privateKey: ByteArray,
            authKey: ByteArray,
            payload: ByteArray
        ): AuthChallengeResult = processChallenge(privateKey, authKey, payload)

        /**
         * Verifies watch's response to the session key command.
         * Validates [0x10, 0x05, 0x01] -> SUCCESS, [0x10, 0x05, 0x25] -> WRONG_KEY, etc.
         * Also supports unpadded [0x05, 0x01] / [0x05, 0x25].
         */
        fun verifyResponse(payload: ByteArray): AuthResultStatus {
            if (payload.size >= 3 && payload[0] == RESPONSE_BYTE && payload[1] == CMD_SESSION_KEY) {
                return when (payload[2]) {
                    STATUS_SUCCESS -> AuthResultStatus.SUCCESS
                    STATUS_WRONG_KEY -> AuthResultStatus.WRONG_KEY
                    else -> AuthResultStatus.ERROR
                }
            }
            if (payload.size >= 2 && payload[0] == CMD_SESSION_KEY) {
                return when (payload[1]) {
                    STATUS_SUCCESS -> AuthResultStatus.SUCCESS
                    STATUS_WRONG_KEY -> AuthResultStatus.WRONG_KEY
                    else -> AuthResultStatus.ERROR
                }
            }
            return AuthResultStatus.ERROR
        }

        fun handleServerResponse(payload: ByteArray): AuthResultStatus = verifyResponse(payload)

        /**
         * Encrypts data using AES-128 in ECB mode without padding.
         */
        fun encryptAes128Ecb(data: ByteArray, key: ByteArray): ByteArray {
            require(key.size == 16) { "AES-128 key must be 16 bytes, got ${key.size}" }
            require(data.size % 16 == 0) { "Data length must be a multiple of 16 bytes, got ${data.size}" }
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            return cipher.doFinal(data)
        }

        /**
         * Parses a 32-character hexadecimal string (optionally prefixed with "0x") into a 16-byte array.
         */
        fun parseAuthKey(hexString: String): ByteArray {
            val clean = hexString.trim().removePrefix("0x").removePrefix("0X")
            require(clean.length == 32) {
                "Auth key hex string must be 32 characters (16 bytes), got ${clean.length} in '$hexString'"
            }
            return ByteArray(16) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
