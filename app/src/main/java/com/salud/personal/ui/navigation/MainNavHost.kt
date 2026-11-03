package com.salud.personal.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.domain.recovery.RecoveryCalculator
import com.salud.personal.ui.activity.ActivityDetailScreen
import com.salud.personal.ui.calendar.CalendarScreen
import com.salud.personal.ui.calendar.CalendarViewModel
import com.salud.personal.ui.coach.CoachViewModel
import com.salud.personal.ui.dashboard.DashboardViewModel
import com.salud.personal.ui.dashboard.HoyScreen
import com.salud.personal.ui.map.WalkTrackingScreen
import com.salud.personal.ui.recovery.RecoveryScreen
import com.salud.personal.ui.summary.PeriodSummaryScreen
import com.salud.personal.ui.summary.PeriodSummaryViewModel
import com.salud.personal.ui.theme.WhoopColors
import com.salud.personal.ui.trends.ProgressViewModel
import com.salud.personal.ui.workout.WorkoutScreen
import com.salud.personal.ui.workout.WorkoutViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MainNavHost(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    workoutViewModel: WorkoutViewModel? = null,
    progressViewModel: ProgressViewModel? = null,
    coachViewModel: CoachViewModel? = null,
    calendarViewModel: CalendarViewModel? = null,
    database: AppDatabase? = null,
    healthConnectAvailable: Boolean = true,
    onSyncHealthConnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val todayCheckIn by (database?.dailyCheckInDao()?.observeCheckInForDate(LocalDate.now()) ?: flowOf(null)).collectAsState(initial = null)

    val topLevelRoutes = remember { TopLevelDestination.entries.map { it.route }.toSet() }

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOY.route,
        modifier = modifier,
        enterTransition = {
            val fromTop = initialState.destination.route in topLevelRoutes
            val toTop = targetState.destination.route in topLevelRoutes
            if (fromTop && toTop) {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
            } else {
                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffset = { it / 5 }
                )
            }
        },
        exitTransition = {
            val fromTop = initialState.destination.route in topLevelRoutes
            val toTop = targetState.destination.route in topLevelRoutes
            if (fromTop && toTop) {
                fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
            } else {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    targetOffset = { -it / 5 }
                )
            }
        },
        popEnterTransition = {
            val fromTop = initialState.destination.route in topLevelRoutes
            val toTop = targetState.destination.route in topLevelRoutes
            if (fromTop && toTop) {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
            } else {
                fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffset = { -it / 5 }
                )
            }
        },
        popExitTransition = {
            val fromTop = initialState.destination.route in topLevelRoutes
            val toTop = targetState.destination.route in topLevelRoutes
            if (fromTop && toTop) {
                fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
            } else {
                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    targetOffset = { it / 5 }
                )
            }
        }
    ) {
        composable(TopLevelDestination.HOY.route) {
            val todayWorkouts by (workoutViewModel?.workouts ?: flowOf(emptyList())).collectAsState(emptyList())
            val todayHrSamples by (database?.heartRateDao()?.getAllSamples() ?: flowOf(emptyList())).collectAsState(emptyList())
            val allCheckIns by (database?.dailyCheckInDao()?.getAllCheckIns() ?: flowOf(emptyList())).collectAsState(emptyList())
            val allWeights by (database?.weightDao()?.observeAll() ?: flowOf(emptyList())).collectAsState(emptyList())
            val allSleeps by (database?.sleepSessionDao()?.getAllSleep() ?: flowOf(emptyList())).collectAsState(emptyList())

            HoyScreen(
                viewModel = dashboardViewModel,
                onOpenSettings = { navController.navigate("settings") },
                onOpenCalendar = { navController.navigate("calendar") },
                onOpenCheckIn = { navController.navigate("recovery") },
                onOpenActivityDetail = { navController.navigate("activity_detail") },
                onOpenSleepDetail = { navController.navigate("sleep_detail") },
                onOpenWorkouts = { navController.navigate(TopLevelDestination.ENTRENAMIENTO.route) },
                onSync = onSyncHealthConnect,
                todayCheckIn = todayCheckIn,
                todayWorkouts = todayWorkouts,
                todayHrSamples = todayHrSamples,
                allCheckIns = allCheckIns,
                allWeights = allWeights,
                allSleeps = allSleeps
            )
        }
        composable(TopLevelDestination.ENTRENAMIENTO.route) {
            if (workoutViewModel != null) {
                val workouts by workoutViewModel.workouts.collectAsState()
                val activeWorkout by workoutViewModel.activeWorkout.collectAsState()
                WorkoutScreen(
                    workouts = workouts,
                    activeWorkout = activeWorkout,
                    onStartWorkout = { type, name -> workoutViewModel.startWorkout(type, name) },
                    onFinishWorkout = { rpe, notes -> workoutViewModel.finishWorkout(rpe, notes) },
                    onCancelWorkout = { workoutViewModel.cancelWorkout() },
                    onAddManualWorkout = { type, name, duration, rpe, notes ->
                        workoutViewModel.addManualWorkout(type, name, duration, rpe, notes)
                    },
                    onOpenGpsWalk = { navController.navigate("walk_tracking") },
                    onSyncHealthConnect = onSyncHealthConnect
                )
            } else {
                WorkoutScreen(
                    onOpenGpsWalk = { navController.navigate("walk_tracking") },
                    onSyncHealthConnect = onSyncHealthConnect
                )
            }
        }
        composable(TopLevelDestination.PROGRESO.route) {
            val allCheckIns by (database?.dailyCheckInDao()?.getAllCheckIns() ?: flowOf(emptyList())).collectAsState(emptyList())
            val allSleeps by (database?.sleepSessionDao()?.getAllSleep() ?: flowOf(emptyList())).collectAsState(emptyList())
            val habitInsights = remember(allCheckIns, allSleeps) {
                com.salud.personal.domain.habits.HabitCorrelationCalculator.computeInsights(allCheckIns, allSleeps)
            }

            if (progressViewModel != null) {
                val state by progressViewModel.uiState.collectAsState()
                com.salud.personal.ui.trends.ProgressScreen(
                    selectedPeriod = state.selectedPeriod,
                    onSelectPeriod = { progressViewModel.selectPeriod(it) },
                    acuteLoadTrimp7d = state.acuteLoadTrimp7d,
                    chronicLoadTrimp28d = state.chronicLoadTrimp28d,
                    weeklyLoadsTrimp = state.weeklyLoadsTrimp,
                    recentWeights = state.recentWeights,
                    movingAverages = state.movingAverages,
                    weightTrend = state.weightTrend,
                    rhrTrend = state.rhrTrend,
                    weeklyComparison = state.weeklyComparison,
                    habitInsights = habitInsights,
                    onOpenPeriodSummary = { navController.navigate("period_summary") }
                )
            } else {
                com.salud.personal.ui.trends.ProgressScreen(
                    habitInsights = habitInsights,
                    onOpenPeriodSummary = { navController.navigate("period_summary") }
                )
            }
        }
        composable(TopLevelDestination.COACH.route) {
            if (coachViewModel != null) {
                val snapshot by coachViewModel.snapshot.collectAsState()
                com.salud.personal.ui.coach.CoachScreen(snapshot = snapshot)
            } else {
                val defaultSnapshot = com.salud.personal.domain.coach.UserHealthSnapshot(
                    stepsToday = 0,
                    recoveryScore = 75,
                    restingHeartRate = null,
                    sleepHours = 7.5,
                    recent7DaysTrimp = 0.0,
                    lastWorkoutType = null,
                    weightTrendKgDelta = null
                )
                com.salud.personal.ui.coach.CoachScreen(snapshot = defaultSnapshot)
            }
        }
        composable("recovery") {
            val dashState by dashboardViewModel.uiState.collectAsState()
            val rhr = dashState.latestHeartRate?.bpm ?: 50
            RecoveryScreen(
                currentRhr = rhr,
                baselineRhr = 49,
                sleepHours = 7.5,
                onSaveCheckIn = { energy, sleepQuality, soreness, readiness, lateMeal, lateCaffeine, alcohol, highStress, supplements, screenBeforeBed ->
                    if (database != null) {
                        val score = RecoveryCalculator.calculateRecovery(
                            currentRhr = rhr,
                            baselineRhr = 49,
                            sleepHours = 7.5,
                            subjectiveRating = readiness
                        ).score
                        coroutineScope.launch {
                            database.dailyCheckInDao().insertOrUpdate(
                                DailyCheckInEntity(
                                    date = LocalDate.now(),
                                    energyRating = energy,
                                    sleepQualityRating = sleepQuality,
                                    muscleSorenessRating = soreness,
                                    readinessRating = readiness,
                                    sleepHours = 7.5,
                                    restingHeartRate = rhr,
                                    calculatedRecoveryScore = score,
                                    lateMeal = lateMeal,
                                    lateCaffeine = lateCaffeine,
                                    alcohol = alcohol,
                                    highStress = highStress,
                                    supplements = supplements,
                                    screenBeforeBed = screenBeforeBed
                                )
                            )
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("activity_detail") {
            val dashState by dashboardViewModel.uiState.collectAsState()
            val hourlySteps by (database?.hourlyStepsDao()?.getHourlyStepsForDate(LocalDate.now()) ?: flowOf(emptyList())).collectAsState(initial = emptyList())
            ActivityDetailScreen(
                currentSteps = dashState.todayActivity?.steps ?: 0,
                goal = 10_000,
                distanceMeters = dashState.todayActivity?.distanceMeters ?: 0,
                caloriesBurned = dashState.todayActivity?.caloriesBurned ?: 0,
                hourlySteps = hourlySteps,
                onBack = { navController.popBackStack() }
            )
        }
        composable("sleep_detail") {
            val dashState by dashboardViewModel.uiState.collectAsState()
            val recentSleep by (database?.sleepSessionDao()?.getRecentSleep(7) ?: flowOf(emptyList())).collectAsState(initial = emptyList())
            val recentWorkouts by (workoutViewModel?.workouts ?: flowOf(emptyList())).collectAsState(initial = emptyList())
            com.salud.personal.ui.sleep.SleepDetailScreen(
                todaySleep = dashState.todaySleep,
                recentSleep = recentSleep,
                recentWorkouts = recentWorkouts,
                onBack = { navController.popBackStack() }
            )
        }
        composable("calendar") {
            val calVm = calendarViewModel ?: remember { CalendarViewModel(database) }
            val state by calVm.uiState.collectAsState()
            CalendarScreen(
                state = state,
                onSelectDate = { calVm.onSelectDate(it) },
                onPreviousMonth = { calVm.onPreviousMonth() },
                onNextMonth = { calVm.onNextMonth() },
                onSelectToday = { calVm.onSelectToday() },
                onBack = { navController.popBackStack() }
            )
        }
        composable("walk_tracking") {
            WalkTrackingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("period_summary") {
            val summaryVm = remember { PeriodSummaryViewModel(database) }
            val state by summaryVm.uiState.collectAsState()
            PeriodSummaryScreen(
                state = state,
                onSelectPeriodType = { summaryVm.selectPeriodType(it) },
                onPrevious = { summaryVm.previousPeriod() },
                onNext = { summaryVm.nextPeriod() },
                onResetToToday = { summaryVm.resetToCurrent() },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            val state by dashboardViewModel.uiState.collectAsState()
            com.salud.personal.ui.settings.SettingsScreen(
                gtr3Connected = state.watchConnectionState == com.salud.personal.domain.model.DeviceConnectionState.CONNECTED,
                gtr3Battery = state.watchBattery?.percentage ?: 0,
                healthConnectAvailable = healthConnectAvailable,
                onSyncHealthConnect = onSyncHealthConnect,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
