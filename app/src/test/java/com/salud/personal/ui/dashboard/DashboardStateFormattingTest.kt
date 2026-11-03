package com.salud.personal.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class DashboardStateFormattingTest {

    @Test
    fun formatsWeightWithTwoDecimalsAndUnit() {
        assertEquals("74.80 kg", DashboardFormatting.formatWeight(74.8))
        assertEquals("80.00 kg", DashboardFormatting.formatWeight(80.0))
        assertEquals("-- kg", DashboardFormatting.formatWeight(null))
    }

    @Test
    fun formatsDeltaWithHonestSign() {
        assertEquals("-0.45 kg", DashboardFormatting.formatDelta(-0.45))
        assertEquals("+0.30 kg", DashboardFormatting.formatDelta(0.30))
        assertEquals("0.00 kg", DashboardFormatting.formatDelta(0.0))
        assertEquals("-- kg", DashboardFormatting.formatDelta(null))
    }

    @Test
    fun formatsPercentage() {
        assertEquals("18.2%", DashboardFormatting.formatPercentage(18.2f))
        assertEquals("--%", DashboardFormatting.formatPercentage(null))
    }

    @Test
    fun formatsTimestampCorrectly() {
        val instant = Instant.parse("2026-10-27T10:45:00Z")
        val formatted = DashboardFormatting.formatTimestamp(instant, ZoneOffset.UTC)
        assertTrue("Formatted date should contain 2026", formatted.contains("2026"))
        assertTrue("Formatted date should contain 10:45", formatted.contains("10:45"))
        assertEquals("--", DashboardFormatting.formatTimestamp(null))
    }

    @Test
    fun formatsStepsWithGrouping() {
        assertEquals("8.420", DashboardFormatting.formatSteps(8420))
        assertEquals("0", DashboardFormatting.formatSteps(0))
        assertEquals("--", DashboardFormatting.formatSteps(null))
    }

    @Test
    fun formatsDistanceInKilometers() {
        assertEquals("5.20 km", DashboardFormatting.formatDistance(5200))
        assertEquals("0.45 km", DashboardFormatting.formatDistance(450))
        assertEquals("-- km", DashboardFormatting.formatDistance(null))
    }

    @Test
    fun formatsCalories() {
        assertEquals("350 kcal", DashboardFormatting.formatCalories(350))
        assertEquals("-- kcal", DashboardFormatting.formatCalories(null))
    }

    @Test
    fun formatsBpm() {
        assertEquals("72 bpm", DashboardFormatting.formatBpm(72))
        assertEquals("-- bpm", DashboardFormatting.formatBpm(null))
    }

    @Test
    fun formatsTimeOnly() {
        val instant = Instant.parse("2026-10-27T14:30:00Z")
        assertEquals("14:30", DashboardFormatting.formatTime(instant, ZoneOffset.UTC))
        assertEquals("--:--", DashboardFormatting.formatTime(null))
    }
}
