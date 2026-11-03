package com.salud.personal.ui.dashboard

import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import com.salud.personal.domain.model.WeightMeasurement

data class DashboardUiState(
    val isLoading: Boolean = false,
    val latestMeasurement: WeightMeasurement? = null,
    val delta7DaysKg: Double? = null,
    val scaleStatus: String = "RENPHO Elis 1 BLE (Listo)",
    val watchStatus: String = "Amazfit GTR 3",
    val errorMessage: String? = null,
    val watchConnectionState: DeviceConnectionState = DeviceConnectionState.DISCONNECTED,
    val watchBattery: WatchBatteryStatus? = null,
    val latestHeartRate: HeartRateSample? = null,
    val todayActivity: DailyActivitySummary? = null,
    val todaySleep: com.salud.personal.data.local.entity.SleepSessionEntity? = null
)

typealias DashboardState = DashboardUiState
