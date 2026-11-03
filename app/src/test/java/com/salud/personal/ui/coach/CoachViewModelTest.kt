package com.salud.personal.ui.coach

import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.HeartRateSampleEntity
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeDailyActivityDao : DailyActivityDao {
        val activitiesFlow = MutableStateFlow<List<DailyActivityEntity>>(emptyList())
        override suspend fun insertOrUpdate(entity: DailyActivityEntity): Long = 1L
        override suspend fun insertAll(entities: List<DailyActivityEntity>): List<Long> = emptyList()
        override fun getActivityForDate(date: String, sourceDevice: String): Flow<DailyActivityEntity?> = flowOf(null)
        override suspend fun getActivityForDateSync(date: String, sourceDevice: String): DailyActivityEntity? = null
        override fun getRecentActivities(limit: Int): Flow<List<DailyActivityEntity>> = activitiesFlow
        override fun getAllDailyActivitiesFlow(): Flow<List<DailyActivityEntity>> = activitiesFlow
        override suspend fun getAllDailyActivities(): List<DailyActivityEntity> = activitiesFlow.value
        override suspend fun count(): Int = 0
        override suspend fun deleteAll(): Int = 0
    }

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

    private class FakeHeartRateDao : HeartRateDao {
        val hrFlow = MutableStateFlow<HeartRateSampleEntity?>(null)
        override suspend fun insert(entity: HeartRateSampleEntity): Long = 1L
        override suspend fun insertAll(entities: List<HeartRateSampleEntity>): List<Long> = emptyList()
        override fun getLatestSample(): Flow<HeartRateSampleEntity?> = hrFlow
        override suspend fun getSamplesInRange(fromTimestamp: Long, toTimestamp: Long): List<HeartRateSampleEntity> = emptyList()
        override suspend fun count(): Int = 0
        override suspend fun getLatestSampleSync(): HeartRateSampleEntity? = null
        override fun getAllSamples(): Flow<List<HeartRateSampleEntity>> = flowOf(emptyList())
        override suspend fun getRecentSamplesList(limit: Int): List<HeartRateSampleEntity> = listOfNotNull(hrFlow.value)
        override suspend fun deleteAll(): Int = 0
    }

    private class FakeWeightDao : WeightDaoInterface {
        val weightsFlow = MutableStateFlow<List<WeightMeasurementEntity>>(emptyList())
        override suspend fun insert(entity: WeightMeasurementEntity): Long = 1L
        override suspend fun getLatest(): WeightMeasurementEntity? = weightsFlow.value.firstOrNull()
        override suspend fun getAll(): List<WeightMeasurementEntity> = weightsFlow.value
        override fun observeLatest(): Flow<WeightMeasurementEntity?> = flowOf(weightsFlow.value.firstOrNull())
        override fun observeAll(): Flow<List<WeightMeasurementEntity>> = weightsFlow
    }

    private class FakeDailyCheckInDao : com.salud.personal.data.local.dao.DailyCheckInDao {
        val checkInFlow = MutableStateFlow<com.salud.personal.data.local.entity.DailyCheckInEntity?>(null)
        override suspend fun insertOrUpdate(checkIn: com.salud.personal.data.local.entity.DailyCheckInEntity): Long = 1L
        override suspend fun getCheckInForDate(date: LocalDate): com.salud.personal.data.local.entity.DailyCheckInEntity? = checkInFlow.value
        override fun observeCheckInForDate(date: LocalDate): Flow<com.salud.personal.data.local.entity.DailyCheckInEntity?> = checkInFlow
        override fun getAllCheckIns(): Flow<List<com.salud.personal.data.local.entity.DailyCheckInEntity>> = flowOf(listOfNotNull(checkInFlow.value))
        override suspend fun getAllCheckInsList(): List<com.salud.personal.data.local.entity.DailyCheckInEntity> = listOfNotNull(checkInFlow.value)
        override suspend fun getAverageRhrSince(since: LocalDate): Double? = null
    }

    private class FakeSleepSessionDao : com.salud.personal.data.local.dao.SleepSessionDao {
        val sleepFlow = MutableStateFlow<com.salud.personal.data.local.entity.SleepSessionEntity?>(null)
        override suspend fun insertOrUpdate(session: com.salud.personal.data.local.entity.SleepSessionEntity): Long = 1L
        override fun observeSleepForDate(date: LocalDate): Flow<com.salud.personal.data.local.entity.SleepSessionEntity?> = sleepFlow
        override suspend fun getSleepForDate(date: LocalDate): com.salud.personal.data.local.entity.SleepSessionEntity? = sleepFlow.value
        override fun getRecentSleep(limit: Int): Flow<List<com.salud.personal.data.local.entity.SleepSessionEntity>> = flowOf(listOfNotNull(sleepFlow.value))
        override fun getAllSleep(): Flow<List<com.salud.personal.data.local.entity.SleepSessionEntity>> = flowOf(listOfNotNull(sleepFlow.value))
        override suspend fun getAllSleepList(): List<com.salud.personal.data.local.entity.SleepSessionEntity> = listOfNotNull(sleepFlow.value)
        override suspend fun deleteAll(): Int = 0
    }

    private lateinit var dailyActivityDao: FakeDailyActivityDao
    private lateinit var workoutDao: FakeWorkoutDao
    private lateinit var heartRateDao: FakeHeartRateDao
    private lateinit var weightRepository: WeightRepository
    private lateinit var dailyCheckInDao: FakeDailyCheckInDao
    private lateinit var sleepSessionDao: FakeSleepSessionDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dailyActivityDao = FakeDailyActivityDao()
        workoutDao = FakeWorkoutDao()
        heartRateDao = FakeHeartRateDao()
        weightRepository = WeightRepository(FakeWeightDao())
        dailyCheckInDao = FakeDailyCheckInDao()
        sleepSessionDao = FakeSleepSessionDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun snapshot_combinesRealDataFromDaos() = runTest {
        dailyActivityDao.activitiesFlow.value = listOf(
            DailyActivityEntity(
                date = LocalDate.now().toString(),
                steps = 8500,
                distanceMeters = 6200,
                caloriesBurned = 420,
                sourceDevice = "Amazfit GTR 3"
            )
        )

        val now = Instant.now()
        workoutDao.workoutsFlow.value = listOf(
            WorkoutSessionEntity(
                id = 1L,
                type = WorkoutType.CROSSFIT_WOD,
                name = "CrossFit Murph",
                startTime = now.minus(2, ChronoUnit.HOURS),
                endTime = now.minus(1, ChronoUnit.HOURS),
                durationMinutes = 60.0,
                avgHeartRate = 160,
                maxHeartRate = 182,
                trimp = 95.0,
                caloriesBurned = 650,
                rpe = 9,
                notes = null
            )
        )

        heartRateDao.hrFlow.value = HeartRateSampleEntity(
            id = 1L,
            sourceDevice = "Amazfit GTR 3",
            bpm = 56,
            timestamp = now.toEpochMilli()
        )

        val viewModel = CoachViewModel(
            dailyActivityDao = dailyActivityDao,
            workoutDao = workoutDao,
            heartRateDao = heartRateDao,
            weightRepository = weightRepository
        )

        advanceUntilIdle()

        val snapshot = viewModel.snapshot.value
        assertNotNull(snapshot)
        assertEquals(8500, snapshot.stepsToday)
        assertEquals(95.0, snapshot.recent7DaysTrimp, 0.01)
        assertEquals("CrossFit Murph", snapshot.lastWorkoutType)
        assertEquals(56, snapshot.restingHeartRate)
    }

    @Test
    fun snapshot_incorporatesSubjectiveCheckInRecoveryScore() = runTest {
        dailyCheckInDao.checkInFlow.value = com.salud.personal.data.local.entity.DailyCheckInEntity(
            date = LocalDate.now(),
            energyRating = 5,
            sleepQualityRating = 5,
            muscleSorenessRating = 1,
            readinessRating = 5,
            sleepHours = 8.5,
            restingHeartRate = 48,
            calculatedRecoveryScore = 94
        )

        val viewModel = CoachViewModel(
            dailyActivityDao = dailyActivityDao,
            workoutDao = workoutDao,
            heartRateDao = heartRateDao,
            weightRepository = weightRepository,
            dailyCheckInDao = dailyCheckInDao
        )

        advanceUntilIdle()

        val snapshot = viewModel.snapshot.value
        assertNotNull(snapshot)
        assertEquals(94, snapshot.recoveryScore)
    }

    @Test
    fun snapshot_incorporatesRealSleepFromGtr3() = runTest {
        sleepSessionDao.sleepFlow.value = com.salud.personal.data.local.entity.SleepSessionEntity(
            date = LocalDate.now(),
            startTime = Instant.now().minus(8, ChronoUnit.HOURS),
            endTime = Instant.now(),
            deepSleepMinutes = 110,
            lightSleepMinutes = 240,
            remSleepMinutes = 80,
            awakeMinutes = 20,
            totalSleepMinutes = 430, // 430 min = 7.16h -> 7.2h
            sleepEfficiencyScore = 95,
            sourceDevice = "Amazfit GTR 3"
        )

        val viewModel = CoachViewModel(
            dailyActivityDao = dailyActivityDao,
            workoutDao = workoutDao,
            heartRateDao = heartRateDao,
            weightRepository = weightRepository,
            dailyCheckInDao = dailyCheckInDao,
            sleepSessionDao = sleepSessionDao
        )

        advanceUntilIdle()

        val snapshot = viewModel.snapshot.value
        assertNotNull(snapshot)
        assertEquals(7.2, snapshot.sleepHours, 0.1)
    }
}
