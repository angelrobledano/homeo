package com.salud.personal.ui.workout

import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.model.HeartRateSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeWorkoutDao()
        viewModel = WorkoutViewModel(
            workoutDao = fakeDao,
            maxHeartRateSetting = 190,
            restingHeartRateSetting = 50
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startWorkout_setsActiveState() {
        assertFalse(viewModel.activeWorkout.value.isRecording)

        viewModel.startWorkout(WorkoutType.CROSSFIT_WOD, "Murph")

        val state = viewModel.activeWorkout.value
        assertTrue(state.isRecording)
        assertEquals("Murph", state.name)
        assertEquals(WorkoutType.CROSSFIT_WOD, state.type)
        assertNotNull(state.startTime)
    }

    @Test
    fun onHeartRateReceived_accumulatesSamplesAndComputesTrimp() {
        viewModel.startWorkout(WorkoutType.ENDURANCE_CARDIO, "Carrera 5km")

        viewModel.onHeartRateReceived(HeartRateSample(Instant.now(), 150, "Amazfit GTR 3"))
        viewModel.onHeartRateReceived(HeartRateSample(Instant.now(), 170, "Amazfit GTR 3"))

        val state = viewModel.activeWorkout.value
        assertEquals(170, state.currentHeartRate)
        assertEquals(160, state.avgHeartRate)
        assertEquals(170, state.maxHeartRate)
        assertEquals(2, state.hrSamplesCount)
        assertTrue(state.estimatedTrimp >= 0.0)
    }

    @Test
    fun finishWorkout_persistsSessionToDaoAndResetsState() = runTest {
        viewModel.startWorkout(WorkoutType.CROSSFIT_WOD, "Fran")
        viewModel.onHeartRateReceived(HeartRateSample(Instant.now(), 180, "Amazfit GTR 3"))

        viewModel.finishWorkout(rpe = 9, notes = "Unbroken thrusters")
        advanceUntilIdle()

        assertFalse(viewModel.activeWorkout.value.isRecording)
        val list = fakeDao.getAllWorkouts().first()
        assertEquals(1, list.size)
        assertEquals("Fran", list[0].name)
        assertEquals(9, list[0].rpe)
        assertEquals(180, list[0].avgHeartRate)
    }

    @Test
    fun cancelWorkout_discardsSessionWithoutSaving() = runTest {
        viewModel.startWorkout(WorkoutType.CROSSFIT_WOD, "Cancelled")
        viewModel.cancelWorkout()
        advanceUntilIdle()

        assertFalse(viewModel.activeWorkout.value.isRecording)
        val list = fakeDao.getAllWorkouts().first()
        assertTrue(list.isEmpty())
    }

    private class FakeWorkoutDao : WorkoutDao {
        val storage = mutableListOf<WorkoutSessionEntity>()
        val flow = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())

        override suspend fun insertWorkout(session: WorkoutSessionEntity): Long {
            storage.add(session)
            flow.value = storage.toList()
            return storage.size.toLong()
        }

        override fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>> = flow

        override suspend fun getAllWorkoutsList(): List<WorkoutSessionEntity> = storage.toList()

        override fun getWorkoutsSince(since: Instant): Flow<List<WorkoutSessionEntity>> = flow

        override fun getWorkoutsByType(type: WorkoutType): Flow<List<WorkoutSessionEntity>> = flow

        override suspend fun getTotalTrimpSince(since: Instant): Double? {
            return storage.sumOf { it.trimp ?: 0.0 }
        }

        override suspend fun getWorkoutById(id: Long): WorkoutSessionEntity? {
            return storage.find { it.id == id }
        }

        override suspend fun deleteWorkoutById(id: Long) {
            storage.removeAll { it.id == id }
            flow.value = storage.toList()
        }
    }
}
