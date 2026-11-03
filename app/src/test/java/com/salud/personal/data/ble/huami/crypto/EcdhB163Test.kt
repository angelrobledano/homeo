package com.salud.personal.data.ble.huami.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EcdhB163Test {

    @Test
    fun generatePrivateKey_returns24Bytes() {
        val privateKey = EcdhB163.generatePrivateKey()
        assertEquals(24, privateKey.size)
        // Verify it's not all zeros
        assertTrue(privateKey.any { it != 0.toByte() })
    }

    @Test
    fun generatePublicKey_returns48Bytes() {
        val privateKey = EcdhB163.generatePrivateKey()
        val publicKey = EcdhB163.generatePublicKey(privateKey)
        assertEquals(48, publicKey.size)
        // Public key is composed of two 24-byte coordinates (X, Y)
        assertTrue(publicKey.any { it != 0.toByte() })
    }

    @Test
    fun diffieHellman_aliceAndBobComputeIdenticalSharedSecrets() {
        // Alice generates keypair
        val alicePrivate = EcdhB163.generatePrivateKey()
        val alicePublic = EcdhB163.generatePublicKey(alicePrivate)

        // Bob generates keypair
        val bobPrivate = EcdhB163.generatePrivateKey()
        val bobPublic = EcdhB163.generatePublicKey(bobPrivate)

        // Key exchange
        val aliceShared = EcdhB163.generateSharedSecret(alicePrivate, bobPublic)
        val bobShared = EcdhB163.generateSharedSecret(bobPrivate, alicePublic)

        assertNotNull("Alice shared secret must not be null", aliceShared)
        assertNotNull("Bob shared secret must not be null", bobShared)

        assertEquals("Shared secret must be 24 bytes", 24, aliceShared!!.size)
        assertEquals("Shared secret must be 24 bytes", 24, bobShared!!.size)

        assertArrayEquals(
            "Alice and Bob must compute the exact same shared secret",
            aliceShared,
            bobShared
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun generatePublicKey_withInvalidPrivateKeySize_throwsException() {
        EcdhB163.generatePublicKey(ByteArray(23))
    }

    @Test
    fun generateSharedSecret_withInvalidPrivateKeySize_returnsNull() {
        val remotePublic = ByteArray(48) { 1 }
        val shared = EcdhB163.generateSharedSecret(ByteArray(20), remotePublic)
        assertNull(shared)
    }

    @Test
    fun generateSharedSecret_withInvalidPublicKeySize_returnsNull() {
        val privateKey = EcdhB163.generatePrivateKey()
        val shared = EcdhB163.generateSharedSecret(privateKey, ByteArray(47))
        assertNull(shared)
    }

    @Test
    fun generateSharedSecret_withZeroPointPublicKey_returnsNull() {
        val privateKey = EcdhB163.generatePrivateKey()
        val zeroPublic = ByteArray(48)
        val shared = EcdhB163.generateSharedSecret(privateKey, zeroPublic)
        assertNull(shared)
    }

    @Test
    fun generateSharedSecret_withPointNotOnCurve_returnsNull() {
        val privateKey = EcdhB163.generatePrivateKey()
        // Random bytes that do not satisfy curve equation y^2 + x*y = x^3 + a*x^2 + b
        val invalidPublic = ByteArray(48) { 0x55.toByte() }
        val shared = EcdhB163.generateSharedSecret(privateKey, invalidPublic)
        assertNull(shared)
    }
}
