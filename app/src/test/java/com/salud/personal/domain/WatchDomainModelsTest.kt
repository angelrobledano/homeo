package com.salud.personal.domain

import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WatchDomainModelsTest {

    @Test
    fun heartRateSample_validData_createsInstance() {
        val now = Instant.now()
        val sample = HeartRateSample(
            timestamp = now,
            bpm = 72,
            sourceDevice = "Amazfit GTR 3"
        )
        assertEquals(now, sample.timestamp)
        assertEquals(72, sample.bpm)
        assertEquals("Amazfit GTR 3", sample.sourceDevice)
    }

    @Test(expected = IllegalArgumentException::class)
    fun heartRateSample_bpmTooLow_throwsException() {
        HeartRateSample(timestamp = Instant.now(), bpm = 15, sourceDevice = "Amazfit GTR 3")
    }

    @Test(expected = IllegalArgumentException::class)
    fun heartRateSample_bpmTooHigh_throwsException() {
        HeartRateSample(timestamp = Instant.now(), bpm = 350, sourceDevice = "Amazfit GTR 3")
    }

    @Test(expected = IllegalArgumentException::class)
    fun heartRateSample_blankSourceDevice_throwsException() {
        HeartRateSample(timestamp = Instant.now(), bpm = 70, sourceDevice = "   ")
    }

    @Test
    fun dailyActivitySummary_validData_createsInstance() {
        val date = LocalDate.of(2026, 10, 27)
        val activity = DailyActivitySummary(
            date = date,
            steps = 10500,
            distanceMeters = 8100,
            caloriesBurned = 520,
            sourceDevice = "Amazfit GTR 3"
        )
        assertEquals(date, activity.date)
        assertEquals(10500, activity.steps)
        assertEquals(8100, activity.distanceMeters)
        assertEquals(520, activity.caloriesBurned)
        assertEquals("Amazfit GTR 3", activity.sourceDevice)
    }

    @Test(expected = IllegalArgumentException::class)
    fun dailyActivitySummary_negativeSteps_throwsException() {
        DailyActivitySummary(
            date = LocalDate.now(),
            steps = -1,
            distanceMeters = 100,
            caloriesBurned = 50,
            sourceDevice = "Amazfit GTR 3"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun dailyActivitySummary_negativeDistance_throwsException() {
        DailyActivitySummary(
            date = LocalDate.now(),
            steps = 1000,
            distanceMeters = -5,
            caloriesBurned = 50,
            sourceDevice = "Amazfit GTR 3"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun dailyActivitySummary_negativeCalories_throwsException() {
        DailyActivitySummary(
            date = LocalDate.now(),
            steps = 1000,
            distanceMeters = 100,
            caloriesBurned = -10,
            sourceDevice = "Amazfit GTR 3"
        )
    }

    @Test
    fun watchBatteryStatus_validData_createsInstance() {
        val now = Instant.now()
        val battery = WatchBatteryStatus(
            percentage = 85,
            isCharging = false,
            timestamp = now
        )
        assertEquals(85, battery.percentage)
        assertFalse(battery.isCharging)
        assertEquals(now, battery.timestamp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun watchBatteryStatus_percentageBelowZero_throwsException() {
        WatchBatteryStatus(percentage = -1, isCharging = false, timestamp = Instant.now())
    }

    @Test(expected = IllegalArgumentException::class)
    fun watchBatteryStatus_percentageAbove100_throwsException() {
        WatchBatteryStatus(percentage = 101, isCharging = true, timestamp = Instant.now())
    }

    @Test
    fun deviceConnectionState_allStatesAvailable() {
        val states = DeviceConnectionState.entries
        assertTrue(states.contains(DeviceConnectionState.DISCONNECTED))
        assertTrue(states.contains(DeviceConnectionState.SCANNING))
        assertTrue(states.contains(DeviceConnectionState.CONNECTING))
        assertTrue(states.contains(DeviceConnectionState.AUTHENTICATING))
        assertTrue(states.contains(DeviceConnectionState.CONNECTED))
        assertTrue(states.contains(DeviceConnectionState.AUTH_FAILED))
    }
}
