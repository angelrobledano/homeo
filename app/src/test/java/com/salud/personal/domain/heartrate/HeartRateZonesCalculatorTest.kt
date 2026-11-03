package com.salud.personal.domain.heartrate

import com.salud.personal.data.local.entity.HeartRateSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HeartRateZonesCalculatorTest {

    @Test
    fun testZoneDistributionCalculation() {
        val now = Instant.now()
        val samples = mutableListOf<HeartRateSampleEntity>()
        repeat(10) { samples.add(HeartRateSampleEntity(timestamp = now.minusSeconds(it * 60L).toEpochMilli(), bpm = 70, sourceDevice = "GTR 3")) }
        repeat(5) { samples.add(HeartRateSampleEntity(timestamp = now.minusSeconds((10 + it) * 60L).toEpochMilli(), bpm = 120, sourceDevice = "GTR 3")) }
        repeat(5) { samples.add(HeartRateSampleEntity(timestamp = now.minusSeconds((15 + it) * 60L).toEpochMilli(), bpm = 160, sourceDevice = "GTR 3")) }

        val result = HeartRateZonesCalculator.calculateZones(
            samples = samples,
            maxHr = 190
        )

        assertEquals(70, result.minBpm)
        assertEquals(160, result.maxBpm)
        assertEquals(20, result.totalSamples)

        // Z1: 10/20 = 50%
        assertEquals(50, result.zone1Pct)
        // Z2: 5/20 = 25%
        assertEquals(25, result.zone2Pct)
        // Z4: 5/20 = 25%
        assertEquals(25, result.zone4Pct)
        assertEquals(0, result.zone3Pct)
        assertEquals(0, result.zone5Pct)
    }

    @Test
    fun testEmptySamplesHandledSafely() {
        val result = HeartRateZonesCalculator.calculateZones(
            samples = emptyList(),
            maxHr = 190
        )

        assertEquals(0, result.minBpm)
        assertEquals(0, result.maxBpm)
        assertEquals(0, result.avgBpm)
        assertEquals(0, result.totalSamples)
    }
}
