package com.salud.personal.data.ble.huami.crypto

import java.security.SecureRandom

/**
 * Pure Kotlin implementation of NIST B-163 binary field Elliptic-Curve Diffie-Hellman (ECDH).
 * Ported faithfully from tiny-ECDH-c / Gadgetbridge ECDH_B163.
 */
object EcdhB163 {

    const val CURVE_DEGREE = 163
    const val ECC_PRV_KEY_SIZE = 24
    const val ECC_PUB_KEY_SIZE = 2 * ECC_PRV_KEY_SIZE // 48

    /* Margin for overhead needed in intermediate calculations */
    private const val BITVEC_MARGIN = 3
    private const val BITVEC_NBITS = CURVE_DEGREE + BITVEC_MARGIN
    private const val BITVEC_NWORDS = (BITVEC_NBITS + 31) / 32 // 6
    private const val BITVEC_NBYTES = 4 * BITVEC_NWORDS // 24

    /* NIST B-163 curve parameters */
    private val polynomial = intArrayOf(
        0x000000c9, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000008
    )
    private val coeffB = intArrayOf(
        0x4a3205fd, 0x512f7874, 0x1481eb10, 0xb8c953caL.toInt(), 0x0a601907, 0x00000002
    )
    private val baseX = intArrayOf(
        0xe8343e36L.toInt(), 0xd4994637L.toInt(), 0xa0991168L.toInt(), 0x86a2d57eL.toInt(), 0xf0eba162L.toInt(), 0x00000003
    )
    private val baseY = intArrayOf(
        0x797324f1, 0xb11c5c0cL.toInt(), 0xa2cdd545L.toInt(), 0x71a0094f, 0xd51fbc6cL.toInt(), 0x00000000
    )
    private val baseOrder = intArrayOf(
        0xa4234c33L.toInt(), 0x77e70c12, 0x000292fe, 0x00000000, 0x00000000, 0x00000004
    )

    private fun bitvecGetBit(x: IntArray, idx: Int): Int {
        return (x[idx / 32] ushr (idx and 31)) and 1
    }

    private fun bitvecClrBit(x: IntArray, idx: Int) {
        x[idx / 32] = x[idx / 32] and (1 shl (idx and 31)).inv()
    }

    private fun bitvecCopy(x: IntArray, y: IntArray) {
        for (i in 0 until BITVEC_NWORDS) {
            x[i] = y[i]
        }
    }

    private fun bitvecSwap(x: IntArray, y: IntArray) {
        val tmp = IntArray(BITVEC_NWORDS)
        bitvecCopy(tmp, x)
        bitvecCopy(x, y)
        bitvecCopy(y, tmp)
    }

    private fun bitvecEqual(x: IntArray, y: IntArray): Boolean {
        for (i in 0 until BITVEC_NWORDS) {
            if (x[i] != y[i]) return false
        }
        return true
    }

    private fun bitvecSetZero(x: IntArray) {
        for (i in 0 until BITVEC_NWORDS) {
            x[i] = 0
        }
    }

    private fun bitvecIsZero(x: IntArray): Boolean {
        for (i in 0 until BITVEC_NWORDS) {
            if (x[i] != 0) return false
        }
        return true
    }

    private fun bitvecDegree(x: IntArray): Int {
        var i = BITVEC_NWORDS * 32
        var y = BITVEC_NWORDS

        while (i > 0 && x[--y] == 0) {
            i -= 32
        }

        if (i != 0) {
            var u32mask = 1 shl 31
            while ((x[y] and u32mask) == 0) {
                u32mask = u32mask ushr 1
                i -= 1
            }
        }
        return i
    }

