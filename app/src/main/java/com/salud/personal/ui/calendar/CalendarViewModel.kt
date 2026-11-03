package com.salud.personal.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.domain.calendar.DailySummary
import com.salud.personal.domain.calendar.DailySummaryAggregator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dailySummariesMap: Map<LocalDate, DailySummary> = emptyMap(),
    val selectedDaySummary: DailySummary? = null,
    val isLoading: Boolean = false
)

class CalendarViewModel(
    private val database: AppDatabase?
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private val activitiesFlow: Flow<List<DailyActivityEntity>> =
        database?.dailyActivityDao()?.getAllDailyActivitiesFlow() ?: flowOf(emptyList())

    private val workoutsFlow: Flow<List<WorkoutSessionEntity>> =
        database?.workoutDao()?.getAllWorkouts() ?: flowOf(emptyList())

    private val sleepFlow: Flow<List<SleepSessionEntity>> =
        database?.sleepSessionDao()?.getAllSleep() ?: flowOf(emptyList())

    private val checkInsFlow: Flow<List<DailyCheckInEntity>> =
        database?.dailyCheckInDao()?.getAllCheckIns() ?: flowOf(emptyList())

    private val weightsFlow: Flow<List<WeightMeasurementEntity>> =
        database?.weightDao()?.observeAll() ?: flowOf(emptyList())

    // Combina todas las fuentes históricas en un mapa reactivo por fecha
    private val summariesMapFlow: Flow<Map<LocalDate, DailySummary>> = combine(
        activitiesFlow,
        workoutsFlow,
        sleepFlow,
        checkInsFlow,
        weightsFlow
    ) { activities, workouts, sleepList, checkIns, weights ->
        val map = mutableMapOf<LocalDate, DailySummary>()

        val activitiesByDate = activities.associateBy {
            try {
                LocalDate.parse(it.date)
            } catch (e: Exception) {
                LocalDate.now()
            }
        }

        val workoutsByDate = workouts.groupBy {
            it.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val sleepByDate = sleepList.associateBy { it.date }
        val checkInsByDate = checkIns.associateBy { it.date }

        val weightsByDate = weights.associateBy {
            Instant.ofEpochMilli(it.measuredAtEpochMilli).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val allDates = (activitiesByDate.keys + workoutsByDate.keys + sleepByDate.keys + checkInsByDate.keys + weightsByDate.keys)

        allDates.forEach { date ->
            val w = weightsByDate[date]
            map[date] = DailySummaryAggregator.aggregate(
                date = date,
                activity = activitiesByDate[date],
                workouts = workoutsByDate[date] ?: emptyList(),
                sleep = sleepByDate[date],
                checkIn = checkInsByDate[date],
                weightKg = w?.weightKg,
                bodyFatPct = w?.bodyFatPercentage?.toDouble()
            )
        }

        map
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedMonth,
        _selectedDate,
        summariesMapFlow
    ) { month, date, map ->
        val currentSummary = map[date] ?: DailySummaryAggregator.aggregate(
            date = date,
            activity = null,
            workouts = emptyList(),
            sleep = null,
            checkIn = null,
            weightKg = null,
            bodyFatPct = null
        )

        CalendarUiState(
            selectedMonth = month,
            selectedDate = date,
            dailySummariesMap = map,
            selectedDaySummary = currentSummary,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(isLoading = true)
    )

    fun onSelectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _selectedMonth.value) {
            _selectedMonth.value = YearMonth.from(date)
        }
    }

    fun onPreviousMonth() {
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        _selectedMonth.update { it.plusMonths(1) }
    }

    fun onSelectToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _selectedMonth.value = YearMonth.from(today)
    }
}
