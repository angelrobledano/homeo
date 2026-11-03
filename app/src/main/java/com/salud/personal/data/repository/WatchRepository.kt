package com.salud.personal.data.repository

import com.salud.personal.data.ble.huami.AmazfitGtr3BleManager
import com.salud.personal.data.ble.huami.WatchBleManager
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.SleepSessionDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.toDomain
import com.salud.personal.data.local.entity.toEntity
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

interface WatchRepository {
    val connectionState: StateFlow<DeviceConnectionState>
    val batteryStatus: StateFlow<WatchBatteryStatus?>
    val latestHeartRate: Flow<HeartRateSample?>
    val todayActivity: Flow<DailyActivitySummary?>
    val todaySleep: Flow<SleepSessionEntity?>

    fun connect(macAddress: String, authKeyHex: String)
    fun disconnect()
}

class WatchRepositoryImpl(
    private val bleManager: WatchBleManager,
    private val heartRateDao: HeartRateDao,
    private val dailyActivityDao: DailyActivityDao,
    private val workoutDao: WorkoutDao? = null,
    private val sleepSessionDao: SleepSessionDao? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : WatchRepository {

    override val connectionState: StateFlow<DeviceConnectionState> = bleManager.connectionState
    override val batteryStatus: StateFlow<WatchBatteryStatus?> = bleManager.batteryStatus

    override val latestHeartRate: Flow<HeartRateSample?>
        get() = heartRateDao.getLatestSample().map { it?.toDomain() }

    override val todayActivity: Flow<DailyActivitySummary?>
        get() = dailyActivityDao.getActivityForDate(LocalDate.now(), AmazfitGtr3BleManager.SOURCE_DEVICE)
            .map { it?.toDomain() }

    override val todaySleep: Flow<SleepSessionEntity?>
        get() = sleepSessionDao?.observeSleepForDate(LocalDate.now()) ?: flowOf(null)

    init {
        coroutineScope.launch {
            bleManager.heartRateSamples.collect { sample ->
                heartRateDao.insert(sample.toEntity())
            }
        }

        coroutineScope.launch {
            bleManager.dailyActivity.collect { activity ->
                dailyActivityDao.insertOrUpdate(activity.toEntity())
            }
        }

        coroutineScope.launch {
            bleManager.completedWorkouts.collect { workout ->
                workoutDao?.insertWorkout(workout)
            }
        }

        coroutineScope.launch {
            bleManager.sleepSessions.collect { sleep ->
                sleepSessionDao?.insertOrUpdate(sleep)
            }
        }
    }

    override fun connect(macAddress: String, authKeyHex: String) {
        bleManager.connect(macAddress, authKeyHex)
    }

    override fun disconnect() {
        bleManager.disconnect()
    }
}
