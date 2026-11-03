package com.salud.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SanityCheckTest {

    @Test
    fun environment_isProperlyConfigured() {
        val appName = "SaludPersonal"
        assertEquals("SaludPersonal", appName)
        assertTrue(true)
    }
}
