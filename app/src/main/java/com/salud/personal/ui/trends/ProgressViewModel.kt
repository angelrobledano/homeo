package com.salud.personal.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.DailyCheckInDao
import com.salud.personal.data.local.dao.SleepSessionDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.model.WeightMeasurement
import com.salud.personal.domain.trends.TrendsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Periodos de análisis para la pestaña Progreso con soporte de gestos swipe y selección dinámica.
 */
enum class ProgressPeriod(val label: String, val subtitle: String, val days: Int) {
    LAST_7_DAYS("7D", "Semanal", 7),
    LAST_30_DAYS("30D", "Mensual", 30),
    LAST_90_DAYS("90D", "Trimestral", 90)
}

data class ProgressUiState(
    val selectedPeriod: ProgressPeriod = ProgressPeriod.LAST_7_DAYS,
    val acuteLoadTrimp7d: Double = 0.0,
    val chronicLoadTrimp28d: Double = 0.0,
    val weeklyLoadsTrimp: List<Double> = emptyList(), // [Sem-3, Sem-2, Sem-1, Sem-Actual]
    val recentWeights: List<Double> = emptyList(),
    val movingAverages: List<Double> = emptyList(),
    val weightTrend: TrendsCalculator.WeightTrendResult = TrendsCalculator.WeightTrendResult(0.0, 0.0, 0.0, null, emptyList()),
    val rhrTrend: TrendsCalculator.RhrTrendResult = TrendsCalculator.RhrTrendResult(null, 0.0, 0.0, TrendsCalculator.RhrStatus.NORMAL, emptyList()),
    val weeklyComparison: TrendsCalculator.WeeklyComparison = TrendsCalculator.WeeklyComparison(0.0, 0.0, 0, 0, 0, 0, 0, 0, 0, null, null, null),
    val isLoading: Boolean = false
)

private data class RawTrendsData(
    val workouts: List<WorkoutSessionEntity>,
    val weights: List<WeightMeasurement>,
    val checkIns: List<DailyCheckInEntity>,
    val activities: List<DailyActivityEntity>,
    val sleepSessions: List<SleepSessionEntity>
)

