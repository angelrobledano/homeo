package com.salud.personal.data.repository

import com.salud.personal.data.ble.huami.WatchBleManager
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WatchRepositoryTest {

    private class FakeWatchBleManager : WatchBleManager {
        val _connectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
        override val connectionState: StateFlow<DeviceConnectionState> = _connectionState

        val _batteryStatus = MutableStateFlow<WatchBatteryStatus?>(null)
        override val batteryStatus: StateFlow<WatchBatteryStatus?> = _batteryStatus

        val _heartRateSamples = MutableSharedFlow<HeartRateSample>()
        override val heartRateSamples: SharedFlow<HeartRateSample> = _heartRateSamples

        val _dailyActivity = MutableSharedFlow<DailyActivitySummary>()
        override val dailyActivity: SharedFlow<DailyActivitySummary> = _dailyActivity

        val _completedWorkouts = MutableSharedFlow<com.salud.personal.data.local.entity.WorkoutSessionEntity>()
        override val completedWorkouts: SharedFlow<com.salud.personal.data.local.entity.WorkoutSessionEntity> = _completedWorkouts

        val _sleepSessions = MutableSharedFlow<com.salud.personal.data.local.entity.SleepSessionEntity>()
        override val sleepSessions: SharedFlow<com.salud.personal.data.local.entity.SleepSessionEntity> = _sleepSessions

        var lastConnectMac: String? = null
        var lastConnectKey: String? = null
        var disconnectCalled = false

        override fun connect(macAddress: String, authKeyHex: String) {
            lastConnectMac = macAddress
            lastConnectKey = authKeyHex
        }

        override fun disconnect() {
            disconnectCalled = true
        }
    }

    private class FakeHeartRateDao : HeartRateDao {
        val samples = mutableListOf<HeartRateSampleEntity>()
        val latestFlow = MutableStateFlow<HeartRateSampleEntity?>(null)
        private var nextId = 1L

        override suspend fun insert(entity: HeartRateSampleEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id
            val stored = entity.copy(id = id)
            samples.removeIf { it.sourceDevice == stored.sourceDevice && it.timestamp == stored.timestamp }
            samples.add(stored)
            latestFlow.value = samples.maxByOrNull { it.timestamp }
            return id
        }

        override suspend fun insertAll(entities: List<HeartRateSampleEntity>): List<Long> {
            return entities.map { insert(it) }
        }

        override fun getLatestSample(): Flow<HeartRateSampleEntity?> = latestFlow

        override suspend fun getSamplesInRange(fromTimestamp: Long, toTimestamp: Long): List<HeartRateSampleEntity> {
            return samples.filter { it.timestamp in fromTimestamp..toTimestamp }.sortedBy { it.timestamp }
        }

        override suspend fun count(): Int = samples.size

        override suspend fun getLatestSampleSync(): HeartRateSampleEntity? = latestFlow.value

        override fun getAllSamples(): Flow<List<HeartRateSampleEntity>> =
            MutableStateFlow(samples.sortedByDescending { it.timestamp })

        override suspend fun getRecentSamplesList(limit: Int): List<HeartRateSampleEntity> =
            samples.sortedByDescending { it.timestamp }.take(limit)

        override suspend fun deleteAll(): Int {
            val count = samples.size
            samples.clear()
            latestFlow.value = null
            return count
        }
    }

    private class FakeDailyActivityDao : DailyActivityDao {
        val activities = mutableMapOf<String, DailyActivityEntity>()
        val activityFlows = mutableMapOf<String, MutableStateFlow<DailyActivityEntity?>>()
        private var nextId = 1L

        private fun key(date: String, sourceDevice: String): String = "$date|$sourceDevice"

        override suspend fun insertOrUpdate(entity: DailyActivityEntity): Long {
            val id = if (entity.id == 0L) nextId++ else entity.id
            val stored = entity.copy(id = id)
            val k = key(stored.date, stored.sourceDevice)
            activities[k] = stored
            activityFlows.getOrPut(k) { MutableStateFlow(null) }.value = stored
            return id
        }

        override suspend fun insertAll(entities: List<DailyActivityEntity>): List<Long> {
            return entities.map { insertOrUpdate(it) }
        }

        override fun getActivityForDate(date: String, sourceDevice: String): Flow<DailyActivityEntity?> {
            val k = key(date, sourceDevice)
            return activityFlows.getOrPut(k) { MutableStateFlow(activities[k]) }
        }

        override suspend fun getActivityForDateSync(date: String, sourceDevice: String): DailyActivityEntity? {
            return activities[key(date, sourceDevice)]
        }

        override fun getRecentActivities(limit: Int): Flow<List<DailyActivityEntity>> {
            return MutableStateFlow(activities.values.sortedByDescending { it.date }.take(limit))
        }

        override fun getAllDailyActivitiesFlow(): Flow<List<DailyActivityEntity>> {
            return MutableStateFlow(activities.values.sortedByDescending { it.date })
        }

        override suspend fun getAllDailyActivities(): List<DailyActivityEntity> {
            return activities.values.sortedByDescending { it.date }
        }

        override suspend fun count(): Int = activities.size

        override suspend fun deleteAll(): Int {
            val c = activities.size
            activities.clear()
            activityFlows.values.forEach { it.value = null }
            return c
        }
    }

    private lateinit var bleManager: FakeWatchBleManager
    private lateinit var heartRateDao: FakeHeartRateDao
    private lateinit var dailyActivityDao: FakeDailyActivityDao
    private lateinit var repository: WatchRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repositoryJob: Job
    private lateinit var repositoryScope: CoroutineScope

    @Before
    fun setUp() {
        bleManager = FakeWatchBleManager()
        heartRateDao = FakeHeartRateDao()
        dailyActivityDao = FakeDailyActivityDao()
        repositoryJob = SupervisorJob()
        repositoryScope = CoroutineScope(testDispatcher + repositoryJob)
        repository = WatchRepositoryImpl(
            bleManager = bleManager,
            heartRateDao = heartRateDao,
            dailyActivityDao = dailyActivityDao,
            coroutineScope = repositoryScope
        )
    }

    @After
    fun tearDown() {
        repositoryJob.cancel()
    }

    @Test
    fun connectionState_propagatesFromBleManager() {
        assertEquals(DeviceConnectionState.DISCONNECTED, repository.connectionState.value)

        bleManager._connectionState.value = DeviceConnectionState.CONNECTING
        assertEquals(DeviceConnectionState.CONNECTING, repository.connectionState.value)

        bleManager._connectionState.value = DeviceConnectionState.AUTHENTICATING
        assertEquals(DeviceConnectionState.AUTHENTICATING, repository.connectionState.value)

        bleManager._connectionState.value = DeviceConnectionState.CONNECTED
        assertEquals(DeviceConnectionState.CONNECTED, repository.connectionState.value)

        bleManager._connectionState.value = DeviceConnectionState.AUTH_FAILED
        assertEquals(DeviceConnectionState.AUTH_FAILED, repository.connectionState.value)
    }

    @Test
    fun batteryStatus_propagatesFromBleManager() {
        assertNull(repository.batteryStatus.value)

        val status = WatchBatteryStatus(
            percentage = 85,
            isCharging = false,
            timestamp = Instant.parse("2026-10-27T10:00:00Z")
        )
        bleManager._batteryStatus.value = status
        assertEquals(status, repository.batteryStatus.value)
    }

    @Test
    fun connectAndDisconnect_forwardsCallsToBleManager() {
        repository.connect("AA:BB:CC:DD:EE:FF", "0x0102030405060708090a0b0c0d0e0f10")
        assertEquals("AA:BB:CC:DD:EE:FF", bleManager.lastConnectMac)
        assertEquals("0x0102030405060708090a0b0c0d0e0f10", bleManager.lastConnectKey)

        repository.disconnect()
        assertTrue(bleManager.disconnectCalled)
    }

    @Test
    fun incomingHeartRateSamples_areAutomaticallyPersistedInDao() = runTest(testDispatcher) {
        val sample = HeartRateSample(
            timestamp = Instant.parse("2026-10-27T10:15:00Z"),
            bpm = 74,
            sourceDevice = "Amazfit GTR 3"
        )

        bleManager._heartRateSamples.emit(sample)
        advanceUntilIdle()

        assertEquals(1, heartRateDao.count())
        val latestEntity = heartRateDao.getLatestSampleSync()
        assertNotNull(latestEntity)
        assertEquals(74, latestEntity!!.bpm)
        assertEquals(sample.timestamp.toEpochMilli(), latestEntity.timestamp)
        assertEquals("Amazfit GTR 3", latestEntity.sourceDevice)

        // Verify repository's latestHeartRate exposes the mapped domain sample
        val latestDomain = repository.latestHeartRate.first()
        assertNotNull(latestDomain)
        assertEquals(sample.bpm, latestDomain!!.bpm)
        assertEquals(sample.timestamp, latestDomain.timestamp)
    }

    @Test
    fun incomingDailyActivity_isAutomaticallyPersistedInDao() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val activity = DailyActivitySummary(
            date = today,
            steps = 8240,
            distanceMeters = 5900,
            caloriesBurned = 310,
            sourceDevice = "Amazfit GTR 3"
        )

        bleManager._dailyActivity.emit(activity)
        advanceUntilIdle()

        assertEquals(1, dailyActivityDao.count())
        val entity = dailyActivityDao.getActivityForDateSync(today, "Amazfit GTR 3")
        assertNotNull(entity)
        assertEquals(8240, entity!!.steps)
        assertEquals(5900, entity.distanceMeters)
        assertEquals(310, entity.caloriesBurned)

        // Verify repository's todayActivity exposes the mapped domain summary
        val todayDomain = repository.todayActivity.first()
        assertNotNull(todayDomain)
        assertEquals(8240, todayDomain!!.steps)
        assertEquals(5900, todayDomain.distanceMeters)
        assertEquals(310, todayDomain.caloriesBurned)
    }
}
