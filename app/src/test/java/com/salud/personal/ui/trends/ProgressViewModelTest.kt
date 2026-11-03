package com.salud.personal.ui.trends

import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.data.repository.WeightDaoInterface
import com.salud.personal.data.repository.WeightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeWorkoutDao : WorkoutDao {
        val workoutsFlow = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())

        override suspend fun insertWorkout(session: WorkoutSessionEntity): Long = 1L
        override fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>> = workoutsFlow
        override suspend fun getAllWorkoutsList(): List<WorkoutSessionEntity> = workoutsFlow.value
        override fun getWorkoutsSince(since: Instant): Flow<List<WorkoutSessionEntity>> = workoutsFlow
        override fun getWorkoutsByType(type: WorkoutType): Flow<List<WorkoutSessionEntity>> = workoutsFlow
        override suspend fun getTotalTrimpSince(since: Instant): Double? = null
        override suspend fun getWorkoutById(id: Long): WorkoutSessionEntity? = null
        override suspend fun deleteWorkoutById(id: Long) {}
    }

    private class FakeWeightDao : WeightDaoInterface {
        val weightsFlow = MutableStateFlow<List<WeightMeasurementEntity>>(emptyList())

        override suspend fun insert(entity: WeightMeasurementEntity): Long = 1L
        override suspend fun getLatest(): WeightMeasurementEntity? = weightsFlow.value.firstOrNull()
        override suspend fun getAll(): List<WeightMeasurementEntity> = weightsFlow.value
        override fun observeLatest(): Flow<WeightMeasurementEntity?> = flowOf(weightsFlow.value.firstOrNull())
        override fun observeAll(): Flow<List<WeightMeasurementEntity>> = weightsFlow
    }

    private lateinit var fakeWorkoutDao: FakeWorkoutDao
    private lateinit var fakeWeightDao: FakeWeightDao
    private lateinit var weightRepository: WeightRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeWorkoutDao = FakeWorkoutDao()
        fakeWeightDao = FakeWeightDao()
        weightRepository = WeightRepository(fakeWeightDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_calculatesTrimpAndWeightsFromDao() = runTest {
        val now = Instant.now()
        val workout7d = WorkoutSessionEntity(
            id = 1L,
            type = WorkoutType.CROSSFIT_WOD,
            name = "WOD",
            startTime = now.minus(2, ChronoUnit.DAYS),
            endTime = now.minus(2, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES),
            durationMinutes = 30.0,
            avgHeartRate = 150,
            maxHeartRate = 175,
            trimp = 45.0,
            caloriesBurned = 350,
            rpe = 8,
            notes = null
        )
        val workout14d = WorkoutSessionEntity(
            id = 2L,
            type = WorkoutType.ENDURANCE_CARDIO,
            name = "Run",
            startTime = now.minus(14, ChronoUnit.DAYS),
            endTime = now.minus(14, ChronoUnit.DAYS).plus(45, ChronoUnit.MINUTES),
            durationMinutes = 45.0,
            avgHeartRate = 140,
            maxHeartRate = 160,
            trimp = 55.0,
            caloriesBurned = 400,
            rpe = 7,
            notes = null
        )
        fakeWorkoutDao.workoutsFlow.value = listOf(workout7d, workout14d)

        val weight1 = WeightMeasurementEntity(
            id = 1L,
            sourceDevice = "RENPHO",
            weightKg = 75.0,
            measuredAtEpochMilli = now.minus(3, ChronoUnit.DAYS).toEpochMilli(),
            importedAtEpochMilli = now.toEpochMilli()
        )
        val weight2 = WeightMeasurementEntity(
            id = 2L,
            sourceDevice = "RENPHO",
            weightKg = 74.5,
            measuredAtEpochMilli = now.minus(1, ChronoUnit.DAYS).toEpochMilli(),
            importedAtEpochMilli = now.toEpochMilli()
        )
        fakeWeightDao.weightsFlow.value = listOf(weight2, weight1)

        val viewModel = ProgressViewModel(fakeWorkoutDao, weightRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProgressPeriod.LAST_7_DAYS, state.selectedPeriod)
        assertEquals(45.0, state.acuteLoadTrimp7d, 0.01)
        assertEquals(100.0, state.chronicLoadTrimp28d, 0.01)
        assertEquals(listOf(75.0, 74.5), state.recentWeights)
        assertEquals(4, state.weeklyLoadsTrimp.size)
        assertEquals(45.0, state.weeklyLoadsTrimp[3], 0.01) // current acute week
        assertEquals(listOf(75.0, 74.75), state.movingAverages)
    }

    @Test
    fun selectPeriod_updatesSelectedPeriodAndRecalculates() = runTest {
        val now = Instant.now()
        val workout20d = WorkoutSessionEntity(
            id = 1L,
            type = WorkoutType.STRENGTH,
            name = "Lift",
            startTime = now.minus(20, ChronoUnit.DAYS),
            endTime = now.minus(20, ChronoUnit.DAYS).plus(45, ChronoUnit.MINUTES),
            durationMinutes = 45.0,
            avgHeartRate = 135,
            maxHeartRate = 160,
            trimp = 60.0,
            caloriesBurned = 350,
            rpe = 7,
            notes = null
        )
        val workout50d = WorkoutSessionEntity(
            id = 2L,
            type = WorkoutType.ENDURANCE_CARDIO,
            name = "Long Run",
            startTime = now.minus(50, ChronoUnit.DAYS),
            endTime = now.minus(50, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES),
            durationMinutes = 60.0,
            avgHeartRate = 145,
            maxHeartRate = 165,
            trimp = 80.0,
            caloriesBurned = 550,
            rpe = 8,
            notes = null
        )
        fakeWorkoutDao.workoutsFlow.value = listOf(workout20d, workout50d)

        val viewModel = ProgressViewModel(fakeWorkoutDao, weightRepository)
        advanceUntilIdle()

        // Default is LAST_7_DAYS
        assertEquals(ProgressPeriod.LAST_7_DAYS, viewModel.uiState.value.selectedPeriod)
        assertEquals(0.0, viewModel.uiState.value.weeklyComparison.currentWeekTrimp, 0.01)

        // Switch to LAST_30_DAYS: workout20d falls in current 30d (Trimp 60.0), workout50d in prev 30d (Trimp 80.0)
        viewModel.selectPeriod(ProgressPeriod.LAST_30_DAYS)
        advanceUntilIdle()

        val state30d = viewModel.uiState.value
        assertEquals(ProgressPeriod.LAST_30_DAYS, state30d.selectedPeriod)
        assertEquals(60.0, state30d.weeklyComparison.currentWeekTrimp, 0.01)
        assertEquals(80.0, state30d.weeklyComparison.previousWeekTrimp, 0.01)
        // Delta %: (60 - 80) / 80 = -25%
        assertEquals(-25, state30d.weeklyComparison.trimpDeltaPct)

        // Switch to LAST_90_DAYS: both workout20d and workout50d fall in current 90d (Trimp 140.0)
        viewModel.selectPeriod(ProgressPeriod.LAST_90_DAYS)
        advanceUntilIdle()

        val state90d = viewModel.uiState.value
        assertEquals(ProgressPeriod.LAST_90_DAYS, state90d.selectedPeriod)
        assertEquals(140.0, state90d.weeklyComparison.currentWeekTrimp, 0.01)
    }
}
