package com.salud.personal.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.salud.personal.data.local.dao.DailyCheckInDao
import com.salud.personal.data.local.entity.DailyCheckInEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DailyCheckInDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DailyCheckInDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.dailyCheckInDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQuery_checkIn_persistsAndCalculatesAvgRhr() = runBlocking {
        val today = LocalDate.of(2026, 10, 29)
        val entity = DailyCheckInEntity(
            date = today,
            energyRating = 4,
            sleepQualityRating = 4,
            muscleSorenessRating = 2,
            readinessRating = 5,
            sleepHours = 7.5,
            restingHeartRate = 49,
            calculatedRecoveryScore = 84
        )

        dao.insertOrUpdate(entity)
        val fetched = dao.getCheckInForDate(today)

        assertEquals(49, fetched?.restingHeartRate)
        assertEquals(84, fetched?.calculatedRecoveryScore)

        val avgRhr = dao.getAverageRhrSince(today.minusDays(7))
        assertEquals(49.0, avgRhr ?: 0.0, 0.1)
    }
}
