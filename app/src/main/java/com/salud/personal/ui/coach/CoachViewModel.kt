package com.salud.personal.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.DailyCheckInDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.SleepSessionDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.coach.UserHealthSnapshot
import com.salud.personal.domain.recovery.RecoveryCalculator
import com.salud.personal.domain.sleep.SleepAnalysisCalculator
import com.salud.personal.domain.sleep.SleepDebtCalculator
import com.salud.personal.domain.strain.StrainTargetCalculator
import com.salud.personal.domain.trends.TrendsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.salud.personal.domain.habits.HabitCorrelationCalculator
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class CoachViewModel(
    dailyActivityDao: DailyActivityDao,
    workoutDao: WorkoutDao,
    heartRateDao: HeartRateDao,
    weightRepository: WeightRepository,
    dailyCheckInDao: DailyCheckInDao? = null,
    sleepSessionDao: SleepSessionDao? = null
) : ViewModel() {

    private val sevenDaysAgo: Instant
        get() = Instant.now().minus(7, ChronoUnit.DAYS)

    private val twentyEightDaysAgo: Instant
        get() = Instant.now().minus(28, ChronoUnit.DAYS)

    private data class RecoveryInputs(
        val checkIn: DailyCheckInEntity?,
        val sleepSession: SleepSessionEntity?,
        val recentSleeps: List<SleepSessionEntity>,
        val allCheckIns: List<DailyCheckInEntity> = emptyList(),
        val allSleeps: List<SleepSessionEntity> = emptyList()
    )

    private val recoveryInputsFlow: Flow<RecoveryInputs> = combine(
        dailyCheckInDao?.observeCheckInForDate(LocalDate.now()) ?: flowOf(null),
        sleepSessionDao?.observeSleepForDate(LocalDate.now()) ?: flowOf(null),
        sleepSessionDao?.getRecentSleep(7) ?: flowOf(emptyList()),
        dailyCheckInDao?.getAllCheckIns() ?: flowOf(emptyList()),
        sleepSessionDao?.getAllSleep() ?: flowOf(emptyList())
    ) { checkIn, sleep, recentSleeps, allCheckIns, allSleeps ->
        RecoveryInputs(checkIn, sleep, recentSleeps, allCheckIns, allSleeps)
    }

    val snapshot: StateFlow<UserHealthSnapshot> = combine(
        dailyActivityDao.getRecentActivities(1),
        workoutDao.getWorkoutsSince(twentyEightDaysAgo),
        heartRateDao.getLatestSample(),
        weightRepository.allMeasurements,
        recoveryInputsFlow
    ) { activities, workouts28d, latestHr, weights, recoveryInputs ->
        val checkIn = recoveryInputs.checkIn
        val sleepSession = recoveryInputs.sleepSession
        val steps = activities.firstOrNull()?.steps ?: 0
        val workouts7d = workouts28d.filter { it.startTime >= sevenDaysAgo }
        val trimp7d = workouts7d.sumOf { (it.trimp ?: 0.0) }
        val trimp28d = workouts28d.sumOf { (it.trimp ?: 0.0) }
        val lastWorkout = workouts28d.maxByOrNull { it.startTime }

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val todayWorkouts = workouts28d.filter { it.startTime.atZone(zoneId).toLocalDate() == today }
        val todayTrimp = todayWorkouts.sumOf { it.trimp ?: 0.0 }
        val currentDayStrain = StrainTargetCalculator.calculateDayStrain(steps, todayTrimp)

        // Cálculo riguroso de ACWR considerando días reales de historial disponibles
        val earliestWorkout = workouts28d.minByOrNull { it.startTime }
        val daysSpan = earliestWorkout?.let {
            Duration.between(it.startTime, Instant.now()).toDays().toInt().coerceIn(1, 28)
        } ?: 0
        val acwrResult = TrendsCalculator.calculateAcwr(
            acuteLoadSum = trimp7d,
            chronicLoadSum = trimp28d,
            historyDaysAvailable = daysSpan
        )

        // FC en reposo: prioridad al check-in matutino o muestra de reposo más reciente
        val rhr = checkIn?.restingHeartRate ?: latestHr?.bpm
        val baselineRhr = checkIn?.restingHeartRate ?: 50

        // Weight delta over 7 days if available
        val weightDelta = if (weights.size >= 2) {
            val latest = weights.first().weightKg
            val oldest = weights.last().weightKg
            kotlin.math.round((latest - oldest) * 10.0) / 10.0
        } else null

        // Real sleep from watch if synced, otherwise checkIn sleep, otherwise 7.5h default
        val actualSleepHours = sleepSession?.totalSleepMinutes?.let {
            kotlin.math.round((it / 60.0) * 10.0) / 10.0
        } ?: checkIn?.sleepHours ?: 7.5

        // Calculate recovery: check-in first if submitted, otherwise heuristic baseline
        val clampedRecovery = if (checkIn != null) {
            checkIn.calculatedRecoveryScore ?: RecoveryCalculator.calculateRecovery(
                currentRhr = rhr ?: 50,
                baselineRhr = baselineRhr,
                sleepHours = actualSleepHours,
                subjectiveRating = checkIn.readinessRating
            ).score
        } else {
            var recovery = 78
            if (rhr != null) {
                val diff = rhr - baselineRhr
                if (diff <= 0) recovery += 6
                else if (diff > 5) recovery -= (diff * 2).coerceAtMost(16)
            }
            if (actualSleepHours < 6.0) {
                recovery -= 12
            } else if (actualSleepHours >= 8.0) {
                recovery += 5
            }
            if (trimp7d > 450.0) {
                recovery -= 15
            } else if (trimp7d > 250.0) {
                recovery -= 5
            }
            recovery.coerceIn(20, 98)
        }

        val strainTarget = StrainTargetCalculator.calculateTarget(clampedRecovery)
        val debtResult = SleepDebtCalculator.calculate7DayDebt(
            recentSleeps = recoveryInputs.recentSleeps,
            baselineNeedHours = 7.75,
            recentTrimp = trimp7d / 7.0
        )
        val sleepNeed = debtResult.adjustedBedtimeNeedHours
        val sleepDebt = debtResult.totalDebtMinutes / 60.0

        val totalNeedMinutes = (sleepNeed * 60).toInt()
        val wakeHour = 7
        val wakeMinute = 30
        val totalBedMinutes = (wakeHour * 60 + wakeMinute) - totalNeedMinutes
        val normalizedBedMinutes = if (totalBedMinutes < 0) totalBedMinutes + 24 * 60 else totalBedMinutes
        val bedHour = normalizedBedMinutes / 60
        val bedMin = normalizedBedMinutes % 60
        val bedtimeFormatted = String.format(java.util.Locale.US, "%02d:%02d", bedHour, bedMin)

        val rpe = when {
            clampedRecovery >= 67 -> 8
            clampedRecovery >= 34 -> 6
            else -> 4
        }

        val habitInsights = HabitCorrelationCalculator.computeInsights(
            checkIns = recoveryInputs.allCheckIns,
            sleepSessions = recoveryInputs.allSleeps
        )

        UserHealthSnapshot(
            stepsToday = steps,
            stepGoal = 10_000,
            recoveryScore = clampedRecovery,
            restingHeartRate = rhr,
            baselineRestingHeartRate = baselineRhr,
            sleepHours = actualSleepHours,
            recent7DaysTrimp = trimp7d,
            lastWorkoutType = lastWorkout?.name ?: lastWorkout?.type?.name,
            weightTrendKgDelta = weightDelta,
            sleepNeedHours = sleepNeed,
            sleepDebtHours = sleepDebt,
            currentDayStrain = currentDayStrain,
            targetStrainMin = strainTarget.minStrain,
            targetStrainMax = strainTarget.maxStrain,
            acwrRatio = acwrResult.ratio,
            isAcwrCalibrated = acwrResult.riskCategory != TrendsCalculator.AcwrRiskCategory.INSUFFICIENT_DATA,
            rpeRecommendation = rpe,
            targetBedtime = bedtimeFormatted,
            caffeineCutoffTime = "16:00",
            dinnerCutoffTime = "21:00",
            habitInsights = habitInsights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserHealthSnapshot(
            stepsToday = 0,
            stepGoal = 10_000,
            recoveryScore = 75,
            restingHeartRate = null,
            sleepHours = 7.5,
            recent7DaysTrimp = 0.0,
            lastWorkoutType = null,
            weightTrendKgDelta = null
        )
    )
}

class CoachViewModelFactory(
    private val dailyActivityDao: DailyActivityDao,
    private val workoutDao: WorkoutDao,
    private val heartRateDao: HeartRateDao,
    private val weightRepository: WeightRepository,
    private val dailyCheckInDao: DailyCheckInDao? = null,
    private val sleepSessionDao: SleepSessionDao? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CoachViewModel(
            dailyActivityDao,
            workoutDao,
            heartRateDao,
            weightRepository,
            dailyCheckInDao,
            sleepSessionDao
        ) as T
    }
}
