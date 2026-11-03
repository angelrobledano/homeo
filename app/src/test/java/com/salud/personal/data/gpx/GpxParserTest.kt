package com.salud.personal.data.gpx

import com.salud.personal.domain.gps.GpsLocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class GpxParserTest {

    @Test
    fun exportAndParseGpx_roundTrip_preservesCoordinates() {
        val original = listOf(
            GpsLocationPoint(40.4168, -3.7038, 650.0),
            GpsLocationPoint(40.4154, -3.7074, 648.0)
        )

        val gpxXml = GpxParser.exportToGpx("Caminata Madrid Centro", original)
        assertTrue(gpxXml.contains("Caminata Madrid Centro"))
        assertTrue(gpxXml.contains("""lat="40.4168""""))

        val parsed = GpxParser.parseGpx(ByteArrayInputStream(gpxXml.toByteArray()))
        assertEquals(2, parsed.size)
        assertEquals(40.4168, parsed[0].latitude, 0.0001)
        assertEquals(-3.7038, parsed[0].longitude, 0.0001)
    }
}