class ProgressViewModel(
    private val workoutDao: WorkoutDao,
    private val weightRepository: WeightRepository,
    private val dailyCheckInDao: DailyCheckInDao? = null,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val sleepSessionDao: SleepSessionDao? = null
) : ViewModel() {

    private val oneHundredEightyDaysAgo: Instant
        get() = Instant.now().minus(180, ChronoUnit.DAYS)

    private val checkInsFlow = dailyCheckInDao?.getAllCheckIns() ?: flowOf(emptyList())
    private val activitiesFlow = dailyActivityDao?.getAllDailyActivitiesFlow() ?: flowOf(emptyList())
    private val sleepFlow = sleepSessionDao?.getAllSleep() ?: flowOf(emptyList())

    private val _selectedPeriod = MutableStateFlow(ProgressPeriod.LAST_7_DAYS)
    val selectedPeriod: StateFlow<ProgressPeriod> = _selectedPeriod.asStateFlow()

    fun selectPeriod(period: ProgressPeriod) {
        _selectedPeriod.value = period
    }

    private val rawDataFlow = combine(
        workoutDao.getWorkoutsSince(oneHundredEightyDaysAgo),
        weightRepository.allMeasurements,
        checkInsFlow,
        activitiesFlow,
        sleepFlow
    ) { workouts, allWeights, checkIns, activities, sleepSessions ->
        RawTrendsData(workouts, allWeights, checkIns, activities, sleepSessions)
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        rawDataFlow,
        _selectedPeriod
    ) { data, period ->
        val workouts = data.workouts
        val allWeights = data.weights
        val checkIns = data.checkIns
        val activities = data.activities
        val sleepSessions = data.sleepSessions

        val now = Instant.now()
        val today = LocalDate.now()
        val t7d = now.minus(7, ChronoUnit.DAYS)
        val t14d = now.minus(14, ChronoUnit.DAYS)
        val t21d = now.minus(21, ChronoUnit.DAYS)
        val t28d = now.minus(28, ChronoUnit.DAYS)

        // ACWR: 7d acute vs 28d chronic (Gabbett standard)
        val w1 = workouts.filter { it.startTime >= t7d }.sumOf { (it.trimp ?: 0.0) }
        val w2 = workouts.filter { it.startTime in t14d..<t7d }.sumOf { (it.trimp ?: 0.0) }
        val w3 = workouts.filter { it.startTime in t21d..<t14d }.sumOf { (it.trimp ?: 0.0) }
        val w4 = workouts.filter { it.startTime in t28d..<t21d }.sumOf { (it.trimp ?: 0.0) }

        val weeklyLoads = listOf(w4, w3, w2, w1)
        val acuteTrimp = w1
        val chronicTrimp = workouts.filter { it.startTime >= t28d }.sumOf { (it.trimp ?: 0.0) }

        // Filtro biológico de peso (35 a 250 kg)
        val validWeights = allWeights.filter { it.weightKg in 35.0..250.0 }
        val weightThreshold = today.minusDays(period.days.toLong())
        val periodWeights = validWeights.filter {
            it.measuredAt.atZone(ZoneId.systemDefault()).toLocalDate() >= weightThreshold
        }
        val displayedWeights = if (periodWeights.isNotEmpty()) periodWeights else validWeights.take(period.days)

        val weightsChronological = displayedWeights
            .take(30)
            .map { it.weightKg }
            .reversed()

        val movingAverages = TrendsCalculator.calculateMovingAverage(weightsChronological, 3)

        // 1. Trend Weight (EMA) para el periodo seleccionado
        val weightPoints = displayedWeights.take(60).map {
            TrendsCalculator.WeightPoint(
                date = it.measuredAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                weightKg = it.weightKg
            )
        }
        val weightTrend = TrendsCalculator.calculateExponentialTrendWeight(weightPoints, alpha = 0.10)

        // 2. Historial FC en reposo para el periodo seleccionado
        val rhrStartDate = today.minusDays(period.days.toLong())
        val rhrPoints = checkIns
            .filter { it.date >= rhrStartDate && it.restingHeartRate != null }
            .map { TrendsCalculator.DailyRhrPoint(it.date, it.restingHeartRate!!) }
        val rhrTrend = TrendsCalculator.calculateRhrTrend(rhrPoints)

        // 3. Comparador según periodo seleccionado (7d vs 7d, 30d vs 30d, 90d vs 90d)
        val days = period.days
        val curDateRange = today.minusDays((days - 1).toLong())..today
        val prevDateRange = today.minusDays((2L * days - 1))..today.minusDays(days.toLong())

        val curPeriodTrimp = workouts.filter {
            val d = it.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            d in curDateRange
        }.sumOf { it.trimp ?: 0.0 }

        val prevPeriodTrimp = workouts.filter {
            val d = it.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            d in prevDateRange
        }.sumOf { it.trimp ?: 0.0 }

        val curSteps = activities.filter {
            val d = runCatching { LocalDate.parse(it.date) }.getOrNull()
            d != null && d in curDateRange
        }.map { it.steps }

        val prevSteps = activities.filter {
            val d = runCatching { LocalDate.parse(it.date) }.getOrNull()
            d != null && d in prevDateRange
        }.map { it.steps }

        val curSleep = sleepSessions.filter { it.date in curDateRange }.map { it.totalSleepMinutes }
        val prevSleep = sleepSessions.filter { it.date in prevDateRange }.map { it.totalSleepMinutes }

        val curRhrs = checkIns.filter { it.date in curDateRange && it.restingHeartRate != null }.map { it.restingHeartRate!! }
        val prevRhrs = checkIns.filter { it.date in prevDateRange && it.restingHeartRate != null }.map { it.restingHeartRate!! }

        val periodComp = TrendsCalculator.calculateWeeklyComparison(
            currentWeekTrimp = curPeriodTrimp,
            previousWeekTrimp = prevPeriodTrimp,
            currentSteps = curSteps,
            previousSteps = prevSteps,
            currentSleepMinutes = curSleep,
            previousSleepMinutes = prevSleep,
            currentRhrs = curRhrs,
            previousRhrs = prevRhrs
        )

        ProgressUiState(
            selectedPeriod = period,
            acuteLoadTrimp7d = acuteTrimp,
            chronicLoadTrimp28d = chronicTrimp,
            weeklyLoadsTrimp = weeklyLoads,
            recentWeights = weightsChronological,
            movingAverages = movingAverages,
            weightTrend = weightTrend,
            rhrTrend = rhrTrend,
            weeklyComparison = periodComp,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProgressUiState(isLoading = true)
    )
}

class ProgressViewModelFactory(
    private val workoutDao: WorkoutDao,
    private val weightRepository: WeightRepository,
    private val dailyCheckInDao: DailyCheckInDao? = null,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val sleepSessionDao: SleepSessionDao? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProgressViewModel(workoutDao, weightRepository, dailyCheckInDao, dailyActivityDao, sleepSessionDao) as T
    }
}
