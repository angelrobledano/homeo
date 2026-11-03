package com.salud.personal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salud.personal.BuildConfig
import com.salud.personal.data.repository.WatchRepository
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.abs

class DashboardViewModel(
    private val weightRepository: WeightRepository,
    private val watchRepository: WatchRepository? = null
) : ViewModel() {

    private data class WeightState(
        val isLoading: Boolean = true,
        val latestMeasurement: WeightMeasurement? = null,
        val delta7DaysKg: Double? = null,
        val errorMessage: String? = null
    )

    private val _weightState = MutableStateFlow(WeightState(isLoading = true))

    private val connectionFlow: Flow<DeviceConnectionState> =
        watchRepository?.connectionState ?: flowOf(DeviceConnectionState.DISCONNECTED)

    private val batteryFlow: Flow<WatchBatteryStatus?> =
        watchRepository?.batteryStatus ?: flowOf(null)

    private val heartRateFlow: Flow<HeartRateSample?> =
        (watchRepository?.latestHeartRate ?: flowOf(null)).onStart { emit(null) }

    private val activityFlow: Flow<DailyActivitySummary?> =
        (watchRepository?.todayActivity ?: flowOf(null)).onStart { emit(null) }

    private val sleepFlow: Flow<com.salud.personal.data.local.entity.SleepSessionEntity?> =
        (watchRepository?.todaySleep ?: flowOf(null)).onStart { emit(null) }

    private data class ActivityAndSleep(
        val activity: DailyActivitySummary?,
        val sleep: com.salud.personal.data.local.entity.SleepSessionEntity?
    )

    private val activityAndSleepFlow = combine(activityFlow, sleepFlow) { a, s ->
        ActivityAndSleep(a, s)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _weightState,
        connectionFlow,
        batteryFlow,
        heartRateFlow,
        activityAndSleepFlow
    ) { weight, conn, battery, hr, actSleep ->
        DashboardUiState(
            isLoading = weight.isLoading,
            latestMeasurement = weight.latestMeasurement,
            delta7DaysKg = weight.delta7DaysKg,
            errorMessage = weight.errorMessage,
            watchConnectionState = conn,
            watchBattery = battery,
            latestHeartRate = hr,
            todayActivity = actSleep.activity,
            todaySleep = actSleep.sleep,
            watchStatus = formatWatchStatus(conn, battery)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardUiState(isLoading = true)
    )

    init {
        observeWeightData()
    }

    private fun observeWeightData() {
        viewModelScope.launch {
            _weightState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                weightRepository.allMeasurements.collect { all ->
                    // Filtrar pesajes biológicamente válidos (35kg a 250kg) para evitar taras anómalas o pisadas accidentales
                    val validWeights = all.filter { it.weightKg in 35.0..250.0 }
                    val latest = validWeights.firstOrNull() ?: all.firstOrNull()
                    val delta = calculate7DayDelta(latest, validWeights)

                    _weightState.update {
                        it.copy(
                            isLoading = false,
                            latestMeasurement = latest,
                            delta7DaysKg = delta,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _weightState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Error al cargar las mediciones"
                    )
                }
            }
        }
    }

    fun loadData() {
        observeWeightData()
    }

    fun connectWatch(
        mac: String = BuildConfig.AMAZFIT_MAC,
        authKey: String = BuildConfig.AMAZFIT_AUTH_KEY
    ) {
        if (mac.isNotBlank() && authKey.isNotBlank()) {
            watchRepository?.connect(mac, authKey)
        }
    }

    fun disconnectWatch() {
        watchRepository?.disconnect()
    }

    private fun calculate7DayDelta(
        latest: WeightMeasurement?,
        all: List<WeightMeasurement>
    ): Double? {
        if (latest == null || all.size < 2) return null

        val targetInstant = latest.measuredAt.minus(Duration.ofDays(7))
        val candidateMeasurements = all.filter {
            it.id != latest.id &&
                abs(Duration.between(it.measuredAt, targetInstant).toDays()) <= TOLERANCE_DAYS
        }

        val baseline = candidateMeasurements.minByOrNull {
            abs(Duration.between(it.measuredAt, targetInstant).toMinutes())
        } ?: return null

        return latest.weightKg - baseline.weightKg
    }

    companion object {
        private const val TOLERANCE_DAYS = 2L

        fun formatWatchStatus(conn: DeviceConnectionState, battery: WatchBatteryStatus?): String {
            return when (conn) {
                DeviceConnectionState.CONNECTED -> {
                    if (battery != null) {
                        "Amazfit GTR 3 (${battery.percentage}%)"
                    } else {
                        "Amazfit GTR 3 (Conectado)"
                    }
                }
                DeviceConnectionState.CONNECTING -> "Amazfit GTR 3 (Conectando)"
                DeviceConnectionState.AUTHENTICATING -> "Amazfit GTR 3 (Autenticando)"
                DeviceConnectionState.SCANNING -> "Amazfit GTR 3 (Buscando)"
                DeviceConnectionState.AUTH_FAILED -> "Amazfit GTR 3 (Error clave)"
                DeviceConnectionState.DISCONNECTED -> "Amazfit GTR 3 (Desconectado)"
            }
        }
    }
}

class DashboardViewModelFactory(
    private val repository: WeightRepository,
    private val watchRepository: WatchRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository, watchRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