    private fun bitvecLshift(x: IntArray, y: IntArray, nbitsArg: Int) {
        val ySrc = if (x === y && (nbitsArg / 32) > 0) y.copyOf() else y
        val nwords = nbitsArg / 32
        var i = 0
        while (i < nwords) {
            x[i] = 0
            i++
        }
        var j = 0
        while (i < BITVEC_NWORDS) {
            x[i] = ySrc[j]
            i++
            j++
        }

        val remBits = nbitsArg and 31
        if (remBits != 0) {
            for (k in (BITVEC_NWORDS - 1) downTo 1) {
                x[k] = (x[k] shl remBits) or (x[k - 1] ushr (32 - remBits))
            }
            x[0] = x[0] shl remBits
        }
    }

    private fun gf2fieldSetOne(x: IntArray) {
        x[0] = 1
        for (i in 1 until BITVEC_NWORDS) {
            x[i] = 0
        }
    }

    private fun gf2fieldIsOne(x: IntArray): Boolean {
        if (x[0] != 1) return false
        for (i in 1 until BITVEC_NWORDS) {
            if (x[i] != 0) return false
        }
        return true
    }

    private fun gf2fieldAdd(z: IntArray, x: IntArray, y: IntArray) {
        for (i in 0 until BITVEC_NWORDS) {
            z[i] = x[i] xor y[i]
        }
    }

    private fun gf2fieldInc(x: IntArray) {
        x[0] = x[0] xor 1
    }

    private fun gf2fieldMul(z: IntArray, x: IntArray, y: IntArray) {
        val tmp = IntArray(BITVEC_NWORDS)
        assert(z !== y)

        bitvecCopy(tmp, x)

        if (bitvecGetBit(y, 0) != 0) {
            bitvecCopy(z, x)
        } else {
            bitvecSetZero(z)
        }

        for (i in 1 until CURVE_DEGREE) {
            bitvecLshift(tmp, tmp, 1)

            if (bitvecGetBit(tmp, CURVE_DEGREE) != 0) {
                gf2fieldAdd(tmp, tmp, polynomial)
            }

            if (bitvecGetBit(y, i) != 0) {
                gf2fieldAdd(z, z, tmp)
            }
        }
    }

    private fun gf2fieldInv(z: IntArray, x: IntArray) {
        val u = IntArray(BITVEC_NWORDS)
        val v = IntArray(BITVEC_NWORDS)
        val g = IntArray(BITVEC_NWORDS)
        val h = IntArray(BITVEC_NWORDS)

        bitvecCopy(u, x)
        bitvecCopy(v, polynomial)
        bitvecSetZero(g)
        gf2fieldSetOne(z)

        while (!gf2fieldIsOne(u)) {
            var i = bitvecDegree(u) - bitvecDegree(v)

            if (i < 0) {
                bitvecSwap(u, v)
                bitvecSwap(g, z)
                i = -i
            }
            bitvecLshift(h, v, i)
            gf2fieldAdd(u, u, h)
            bitvecLshift(h, g, i)
            gf2fieldAdd(z, z, h)
        }
    }

    private fun gf2pointCopy(x1: IntArray, y1: IntArray, x2: IntArray, y2: IntArray) {
        bitvecCopy(x1, x2)
        bitvecCopy(y1, y2)
    }

    private fun gf2pointSetZero(x: IntArray, y: IntArray) {
        bitvecSetZero(x)
        bitvecSetZero(y)
    }

    private fun gf2pointIsZero(x: IntArray, y: IntArray): Boolean {
        return bitvecIsZero(x) && bitvecIsZero(y)
    }

    private fun gf2pointDouble(x: IntArray, y: IntArray) {
        if (bitvecIsZero(x)) {
            bitvecSetZero(y)
        } else {
            val l = IntArray(BITVEC_NWORDS)
            gf2fieldInv(l, x)
            gf2fieldMul(l, l, y)
            gf2fieldAdd(l, l, x)
            gf2fieldMul(y, x, x)
            gf2fieldMul(x, l, l)
            gf2fieldInc(l)
            gf2fieldAdd(x, x, l)
            gf2fieldMul(l, l, x)
            gf2fieldAdd(y, y, l)
        }
    }

