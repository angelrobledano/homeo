package com.salud.personal.data.ble.huami

import com.salud.personal.data.ble.huami.auth.Huami2021AuthHandshake
import com.salud.personal.data.ble.huami.codec.DecodedMessage
import com.salud.personal.data.ble.huami.crypto.EcdhB163
import com.salud.personal.domain.model.DeviceConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AmazfitGtr3BleManagerTest {

    private lateinit var bleManager: AmazfitGtr3BleManager

    @Before
    fun setUp() {
        bleManager = AmazfitGtr3BleManager(context = null)
    }

    // =========================================================================
    // 1. Standard BLE Heart Rate 0x2A37 Parsing Tests
    // =========================================================================

    @Test
    fun parseHeartRate_8bitFormat_parsesCorrectly() {
        // Flags: 0x00 (bit 0 = 0 -> 8-bit HR), BPM: 78
        val data = byteArrayOf(0x00, 78.toByte())
        val sample = AmazfitGtr3BleManager.parseHeartRate(data, sourceDevice = "Amazfit GTR 3")

        assertNotNull(sample)
        assertEquals(78, sample!!.bpm)
        assertEquals("Amazfit GTR 3", sample.sourceDevice)
    }

    @Test
    fun parseHeartRate_16bitFormat_parsesCorrectly() {
        // Flags: 0x01 (bit 0 = 1 -> 16-bit HR), BPM: 185 (0x00B9 -> LSB: 0xB9, MSB: 0x00)
        val data = byteArrayOf(0x01, 0xB9.toByte(), 0x00.toByte())
        val sample = AmazfitGtr3BleManager.parseHeartRate(data, sourceDevice = "Amazfit GTR 3")

        assertNotNull(sample)
        assertEquals(185, sample!!.bpm)
        assertEquals("Amazfit GTR 3", sample.sourceDevice)
    }

    @Test
    fun parseHeartRate_16bitHighValue_parsesUpTo300Bpm() {
        // 300 bpm: 0x012C -> LSB: 0x2C, MSB: 0x01
        val data = byteArrayOf(0x01, 0x2C.toByte(), 0x01.toByte())
        val sample = AmazfitGtr3BleManager.parseHeartRate(data)

        assertNotNull(sample)
        assertEquals(300, sample!!.bpm)
    }

    @Test
    fun parseHeartRate_validBoundaries_20BpmSucceeds() {
        val data = byteArrayOf(0x00, 20.toByte())
        val sample = AmazfitGtr3BleManager.parseHeartRate(data)

        assertNotNull(sample)
        assertEquals(20, sample!!.bpm)
    }

    @Test
    fun parseHeartRate_outOfRangeLow_returnsNull() {
        val data = byteArrayOf(0x00, 19.toByte())
        val sample = AmazfitGtr3BleManager.parseHeartRate(data)

        assertNull("BPM < 20 must be rejected", sample)
    }

    @Test
    fun parseHeartRate_outOfRangeHigh_returnsNull() {
        // 301 bpm: 0x012D -> LSB: 0x2D, MSB: 0x01
        val data = byteArrayOf(0x01, 0x2D.toByte(), 0x01.toByte())
        val sample = AmazfitGtr3BleManager.parseHeartRate(data)

        assertNull("BPM > 300 must be rejected", sample)
    }

    @Test
    fun parseHeartRate_truncatedPackets_returnsNull() {
        // Empty
        assertNull(AmazfitGtr3BleManager.parseHeartRate(byteArrayOf()))
        // 8-bit flag with no value byte
        assertNull(AmazfitGtr3BleManager.parseHeartRate(byteArrayOf(0x00)))
        // 16-bit flag with only 1 byte (requires 2 value bytes)
        assertNull(AmazfitGtr3BleManager.parseHeartRate(byteArrayOf(0x01, 0x50.toByte())))
    }

    // =========================================================================
    // 2. Standard BLE Battery Level 0x2A19 Parsing Tests
    // =========================================================================

    @Test
    fun parseBatteryLevel_validValues_parsesCorrectly() {
        val status85 = AmazfitGtr3BleManager.parseBatteryLevel(85.toByte())
        assertNotNull(status85)
        assertEquals(85, status85!!.percentage)
        assertFalse(status85.isCharging)

        val status0 = AmazfitGtr3BleManager.parseBatteryLevel(0.toByte())
        assertNotNull(status0)
        assertEquals(0, status0!!.percentage)

        val status100 = AmazfitGtr3BleManager.parseBatteryLevel(100.toByte())
        assertNotNull(status100)
        assertEquals(100, status100!!.percentage)
    }

    @Test
    fun parseBatteryLevel_outOfBounds_returnsNull() {
        val status150 = AmazfitGtr3BleManager.parseBatteryLevel(150.toByte())
        assertNull("Battery level > 100 must be rejected", status150)
    }

    // =========================================================================
    // 3. Huami Battery Payload (Endpoint 0x0010) Parsing Tests
    // =========================================================================

    @Test
    fun parseBatteryPayload_variousFormats_parsesCorrectly() {
        // Single byte
        val singleByte = AmazfitGtr3BleManager.parseBatteryPayload(byteArrayOf(72))
        assertNotNull(singleByte)
        assertEquals(72, singleByte!!.percentage)
        assertFalse(singleByte.isCharging)

        // 2 bytes with charging status
        val charging = AmazfitGtr3BleManager.parseBatteryPayload(byteArrayOf(90, 1))
        assertNotNull(charging)
        assertEquals(90, charging!!.percentage)
        assertTrue(charging.isCharging)

        // 3 bytes prefixed with response byte 0x10
        val prefixed = AmazfitGtr3BleManager.parseBatteryPayload(byteArrayOf(0x10, 65, 0))
        assertNotNull(prefixed)
        assertEquals(65, prefixed!!.percentage)
        assertFalse(prefixed.isCharging)

        // Empty
        assertNull(AmazfitGtr3BleManager.parseBatteryPayload(byteArrayOf()))
    }

    // =========================================================================
    // 4. Huami Activity / Steps Payload (Endpoint 0x0007) Parsing Tests
    // =========================================================================

    @Test
    fun parseActivityPayload_12BytePayload_parsesStepsDistanceCalories() {
        // steps: 8500 (0x2134 -> 0x34, 0x21, 0x00, 0x00)
        // distance: 6100 (0x17D4 -> 0xD4, 0x17, 0x00, 0x00)
        // calories: 320 (0x0140 -> 0x40, 0x01, 0x00, 0x00)
        val payload = byteArrayOf(
            0x34, 0x21, 0x00, 0x00,
            0xD4.toByte(), 0x17, 0x00, 0x00,
            0x40, 0x01, 0x00, 0x00
        )
        val date = LocalDate.of(2026, 10, 27)
        val activity = AmazfitGtr3BleManager.parseActivityPayload(payload, sourceDevice = "Amazfit GTR 3", date = date)

        assertNotNull(activity)
        assertEquals(date, activity!!.date)
        assertEquals(8500, activity.steps)
        assertEquals(6100, activity.distanceMeters)
        assertEquals(320, activity.caloriesBurned)
        assertEquals("Amazfit GTR 3", activity.sourceDevice)
    }

    @Test
    fun parseActivityPayload_4BytePayload_parsesStepsWithZeroDefaults() {
        // steps: 4200 (0x1068 -> 0x68, 0x10, 0x00, 0x00)
        val payload = byteArrayOf(0x68, 0x10, 0x00, 0x00)
        val activity = AmazfitGtr3BleManager.parseActivityPayload(payload)

        assertNotNull(activity)
        assertEquals(4200, activity!!.steps)
        assertEquals(0, activity.distanceMeters)
        assertEquals(0, activity.caloriesBurned)
    }

    @Test
    fun parseActivityPayload_prefixed13BytePayload_parsesCorrectly() {
        val payload = byteArrayOf(
            0x10, // prefix
            0x34, 0x21, 0x00, 0x00, // steps = 8500
            0xD4.toByte(), 0x17, 0x00, 0x00, // distance = 6100
            0x40, 0x01, 0x00, 0x00 // calories = 320
        )
        val activity = AmazfitGtr3BleManager.parseActivityPayload(payload)

        assertNotNull(activity)
        assertEquals(8500, activity!!.steps)
        assertEquals(6100, activity.distanceMeters)
        assertEquals(320, activity.caloriesBurned)
    }

    @Test
    fun parseActivityPayload_truncated_returnsNull() {
        assertNull(AmazfitGtr3BleManager.parseActivityPayload(byteArrayOf(0x01, 0x02)))
        assertNull(AmazfitGtr3BleManager.parseActivityPayload(byteArrayOf()))
    }

    // =========================================================================
    // 5. BLE Dispatch Logic Tests
    // =========================================================================

    @Test
    fun handleCharacteristicData_heartRate_emitsToFlow() = runTest {
        val data = byteArrayOf(0x00, 82.toByte())
        bleManager.handleCharacteristicData(null, AmazfitGtr3BleManager.UUID_CHAR_HEART_RATE_MEASUREMENT, data)

        val sample = bleManager.heartRateSamples.first()
        assertEquals(82, sample.bpm)
        assertEquals("Amazfit GTR 3", sample.sourceDevice)
    }

    @Test
    fun handleCharacteristicData_batteryLevel_updatesBatteryStatus() {
        val data = byteArrayOf(94.toByte())
        bleManager.handleCharacteristicData(null, AmazfitGtr3BleManager.UUID_CHAR_BATTERY_LEVEL, data)

        val battery = bleManager.batteryStatus.value
        assertNotNull(battery)
        assertEquals(94, battery!!.percentage)
        assertFalse(battery.isCharging)
    }

    @Test
    fun dispatchDecodedMessage_batteryEndpoint_updatesBatteryStatus() {
        val payload = byteArrayOf(81.toByte(), 1.toByte())
        val message = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_BATTERY,
            payload = payload,
            handle = 1,
            count = 0
        )
        bleManager.dispatchDecodedMessage(null, message)

        val battery = bleManager.batteryStatus.value
        assertNotNull(battery)
        assertEquals(81, battery!!.percentage)
        assertTrue(battery.isCharging)
    }

    @Test
    fun dispatchDecodedMessage_stepsEndpoint_emitsToDailyActivity() = runTest {
        val payload = byteArrayOf(
            0x68, 0x10, 0x00, 0x00, // 4200 steps
            0x20, 0x0C, 0x00, 0x00, // 3104 distance
            0x96.toByte(), 0x00, 0x00, 0x00 // 150 calories
        )
        val message = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_STEPS,
            payload = payload,
            handle = 2,
            count = 0
        )
        bleManager.dispatchDecodedMessage(null, message)

        val activity = bleManager.dailyActivity.first()
        assertEquals(4200, activity.steps)
        assertEquals(3104, activity.distanceMeters)
        assertEquals(150, activity.caloriesBurned)
    }

    @Test
    fun authHandshake_fullFlow_transitionsToConnectedOnSuccess() {
        val authKeyHex = "0102030405060708090a0b0c0d0e0f10"
        bleManager.startAuthHandshake(null, authKeyHex)

        assertEquals(DeviceConnectionState.AUTHENTICATING, bleManager.connectionState.value)
        assertNotNull(bleManager.handshake)
        assertEquals(Huami2021AuthHandshake.State.WAITING_CHALLENGE, bleManager.handshake!!.state)

        // Simulate challenge payload from watch
        val watchPrivateKey = EcdhB163.generatePrivateKey()
        val watchPublicKey = EcdhB163.generatePublicKey(watchPrivateKey)
        val watchRandom = ByteArray(16) { it.toByte() }

        val challengePayload = ByteArray(67)
        challengePayload[0] = 0x10.toByte() // RESPONSE
        challengePayload[1] = 0x04.toByte() // CMD_PUB_KEY
        challengePayload[2] = 0x01.toByte() // SUCCESS
        System.arraycopy(watchRandom, 0, challengePayload, 3, 16)
        System.arraycopy(watchPublicKey, 0, challengePayload, 19, 48)

        val challengeMessage = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_AUTH,
            payload = challengePayload,
            handle = 1,
            count = 0
        )
        bleManager.dispatchDecodedMessage(null, challengeMessage)

        // Handshake should now be waiting for auth response
        assertEquals(Huami2021AuthHandshake.State.WAITING_RESPONSE, bleManager.handshake!!.state)
        assertNotNull(bleManager.handshake!!.sessionKey)

        // Simulate success response: [0x10, 0x05, 0x01]
        val successPayload = byteArrayOf(0x10, 0x05, 0x01)
        val successMessage = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_AUTH,
            payload = successPayload,
            handle = 2,
            count = 0
        )
        bleManager.dispatchDecodedMessage(null, successMessage)

        // State must transition to CONNECTED
        assertEquals(DeviceConnectionState.CONNECTED, bleManager.connectionState.value)
    }

    @Test
    fun authHandshake_wrongKeyResponse_transitionsToAuthFailed() {
        val authKeyHex = "0102030405060708090a0b0c0d0e0f10"
        bleManager.startAuthHandshake(null, authKeyHex)

        // Simulate challenge
        val watchPrivateKey = EcdhB163.generatePrivateKey()
        val watchPublicKey = EcdhB163.generatePublicKey(watchPrivateKey)
        val watchRandom = ByteArray(16) { it.toByte() }

        val challengePayload = ByteArray(67)
        challengePayload[0] = 0x10.toByte()
        challengePayload[1] = 0x04.toByte()
        challengePayload[2] = 0x01.toByte()
        System.arraycopy(watchRandom, 0, challengePayload, 3, 16)
        System.arraycopy(watchPublicKey, 0, challengePayload, 19, 48)

        bleManager.dispatchDecodedMessage(
            null,
            DecodedMessage(AmazfitGtr3BleManager.ENDPOINT_AUTH, challengePayload, 1, 0)
        )

        // Simulate failure response: [0x10, 0x05, 0x25] (WRONG_KEY)
        val failPayload = byteArrayOf(0x10, 0x05, 0x25)
        bleManager.dispatchDecodedMessage(
            null,
            DecodedMessage(AmazfitGtr3BleManager.ENDPOINT_AUTH, failPayload, 2, 0)
        )

        // State must transition to AUTH_FAILED
        assertEquals(DeviceConnectionState.AUTH_FAILED, bleManager.connectionState.value)
    }

    @Test
    fun parseActivityPayload_endpoint0x16RealHardwareFormat_parsesCorrectly() {
        // Real payload captured from Amazfit GTR 3 hardware on endpoint 0x16:
        // [04 01 0c af 00 00 00 84 00 00 00 0a 00 00 00]
        // Header: [04, 01, 0c]
        // Steps: 175 (0x000000af)
        // Distance: 132 m (0x00000084)
        // Calories: 10 kcal (0x0000000a)
        val payload = byteArrayOf(
            0x04, 0x01, 0x0c,
            0xaf.toByte(), 0x00, 0x00, 0x00,
            0x84.toByte(), 0x00, 0x00, 0x00,
            0x0a.toByte(), 0x00, 0x00, 0x00
        )
        val activity = AmazfitGtr3BleManager.parseActivityPayload(payload, "Amazfit GTR 3", LocalDate.of(2026, 11, 2))

        assertNotNull(activity)
        assertEquals(175, activity!!.steps)
        assertEquals(132, activity.distanceMeters)
        assertEquals(10, activity.caloriesBurned)
        assertEquals("Amazfit GTR 3", activity.sourceDevice)
        assertEquals(LocalDate.of(2026, 11, 2), activity.date)
    }

    @Test
    fun dispatchDecodedMessage_endpoint0x16_emitsDailyActivity() = runTest {
        val payload = byteArrayOf(
            0x04, 0x01, 0x0c,
            0xaf.toByte(), 0x00, 0x00, 0x00,
            0x84.toByte(), 0x00, 0x00, 0x00,
            0x0a.toByte(), 0x00, 0x00, 0x00
        )
        val msg = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_REALTIME_ACTIVITY,
            payload = payload,
            handle = 1,
            count = 0
        )

        bleManager.dispatchDecodedMessage(null, msg)

        val activity = bleManager.dailyActivity.first()
        assertEquals(175, activity.steps)
        assertEquals(132, activity.distanceMeters)
        assertEquals(10, activity.caloriesBurned)
    }

    @Test
    fun parseWorkoutSummaryPayload_crossTraining_parsesCorrectly() {
        // Sport type: 0x07 (CrossFit / Crosstraining)
        // Duration: 1200 seconds (20 min) -> 0x000004B0 (LSB: B0 04 00 00)
        // Avg HR: 155
        // Max HR: 178
        // Calories: 240 kcal -> 0x00F0 (LSB: F0 00)
        val payload = byteArrayOf(
            0x07,
            0xB0.toByte(), 0x04, 0x00, 0x00,
            155.toByte(),
            178.toByte(),
            0xF0.toByte(), 0x00
        )
        val now = Instant.parse("2026-11-02T12:00:00Z")
        val workout = AmazfitGtr3BleManager.parseWorkoutSummaryPayload(payload, now)

        assertNotNull(workout)
        assertEquals("Crosstraining WOD", workout!!.name)
        assertEquals(com.salud.personal.data.local.entity.WorkoutType.CROSSFIT_WOD, workout.type)
        assertEquals(20.0, workout.durationMinutes, 0.1)
        assertEquals(155, workout.avgHeartRate)
        assertEquals(178, workout.maxHeartRate)
        assertEquals(240, workout.caloriesBurned)
        assertTrue(workout.trimp != null && workout.trimp!! > 0.0)
    }

    @Test
    fun dispatchDecodedMessage_endpoint0x15_emitsCompletedWorkout() = runTest {
        val payload = byteArrayOf(
            0x07,
            0xB0.toByte(), 0x04, 0x00, 0x00,
            160.toByte(),
            180.toByte(),
            0xF0.toByte(), 0x00
        )
        val msg = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_WORKOUT_SUMMARY,
            payload = payload,
            handle = 1,
            count = 0
        )

        bleManager.dispatchDecodedMessage(null, msg)

        val workout = bleManager.completedWorkouts.first()
        assertEquals("Crosstraining WOD", workout.name)
        assertEquals(160, workout.avgHeartRate)
    }

    // =========================================================================
    // Sleep Summary Endpoint 0x12 Parsing Tests
    // =========================================================================

    @Test
    fun parseSleepSummaryPayload_validPayload_parsesStagesCorrectly() {
        // Deep: 90 min (0x005A), Light: 210 min (0x00D2), REM: 80 min (0x0050), Awake: 20 min (0x0014)
        val payload = byteArrayOf(
            0x5A, 0x00,
            0xD2.toByte(), 0x00,
            0x50, 0x00,
            0x14, 0x00
        )
        val today = LocalDate.of(2026, 11, 2)
        val sleep = AmazfitGtr3BleManager.parseSleepSummaryPayload(payload, today)

        assertNotNull(sleep)
        assertEquals(today, sleep!!.date)
        assertEquals(90, sleep.deepSleepMinutes)
        assertEquals(210, sleep.lightSleepMinutes)
        assertEquals(80, sleep.remSleepMinutes)
        assertEquals(20, sleep.awakeMinutes)
        assertEquals(380, sleep.totalSleepMinutes) // 90 + 210 + 80 = 380 min (~6.33 h)
        // In bed: 400 min, Efficiency: (380/400)*100 = 95%
        assertEquals(95, sleep.sleepEfficiencyScore)
        assertEquals("Amazfit GTR 3", sleep.sourceDevice)
    }

    @Test
    fun parseSleepSummaryPayload_tooShortPayload_returnsNull() {
        val payload = byteArrayOf(0x5A, 0x00, 0xD2.toByte())
        val sleep = AmazfitGtr3BleManager.parseSleepSummaryPayload(payload)
        assertNull(sleep)
    }

    @Test
    fun dispatchDecodedMessage_endpoint0x12_emitsSleepSession() = runTest {
        val payload = byteArrayOf(
            0x60, 0x00, // Deep: 96m
            0xC8.toByte(), 0x00, // Light: 200m
            0x48, 0x00, // REM: 72m
            0x10, 0x00  // Awake: 16m
        )
        val msg = DecodedMessage(
            type = AmazfitGtr3BleManager.ENDPOINT_SLEEP_SUMMARY,
            payload = payload,
            handle = 2,
            count = 0
        )

        bleManager.dispatchDecodedMessage(null, msg)

        val session = bleManager.sleepSessions.first()
        assertNotNull(session)
        assertEquals(368, session.totalSleepMinutes)
        assertEquals(96, session.deepSleepMinutes)
    }
}
