package com.salud.personal.ui.dashboard

import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.toEntity
import com.salud.personal.data.repository.WatchRepository
import com.salud.personal.data.repository.WeightDaoInterface
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WatchBatteryStatus
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private class FakeDao : WeightDaoInterface {
        val list = mutableListOf<WeightMeasurementEntity>()
        private val _flow = MutableStateFlow<List<WeightMeasurementEntity>>(emptyList())

        override suspend fun insert(entity: WeightMeasurementEntity): Long {
            list.add(entity)
            _flow.value = list.sortedByDescending { it.measuredAtEpochMilli }
            return entity.id
        }
        override suspend fun getLatest(): WeightMeasurementEntity? {
            return list.maxByOrNull { it.measuredAtEpochMilli }
        }
        override suspend fun getAll(): List<WeightMeasurementEntity> {
            return list.sortedByDescending { it.measuredAtEpochMilli }
        }
        override fun observeLatest(): Flow<WeightMeasurementEntity?> {
            return _flow.map { it.firstOrNull() }
        }
        override fun observeAll(): Flow<List<WeightMeasurementEntity>> {
            return _flow
        }
    }

    private class FakeWatchRepository : WatchRepository {
        val _connectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
        override val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

        val _batteryStatus = MutableStateFlow<WatchBatteryStatus?>(null)
        override val batteryStatus: StateFlow<WatchBatteryStatus?> = _batteryStatus.asStateFlow()

        val _latestHeartRate = MutableStateFlow<HeartRateSample?>(null)
        override val latestHeartRate: Flow<HeartRateSample?> = _latestHeartRate.asStateFlow()

        val _todayActivity = MutableStateFlow<DailyActivitySummary?>(null)
        override val todayActivity: Flow<DailyActivitySummary?> = _todayActivity.asStateFlow()

        val _todaySleep = MutableStateFlow<com.salud.personal.data.local.entity.SleepSessionEntity?>(null)
        override val todaySleep: Flow<com.salud.personal.data.local.entity.SleepSessionEntity?> = _todaySleep.asStateFlow()

        var lastConnectedMac: String? = null
        var lastConnectedKey: String? = null
        var disconnectedCalled = false

        override fun connect(macAddress: String, authKeyHex: String) {
            lastConnectedMac = macAddress
            lastConnectedKey = authKeyHex
            _connectionState.value = DeviceConnectionState.CONNECTING
        }

        override fun disconnect() {
            disconnectedCalled = true
            _connectionState.value = DeviceConnectionState.DISCONNECTED
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDao
    private lateinit var repository: WeightRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeDao()
        repository = WeightRepository(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsEmptyStateWhenNoMeasurements() = runTest {
        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.latestMeasurement)
        assertNull(state.delta7DaysKg)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadsLatestMeasurementAndComputes7DayDelta() = runTest {
        val now = Instant.parse("2026-10-27T10:00:00Z")
        val weekAgo = now.minus(Duration.ofDays(7))

        val oldMeasurement = WeightMeasurement(
            id = 1L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.0,
            measuredAt = weekAgo,
            importedAt = weekAgo
        )
        val latestMeasurement = WeightMeasurement(
            id = 2L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 74.6,
            measuredAt = now,
            importedAt = now
        )

        fakeDao.insert(oldMeasurement.toEntity())
        fakeDao.insert(latestMeasurement.toEntity())

        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.latestMeasurement)
        assertEquals(74.6, state.latestMeasurement!!.weightKg, 0.001)
        assertNotNull(state.delta7DaysKg)
        assertEquals(-0.4, state.delta7DaysKg!!, 0.001)
    }

    @Test
    fun deltaComputesWhenMeasurementFallsWithin5To9DayWindow() = runTest {
        val now = Instant.parse("2026-10-27T10:00:00Z")
        val fiveDaysAgo = now.minus(Duration.ofDays(5))
        val nineDaysAgo = now.minus(Duration.ofDays(9))

        val measurement5Days = WeightMeasurement(
            id = 1L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.0,
            measuredAt = fiveDaysAgo,
            importedAt = fiveDaysAgo
        )
        val measurement9Days = WeightMeasurement(
            id = 2L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 76.0,
            measuredAt = nineDaysAgo,
            importedAt = nineDaysAgo
        )
        val latestMeasurement = WeightMeasurement(
            id = 3L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 74.5,
            measuredAt = now,
            importedAt = now
        )

        fakeDao.insert(measurement9Days.toEntity())
        fakeDao.insert(measurement5Days.toEntity())
        fakeDao.insert(latestMeasurement.toEntity())

        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.delta7DaysKg)
    }

    @Test
    fun deltaIsNullWhenAllPreviousMeasurementsOutside5To9DayWindow() = runTest {
        val now = Instant.parse("2026-10-27T10:00:00Z")
        val oneDayAgo = now.minus(Duration.ofDays(1))
        val tenDaysAgo = now.minus(Duration.ofDays(10))

        val measurement1Day = WeightMeasurement(
            id = 1L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.0,
            measuredAt = oneDayAgo,
            importedAt = oneDayAgo
        )
        val measurement10Days = WeightMeasurement(
            id = 2L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 76.0,
            measuredAt = tenDaysAgo,
            importedAt = tenDaysAgo
        )
        val latestMeasurement = WeightMeasurement(
            id = 3L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 74.5,
            measuredAt = now,
            importedAt = now
        )

        fakeDao.insert(measurement10Days.toEntity())
        fakeDao.insert(measurement1Day.toEntity())
        fakeDao.insert(latestMeasurement.toEntity())

        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.latestMeasurement)
        assertEquals(74.5, state.latestMeasurement!!.weightKg, 0.001)
        assertNull(state.delta7DaysKg)
    }

    @Test
    fun biologicalFilter_ignoresAnomalousWeightScaleReadings() = runTest {
        val now = Instant.parse("2026-10-27T10:00:00Z")
        val weekAgo = now.minus(Duration.ofDays(7))

        val legitimateOld = WeightMeasurement(
            id = 1L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 76.0,
            measuredAt = weekAgo,
            importedAt = weekAgo
        )
        val legitimateLatest = WeightMeasurement(
            id = 2L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.5,
            measuredAt = now,
            importedAt = now
        )
        // Anomalous reading: accidental tare or child step (20.5 kg) recorded later
        val anomalousLater = WeightMeasurement(
            id = 3L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 20.48,
            measuredAt = now.plus(Duration.ofMinutes(15)),
            importedAt = now.plus(Duration.ofMinutes(15))
        )

        fakeDao.insert(legitimateOld.toEntity())
        fakeDao.insert(legitimateLatest.toEntity())
        fakeDao.insert(anomalousLater.toEntity())

        val viewModel = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Must select the legitimate 75.5 kg, NOT the 20.48 kg anomaly
        assertEquals(75.5, state.latestMeasurement!!.weightKg, 0.001)
        // Delta must be between 75.5 and 76.0 (-0.5)
        assertEquals(-0.5, state.delta7DaysKg!!, 0.001)
    }

    @Test
    fun factoryCreatesDashboardViewModel() {
        val factory = DashboardViewModelFactory(repository)
        val created: DashboardViewModel = factory.create(DashboardViewModel::class.java)
        assertNotNull(created)
        assertEquals(DashboardViewModel::class.java, created.javaClass)
    }

    @Test
    fun factoryThrowsOnUnknownClass() {
        val factory = DashboardViewModelFactory(repository)
        class UnknownViewModel : androidx.lifecycle.ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }

    @Test
    fun watchStateDefaultsToDisconnectedAndNullMetrics() = runTest {
        val fakeWatch = FakeWatchRepository()
        val viewModel = DashboardViewModel(repository, fakeWatch)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DeviceConnectionState.DISCONNECTED, state.watchConnectionState)
        assertNull(state.watchBattery)
        assertNull(state.latestHeartRate)
        assertNull(state.todayActivity)
        assertTrue(state.watchStatus.contains("Desconectado"))
    }

    @Test
    fun updatesUiStateWhenWatchConnectsAndReportsBattery() = runTest {
        val fakeWatch = FakeWatchRepository()
        val viewModel = DashboardViewModel(repository, fakeWatch)
        advanceUntilIdle()

        fakeWatch._connectionState.value = DeviceConnectionState.CONNECTED
        fakeWatch._batteryStatus.value = WatchBatteryStatus(
            percentage = 85,
            isCharging = false,
            timestamp = Instant.now()
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DeviceConnectionState.CONNECTED, state.watchConnectionState)
        assertNotNull(state.watchBattery)
        assertEquals(85, state.watchBattery!!.percentage)
        assertEquals("Amazfit GTR 3 (85%)", state.watchStatus)
    }

    @Test
    fun updatesUiStateWhenHeartRateSampleArrives() = runTest {
        val fakeWatch = FakeWatchRepository()
        val viewModel = DashboardViewModel(repository, fakeWatch)
        advanceUntilIdle()

        val sample = HeartRateSample(
            timestamp = Instant.now(),
            bpm = 72,
            sourceDevice = "Amazfit GTR 3"
        )
        fakeWatch._latestHeartRate.value = sample
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.latestHeartRate)
        assertEquals(72, state.latestHeartRate!!.bpm)
        assertEquals("Amazfit GTR 3", state.latestHeartRate!!.sourceDevice)
    }

    @Test
    fun updatesUiStateWhenTodayActivityArrives() = runTest {
        val fakeWatch = FakeWatchRepository()
        val viewModel = DashboardViewModel(repository, fakeWatch)
        advanceUntilIdle()

        val activity = DailyActivitySummary(
            date = LocalDate.now(),
            steps = 8420,
            distanceMeters = 5800,
            caloriesBurned = 390,
            sourceDevice = "Amazfit GTR 3"
        )
        fakeWatch._todayActivity.value = activity
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.todayActivity)
        assertEquals(8420, state.todayActivity!!.steps)
        assertEquals(5800, state.todayActivity!!.distanceMeters)
        assertEquals(390, state.todayActivity!!.caloriesBurned)
    }

    @Test
    fun connectWatchDelegatesToWatchRepository() = runTest {
        val fakeWatch = FakeWatchRepository()
        val viewModel = DashboardViewModel(repository, fakeWatch)

        viewModel.connectWatch("AA:BB:CC:DD:EE:FF", "0x0102030405060708090a0b0c0d0e0f10")
        advanceUntilIdle()

        assertEquals("AA:BB:CC:DD:EE:FF", fakeWatch.lastConnectedMac)
        assertEquals("0x0102030405060708090a0b0c0d0e0f10", fakeWatch.lastConnectedKey)
        assertEquals(DeviceConnectionState.CONNECTING, fakeWatch._connectionState.value)
    }

    @Test
    fun disconnectWatchDelegatesToWatchRepository() = runTest {
        val fakeWatch = FakeWatchRepository()
        val viewModel = DashboardViewModel(repository, fakeWatch)

        fakeWatch._connectionState.value = DeviceConnectionState.CONNECTED
        viewModel.disconnectWatch()
        advanceUntilIdle()

        assertTrue(fakeWatch.disconnectedCalled)
        assertEquals(DeviceConnectionState.DISCONNECTED, fakeWatch._connectionState.value)
    }

    @Test
    fun factoryWithWatchRepositoryCreatesViewModel() {
        val fakeWatch = FakeWatchRepository()
        val factory = DashboardViewModelFactory(repository, fakeWatch)
        val created = factory.create(DashboardViewModel::class.java)
        assertNotNull(created)
    }
}