    private fun gf2pointAdd(x1: IntArray, y1: IntArray, x2: IntArray, y2: IntArray) {
        if (!gf2pointIsZero(x2, y2)) {
            if (gf2pointIsZero(x1, y1)) {
                gf2pointCopy(x1, y1, x2, y2)
            } else {
                if (bitvecEqual(x1, x2)) {
                    if (bitvecEqual(y1, y2)) {
                        gf2pointDouble(x1, y1)
                    } else {
                        gf2pointSetZero(x1, y1)
                    }
                } else {
                    val a = IntArray(BITVEC_NWORDS)
                    val b = IntArray(BITVEC_NWORDS)
                    val c = IntArray(BITVEC_NWORDS)
                    val d = IntArray(BITVEC_NWORDS)

                    gf2fieldAdd(a, y1, y2)
                    gf2fieldAdd(b, x1, x2)
                    gf2fieldInv(c, b)
                    gf2fieldMul(c, c, a)
                    gf2fieldMul(d, c, c)
                    gf2fieldAdd(d, d, c)
                    gf2fieldAdd(d, d, b)
                    gf2fieldInc(d)
                    gf2fieldAdd(x1, x1, d)
                    gf2fieldMul(a, x1, c)
                    gf2fieldAdd(a, a, d)
                    gf2fieldAdd(y1, y1, a)
                    bitvecCopy(x1, d)
                }
            }
        }
    }

    private fun gf2pointMul(x: IntArray, y: IntArray, exp: IntArray) {
        val tmpx = IntArray(BITVEC_NWORDS)
        val tmpy = IntArray(BITVEC_NWORDS)

        val nbits = bitvecDegree(exp)
        gf2pointSetZero(tmpx, tmpy)

        for (i in (nbits - 1) downTo 0) {
            gf2pointDouble(tmpx, tmpy)

            if (bitvecGetBit(exp, i) != 0) {
                gf2pointAdd(tmpx, tmpy, x, y)
            }
        }

        gf2pointCopy(x, y, tmpx, tmpy)
    }

    private fun gf2pointOnCurve(x: IntArray, y: IntArray): Boolean {
        val a = IntArray(BITVEC_NWORDS)
        val b = IntArray(BITVEC_NWORDS)

        if (gf2pointIsZero(x, y)) {
            return false
        } else {
            gf2fieldMul(a, x, x)
            gf2fieldMul(b, a, x)
            gf2fieldAdd(a, a, b)
            gf2fieldAdd(a, a, coeffB)
            gf2fieldMul(b, y, y)
            gf2fieldAdd(a, a, b)
            gf2fieldMul(b, x, y)

            return bitvecEqual(a, b)
        }
    }

    private fun bytesToInt(bytes: ByteArray, offset: Int): IntArray {
        val value = IntArray(BITVEC_NWORDS)
        var byteptr = offset
        for (i in 0 until BITVEC_NWORDS) {
            val b0 = bytes[byteptr++].toInt() and 0xFF
            val b1 = bytes[byteptr++].toInt() and 0xFF
            val b2 = bytes[byteptr++].toInt() and 0xFF
            val b3 = bytes[byteptr++].toInt() and 0xFF
            value[i] = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }
        return value
    }

    private fun intsToBytes(bytes: ByteArray, ints: IntArray, offset: Int) {
        var byteptr = offset
        for (i in 0 until BITVEC_NWORDS) {
            bytes[byteptr++] = (ints[i] and 0xFF).toByte()
            bytes[byteptr++] = ((ints[i] ushr 8) and 0xFF).toByte()
            bytes[byteptr++] = ((ints[i] ushr 16) and 0xFF).toByte()
            bytes[byteptr++] = ((ints[i] ushr 24) and 0xFF).toByte()
        }
    }

