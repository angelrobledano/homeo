package com.salud.personal.data.ble.huami.auth

import com.salud.personal.data.ble.huami.crypto.EcdhB163
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Huami2021AuthHandshakeTest {

    private fun decryptAes128Ecb(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    @Test
    fun createInitCommand_framingAndLengthAreCorrect() {
        val privateKey = EcdhB163.generatePrivateKey()
        val publicKey = EcdhB163.generatePublicKey(privateKey)

        val cmd = Huami2021AuthHandshake.createInitCommand(privateKey)

        assertEquals("Init command must be 52 bytes", 52, cmd.size)
        assertEquals(0x04.toByte(), cmd[0])
        assertEquals(0x02.toByte(), cmd[1])
        assertEquals(0x00.toByte(), cmd[2])
        assertEquals(0x02.toByte(), cmd[3])

        val extractedPublic = cmd.copyOfRange(4, 52)
        assertArrayEquals("Public key in init command must match derived public key", publicKey, extractedPublic)
    }

    @Test
    fun buildInitCommand_aliasProducesSameOutput() {
        val privateKey = EcdhB163.generatePrivateKey()
        val cmd1 = Huami2021AuthHandshake.createInitCommand(privateKey)
        val cmd2 = Huami2021AuthHandshake.buildInitCommand(privateKey)
        assertArrayEquals(cmd1, cmd2)
    }

    @Test
    fun processChallenge_withSimulatedWatch_producesValidDoubleEncryptedResponse() {
        // App side
        val appPrivateKey = EcdhB163.generatePrivateKey()
        val appPublicKey = EcdhB163.generatePublicKey(appPrivateKey)
        val authKey = Huami2021AuthHandshake.parseAuthKey("0x0102030405060708090a0b0c0d0e0f10")

        // Watch side simulation
        val watchPrivateKey = EcdhB163.generatePrivateKey()
        val watchPublicKey = EcdhB163.generatePublicKey(watchPrivateKey)
        val watchRandom = ByteArray(16) { (it * 7 + 3).toByte() }

        // Watch challenge notification payload: [0x10, 0x04, 0x01] + watchRandom (16) + watchPublicKey (48)
        val payload = ByteArray(67)
        payload[0] = 0x10.toByte() // RESPONSE
        payload[1] = 0x04.toByte() // CMD_PUB_KEY
        payload[2] = 0x01.toByte() // SUCCESS
        System.arraycopy(watchRandom, 0, payload, 3, 16)
        System.arraycopy(watchPublicKey, 0, payload, 19, 48)

        val result = Huami2021AuthHandshake.processChallenge(appPrivateKey, authKey, payload)

        // Validate command structure
        assertEquals("Response command must be 33 bytes", 33, result.responseCommand.size)
        assertEquals("Command prefix must be 0x05", 0x05.toByte(), result.responseCommand[0])
        assertEquals("Session key must be 16 bytes", 16, result.sessionKey.size)

        // Watch independently computes shared secret and validates
        val watchShared = EcdhB163.generateSharedSecret(watchPrivateKey, appPublicKey)
        assertNotNull(watchShared)

        val expectedSeq = (watchShared!![0].toInt() and 0xFF) or
                ((watchShared[1].toInt() and 0xFF) shl 8) or
                ((watchShared[2].toInt() and 0xFF) shl 16) or
                ((watchShared[3].toInt() and 0xFF) shl 24)
        assertEquals(expectedSeq, result.sequenceNumber)

        val expectedSessionKey = ByteArray(16) { i ->
            (watchShared[i + 8].toInt() xor authKey[i].toInt()).toByte()
        }
        assertArrayEquals("Session key must match expected derivation", expectedSessionKey, result.sessionKey)

        // Decrypt enc1 and verify it matches watchRandom
        val enc1 = result.responseCommand.copyOfRange(1, 17)
        val decryptedRandom1 = decryptAes128Ecb(enc1, authKey)
        assertArrayEquals("EncryptedRandom1 must decrypt to original random with authKey", watchRandom, decryptedRandom1)

        // Decrypt enc2 and verify it matches watchRandom
        val enc2 = result.responseCommand.copyOfRange(17, 33)
        val decryptedRandom2 = decryptAes128Ecb(enc2, result.sessionKey)
        assertArrayEquals("EncryptedRandom2 must decrypt to original random with sessionKey", watchRandom, decryptedRandom2)
    }

    @Test
    fun processChallenge_with64BytePayloadWithoutHeader_alsoSucceeds() {
        val appPrivateKey = EcdhB163.generatePrivateKey()
        val authKey = ByteArray(16) { it.toByte() }

        val watchPrivateKey = EcdhB163.generatePrivateKey()
        val watchPublicKey = EcdhB163.generatePublicKey(watchPrivateKey)
        val watchRandom = ByteArray(16) { (it + 5).toByte() }

        // Raw 64 bytes: 16-byte random + 48-byte public key
        val payload = ByteArray(64)
        System.arraycopy(watchRandom, 0, payload, 0, 16)
        System.arraycopy(watchPublicKey, 0, payload, 16, 48)

        val result = Huami2021AuthHandshake.processChallenge(appPrivateKey, authKey, payload)
        assertEquals(33, result.responseCommand.size)
        assertEquals(0x05.toByte(), result.responseCommand[0])
    }

    @Test
    fun verifyResponse_parsesStatusCorrectly() {
        // Standard full packet with 0x10 header
        assertEquals(
            AuthResultStatus.SUCCESS,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x10, 0x05, 0x01))
        )
        assertEquals(
            AuthResultStatus.WRONG_KEY,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x10, 0x05, 0x25))
        )
        assertEquals(
            AuthResultStatus.ERROR,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x10, 0x05, 0x02))
        )

        // Short packet without 0x10
        assertEquals(
            AuthResultStatus.SUCCESS,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x05, 0x01))
        )
        assertEquals(
            AuthResultStatus.WRONG_KEY,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x05, 0x25))
        )
        assertEquals(
            AuthResultStatus.ERROR,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x05, 0x99.toByte()))
        )

        // Empty or invalid packet
        assertEquals(
            AuthResultStatus.ERROR,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf())
        )
        assertEquals(
            AuthResultStatus.ERROR,
            Huami2021AuthHandshake.verifyResponse(byteArrayOf(0x10, 0x04, 0x01))
        )
    }

    @Test
    fun stateMachine_fullAuthFlowLifecycle() {
        val authKey = Huami2021AuthHandshake.parseAuthKey("0x0102030405060708090a0b0c0d0e0f10")
        val handshake = Huami2021AuthHandshake(authKey)

        assertEquals(Huami2021AuthHandshake.State.INITIAL, handshake.state)

        // 1. Start handshake
        val initCmd = handshake.start()
        assertEquals(52, initCmd.size)
        assertEquals(Huami2021AuthHandshake.State.WAITING_CHALLENGE, handshake.state)

        // 2. Watch sends challenge
        val watchPrivateKey = EcdhB163.generatePrivateKey()
        val watchPublicKey = EcdhB163.generatePublicKey(watchPrivateKey)
        val watchRandom = ByteArray(16) { 0x42.toByte() }

        val payload = ByteArray(67)
        payload[0] = 0x10
        payload[1] = 0x04
        payload[2] = 0x01
        System.arraycopy(watchRandom, 0, payload, 3, 16)
        System.arraycopy(watchPublicKey, 0, payload, 19, 48)

        val responseCmd = handshake.handleChallenge(payload)
        assertEquals(33, responseCmd.size)
        assertEquals(Huami2021AuthHandshake.State.WAITING_RESPONSE, handshake.state)
        assertNotNull(handshake.sessionKey)
        assertNotNull(handshake.sequenceNumber)

        // 3. Watch sends success
        val status = handshake.handleResponse(byteArrayOf(0x10, 0x05, 0x01))
        assertEquals(AuthResultStatus.SUCCESS, status)
        assertEquals(Huami2021AuthHandshake.State.AUTHENTICATED, handshake.state)
    }

    @Test
    fun parseAuthKey_validatesAndParsesHex() {
        val keyWith0x = Huami2021AuthHandshake.parseAuthKey("0x0102030405060708090a0b0c0d0e0f10")
        assertEquals(16, keyWith0x.size)
        assertEquals(0x01.toByte(), keyWith0x[0])
        assertEquals(0x10.toByte(), keyWith0x[15])

        val keyWithout0x = Huami2021AuthHandshake.parseAuthKey("0102030405060708090a0b0c0d0e0f10")
        assertArrayEquals(keyWith0x, keyWithout0x)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseAuthKey_withInvalidLength_throwsException() {
        Huami2021AuthHandshake.parseAuthKey("0x1234")
    }
}
