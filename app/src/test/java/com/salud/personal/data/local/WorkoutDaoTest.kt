package com.salud.personal.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
class WorkoutDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: WorkoutDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.workoutDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQuery_workoutSession_calculatesTotalTrimp() = runBlocking {
        val now = Instant.now()
        val session1 = WorkoutSessionEntity(
            type = WorkoutType.CROSSFIT_WOD,
            name = "Murph",
            startTime = now.minus(2, ChronoUnit.HOURS),
            endTime = now.minus(1, ChronoUnit.HOURS),
            durationMinutes = 60.0,
            avgHeartRate = 165,
            maxHeartRate = 188,
            trimp = 125.4,
            caloriesBurned = 620,
            rpe = 9,
            notes = "Con chaleco de 9kg"
        )

        val id = dao.insertWorkout(session1)
        val fetched = dao.getWorkoutById(id)

        assertEquals("Murph", fetched?.name)
        assertEquals(WorkoutType.CROSSFIT_WOD, fetched?.type)
        assertEquals(125.4, fetched?.trimp ?: 0.0, 0.01)

        val totalTrimp = dao.getTotalTrimpSince(now.minus(1, ChronoUnit.DAYS))
        assertEquals(125.4, totalTrimp ?: 0.0, 0.01)

        val all = dao.getAllWorkouts().first()
        assertEquals(1, all.size)

        dao.deleteWorkoutById(id)
        val afterDelete = dao.getAllWorkoutsList()
        assertEquals(0, afterDelete.size)
    }
}