    private fun ecdhGenerateKeys(publicKey: ByteArray, privateKey: ByteArray): Boolean {
        val privateKeyInt32 = bytesToInt(privateKey, 0)
        val publicKeyInt321 = bytesToInt(publicKey, 0)
        val publicKeyInt322 = bytesToInt(publicKey, BITVEC_NBYTES)

        gf2pointCopy(publicKeyInt321, publicKeyInt322, baseX, baseY)

        if (bitvecDegree(privateKeyInt32) < (CURVE_DEGREE / 2)) {
            return false
        }

        val nbits = bitvecDegree(baseOrder)
        for (i in (nbits - 1) until (BITVEC_NWORDS * 32)) {
            bitvecClrBit(privateKeyInt32, i)
        }

        gf2pointMul(publicKeyInt321, publicKeyInt322, privateKeyInt32)

        intsToBytes(publicKey, publicKeyInt321, 0)
        intsToBytes(publicKey, publicKeyInt322, BITVEC_NBYTES)
        return true
    }

    private fun ecdhSharedSecret(privateKey: ByteArray, othersPub: ByteArray, output: ByteArray): Boolean {
        val privateKeyInt32 = bytesToInt(privateKey, 0)
        val othersPubInt321 = bytesToInt(othersPub, 0)
        val othersPubInt322 = bytesToInt(othersPub, BITVEC_NBYTES)

        if (!gf2pointIsZero(othersPubInt321, othersPubInt322) &&
            gf2pointOnCurve(othersPubInt321, othersPubInt322)
        ) {
            for (i in 0 until (BITVEC_NBYTES * 2)) {
                output[i] = othersPub[i]
            }

            val nbits = bitvecDegree(baseOrder)
            for (i in (nbits - 1) until (BITVEC_NWORDS * 32)) {
                bitvecClrBit(privateKeyInt32, i)
            }

            val outputInt321 = bytesToInt(output, 0)
            val outputInt322 = bytesToInt(output, BITVEC_NBYTES)

            gf2pointMul(outputInt321, outputInt322, privateKeyInt32)

            intsToBytes(output, outputInt321, 0)
            intsToBytes(output, outputInt322, BITVEC_NBYTES)
            return true
        } else {
            return false
        }
    }

    /**
     * Generates a 24-byte cryptographically secure random private key.
     */
    fun generatePrivateKey(random: SecureRandom = SecureRandom()): ByteArray {
        val key = ByteArray(ECC_PRV_KEY_SIZE)
        while (true) {
            random.nextBytes(key)
            val keyInts = bytesToInt(key, 0)
            if (bitvecDegree(keyInts) >= (CURVE_DEGREE / 2)) {
                break
            }
        }
        return key
    }

    /**
     * Derives a 48-byte public key (x: 24 bytes, y: 24 bytes) from a 24-byte private key.
     * Throws [IllegalArgumentException] if private key is invalid.
     */
    fun generatePublicKey(privateKey: ByteArray): ByteArray {
        require(privateKey.size == ECC_PRV_KEY_SIZE) {
            "Private key must be $ECC_PRV_KEY_SIZE bytes, got ${privateKey.size}"
        }
        val publicKey = ByteArray(ECC_PUB_KEY_SIZE)
        val success = ecdhGenerateKeys(publicKey, privateKey)
        if (!success) {
            throw IllegalArgumentException("Invalid private key: degree too low for NIST B-163")
        }
        return publicKey
    }

    /**
     * Computes the 24-byte shared secret (x-coordinate of DH point) from own private key
     * and remote 48-byte public key.
     * Returns null if key sizes are invalid or remote public key is not a valid curve point.
     */
    fun generateSharedSecret(privateKey: ByteArray, remotePublicKey: ByteArray): ByteArray? {
        if (privateKey.size != ECC_PRV_KEY_SIZE || remotePublicKey.size != ECC_PUB_KEY_SIZE) {
            return null
        }
        val output = ByteArray(ECC_PUB_KEY_SIZE)
        val success = ecdhSharedSecret(privateKey, remotePublicKey, output)
        if (!success) {
            return null
        }
        return output.copyOfRange(0, ECC_PRV_KEY_SIZE)
    }
}
