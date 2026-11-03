package com.salud.personal.data.ble.huami

import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface WatchBleManager {
    val connectionState: StateFlow<DeviceConnectionState>
    val batteryStatus: StateFlow<WatchBatteryStatus?>
    val heartRateSamples: SharedFlow<HeartRateSample>
    val dailyActivity: SharedFlow<DailyActivitySummary>
    val completedWorkouts: SharedFlow<WorkoutSessionEntity>
    val sleepSessions: SharedFlow<SleepSessionEntity>

    fun connect(macAddress: String, authKeyHex: String)
    fun disconnect()
}
