package com.salud.personal.domain

import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class WeightMeasurementTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")

    @Test
    fun validMeasurement_withAllFields_isCreatedSuccessfully() {
        val measurement = WeightMeasurement(
            id = 1L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            externalId = "ext-12345",
            weightKg = 74.85,
            bodyFatPercentage = 18.2f,
            muscleMassKg = 58.1,
            impedanceOhms = 495.0,
            measuredAt = now,
            importedAt = now
        )

        assertEquals(1L, measurement.id)
        assertEquals(ProvenanceSource.RENPHO_ELIS_1, measurement.source)
        assertEquals("ext-12345", measurement.externalId)
        assertEquals(74.85, measurement.weightKg, 0.001)
        assertEquals(18.2f, measurement.bodyFatPercentage ?: 0f, 0.001f)
        assertEquals(58.1, measurement.muscleMassKg ?: 0.0, 0.001)
        assertEquals(495.0, measurement.impedanceOhms ?: 0.0, 0.001)
        assertEquals(now, measurement.measuredAt)
        assertEquals(now, measurement.importedAt)
    }

    @Test
    fun validMeasurement_withMinimalFields_isCreatedSuccessfully() {
        val measurement = WeightMeasurement(
            source = ProvenanceSource.MANUAL_INPUT,
            weightKg = 70.0,
            measuredAt = now,
            importedAt = now
        )

        assertEquals(0L, measurement.id)
        assertEquals(ProvenanceSource.MANUAL_INPUT, measurement.source)
        assertNull(measurement.externalId)
        assertEquals(70.0, measurement.weightKg, 0.001)
        assertNull(measurement.bodyFatPercentage)
        assertNull(measurement.muscleMassKg)
        assertNull(measurement.impedanceOhms)
    }

    @Test
    fun validMeasurement_atPhysiologicalBoundaries_isCreatedSuccessfully() {
        val minBoundary = WeightMeasurement(
            source = ProvenanceSource.HEALTH_CONNECT,
            weightKg = 10.0,
            bodyFatPercentage = 3.0f,
            muscleMassKg = 5.0,
            measuredAt = now,
            importedAt = now
        )
        assertEquals(10.0, minBoundary.weightKg, 0.001)
        assertEquals(3.0f, minBoundary.bodyFatPercentage ?: 0f, 0.001f)
        assertEquals(5.0, minBoundary.muscleMassKg ?: 0.0, 0.001)

        val maxBoundary = WeightMeasurement(
            source = ProvenanceSource.AMAZFIT_GTR_3,
            weightKg = 300.0,
            bodyFatPercentage = 70.0f,
            muscleMassKg = 300.0,
            measuredAt = now,
            importedAt = now
        )
        assertEquals(300.0, maxBoundary.weightKg, 0.001)
        assertEquals(70.0f, maxBoundary.bodyFatPercentage ?: 0f, 0.001f)
        assertEquals(300.0, maxBoundary.muscleMassKg ?: 0.0, 0.001)
    }

    @Test
    fun weightBelowMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.MANUAL_INPUT,
                weightKg = 9.99,
                measuredAt = now,
                importedAt = now
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.MANUAL_INPUT,
                weightKg = -5.0,
                measuredAt = now,
                importedAt = now
            )
        }
    }

    @Test
    fun weightAboveMaximum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.MANUAL_INPUT,
                weightKg = 300.01,
                measuredAt = now,
                importedAt = now
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.MANUAL_INPUT,
                weightKg = 500.0,
                measuredAt = now,
                importedAt = now
            )
        }
    }

    @Test
    fun bodyFatBelowMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.RENPHO_ELIS_1,
                weightKg = 70.0,
                bodyFatPercentage = 2.9f,
                measuredAt = now,
                importedAt = now
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.RENPHO_ELIS_1,
                weightKg = 70.0,
                bodyFatPercentage = -1.0f,
                measuredAt = now,
                importedAt = now
            )
        }
    }

    @Test
    fun bodyFatAboveMaximum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.RENPHO_ELIS_1,
                weightKg = 70.0,
                bodyFatPercentage = 70.1f,
                measuredAt = now,
                importedAt = now
            )
        }
    }

    @Test
    fun muscleMassBelowMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.RENPHO_ELIS_1,
                weightKg = 70.0,
                muscleMassKg = 4.9,
                measuredAt = now,
                importedAt = now
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.RENPHO_ELIS_1,
                weightKg = 70.0,
                muscleMassKg = -1.0,
                measuredAt = now,
                importedAt = now
            )
        }
    }

    @Test
    fun muscleMassExceedingTotalWeight_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeightMeasurement(
                source = ProvenanceSource.RENPHO_ELIS_1,
                weightKg = 70.0,
                muscleMassKg = 70.1,
                measuredAt = now,
                importedAt = now
            )
        }
    }

    @Test
    fun provenanceSource_hasExpectedDisplayNames() {
        assertEquals("RENPHO Elis 1", ProvenanceSource.RENPHO_ELIS_1.displayName)
        assertEquals("Amazfit GTR 3", ProvenanceSource.AMAZFIT_GTR_3.displayName)
        assertEquals("Manual", ProvenanceSource.MANUAL_INPUT.displayName)
        assertEquals("Health Connect", ProvenanceSource.HEALTH_CONNECT.displayName)
    }
}
