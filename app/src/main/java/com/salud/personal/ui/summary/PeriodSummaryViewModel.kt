package com.salud.personal.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.summary.PeriodSummary
import com.salud.personal.domain.summary.PeriodSummaryCalculator
import com.salud.personal.domain.summary.PeriodType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class PeriodSummaryUiState(
    val periodType: PeriodType = PeriodType.WEEK,
    val referenceDate: LocalDate = LocalDate.now(),
    val summary: PeriodSummary? = null,
    val canGoForward: Boolean = false
)

class PeriodSummaryViewModel(
    private val database: AppDatabase?
) : ViewModel() {

    private val _periodType = MutableStateFlow(PeriodType.WEEK)
    private val _referenceDate = MutableStateFlow(LocalDate.now())

    private val workoutsFlow = database?.workoutDao()?.getAllWorkouts() ?: flowOf(emptyList())
    private val activitiesFlow = database?.dailyActivityDao()?.getAllDailyActivitiesFlow() ?: flowOf(emptyList())
    private val sleepsFlow = database?.sleepSessionDao()?.getAllSleep() ?: flowOf(emptyList())
    private val checkInsFlow = database?.dailyCheckInDao()?.getAllCheckIns() ?: flowOf(emptyList())
    private val weightsFlow = database?.let { WeightRepository(it.weightDao()).allMeasurements } ?: flowOf(emptyList())

    private data class DataInputs(
        val workouts: List<com.salud.personal.data.local.entity.WorkoutSessionEntity>,
        val activities: List<com.salud.personal.data.local.entity.DailyActivityEntity>,
        val sleeps: List<com.salud.personal.data.local.entity.SleepSessionEntity>,
        val checkIns: List<com.salud.personal.data.local.entity.DailyCheckInEntity>,
        val weights: List<com.salud.personal.domain.model.WeightMeasurement>
    )

    private val dataInputsFlow = combine(
        workoutsFlow,
        activitiesFlow,
        sleepsFlow,
        checkInsFlow,
        weightsFlow
    ) { w, a, s, c, wt ->
        DataInputs(w, a, s, c, wt)
    }

    val uiState: StateFlow<PeriodSummaryUiState> = combine(
        _periodType,
        _referenceDate,
        dataInputsFlow
    ) { type, refDate, inputs ->
        val weightsEntities = inputs.weights.map {
            com.salud.personal.data.local.entity.WeightMeasurementEntity(
                id = it.id,
                sourceDevice = it.source.name,
                weightKg = it.weightKg,
                bodyFatPercentage = it.bodyFatPercentage,
                muscleMassKg = it.muscleMassKg,
                impedanceOhms = it.impedanceOhms,
                measuredAtEpochMilli = it.measuredAt.toEpochMilli(),
                importedAtEpochMilli = it.importedAt.toEpochMilli()
            )
        }
        val summary = PeriodSummaryCalculator.computeSummary(
            periodType = type,
            referenceDate = refDate,
            workouts = inputs.workouts,
            activities = inputs.activities,
            sleeps = inputs.sleeps,
            checkIns = inputs.checkIns,
            weights = weightsEntities
        )

        val today = LocalDate.now()
        val canGoForward = when (type) {
            PeriodType.DAY -> refDate.isBefore(today)
            PeriodType.WEEK -> summary.endDate.isBefore(today)
            PeriodType.MONTH -> summary.endDate.isBefore(today)
            PeriodType.QUARTER -> summary.endDate.isBefore(today)
            PeriodType.YEAR -> summary.endDate.isBefore(today)
        }

        PeriodSummaryUiState(
            periodType = type,
            referenceDate = refDate,
            summary = summary,
            canGoForward = canGoForward
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeriodSummaryUiState()
    )

    fun selectPeriodType(type: PeriodType) {
        _periodType.value = type
        _referenceDate.value = LocalDate.now()
    }

    fun previousPeriod() {
        val current = _referenceDate.value
        _referenceDate.value = when (_periodType.value) {
            PeriodType.DAY -> current.minusDays(1)
            PeriodType.WEEK -> current.minusWeeks(1)
            PeriodType.MONTH -> current.minusMonths(1)
            PeriodType.QUARTER -> current.minusMonths(3)
            PeriodType.YEAR -> current.minusYears(1)
        }
    }

    fun nextPeriod() {
        val current = _referenceDate.value
        val today = LocalDate.now()
        val next = when (_periodType.value) {
            PeriodType.DAY -> current.plusDays(1)
            PeriodType.WEEK -> current.plusWeeks(1)
            PeriodType.MONTH -> current.plusMonths(1)
            PeriodType.QUARTER -> current.plusMonths(3)
            PeriodType.YEAR -> current.plusYears(1)
        }
        if (!next.isAfter(today)) {
            _referenceDate.value = next
        }
    }

    fun resetToCurrent() {
        _referenceDate.value = LocalDate.now()
    }
}

class PeriodSummaryViewModelFactory(
    private val database: AppDatabase?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PeriodSummaryViewModel(database) as T
    }
}
