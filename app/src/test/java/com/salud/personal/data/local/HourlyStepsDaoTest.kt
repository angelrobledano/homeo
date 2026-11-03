package com.salud.personal.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.salud.personal.data.local.dao.HourlyStepsDao
import com.salud.personal.data.local.entity.HourlyStepsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class HourlyStepsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: HourlyStepsDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.hourlyStepsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQuery_hourlySteps_returnsCorrectItems() = runBlocking {
        val today = LocalDate.of(2026, 10, 29)
        val h1 = HourlyStepsEntity(date = today, hour = 8, steps = 1200, sourceDevice = "Amazfit GTR 3")
        val h2 = HourlyStepsEntity(date = today, hour = 9, steps = 2400, sourceDevice = "Amazfit GTR 3")

        dao.insertOrUpdateAll(listOf(h1, h2))

        val results = dao.getHourlyStepsForDate(today).first()
        assertEquals(2, results.size)
        assertEquals(8, results[0].hour)
        assertEquals(1200, results[0].steps)
        assertEquals(9, results[1].hour)
        assertEquals(2400, results[1].steps)

        val total = dao.getTotalStepsForDate(today)
        assertEquals(3600, total)
    }

    @Test
    fun insertDuplicateHour_replacesExisting() = runBlocking {
        val today = LocalDate.of(2026, 10, 29)
        val h1 = HourlyStepsEntity(date = today, hour = 10, steps = 500, sourceDevice = "Amazfit GTR 3")
        dao.insertOrUpdate(h1)

        val h1Updated = HourlyStepsEntity(date = today, hour = 10, steps = 950, sourceDevice = "Amazfit GTR 3")
        dao.insertOrUpdate(h1Updated)

        val results = dao.getHourlyStepsForDateOnce(today)
        assertEquals(1, results.size)
        assertEquals(950, results[0].steps)
    }
}
