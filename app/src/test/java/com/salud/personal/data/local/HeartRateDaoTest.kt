package com.salud.personal.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import com.salud.personal.data.local.entity.toDomain
import com.salud.personal.data.local.entity.toEntity
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.HeartRateSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class HeartRateDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var heartRateDao: HeartRateDao
    private lateinit var dailyActivityDao: DailyActivityDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        heartRateDao = db.heartRateDao()
        dailyActivityDao = db.dailyActivityDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertHeartRate_duplicateSourceAndTimestamp_replacesIdempotently() = runTest {
        val sample1 = HeartRateSampleEntity(
            timestamp = 1700000000000L,
            bpm = 68,
            sourceDevice = "Amazfit GTR 3"
        )
        val sampleDuplicate = HeartRateSampleEntity(
            timestamp = 1700000000000L,
            bpm = 74, // Updated/corrected measurement
            sourceDevice = "Amazfit GTR 3"
        )

        heartRateDao.insert(sample1)
        assertEquals(1, heartRateDao.count())

        // Insert duplicate (same sourceDevice and timestamp)
        heartRateDao.insert(sampleDuplicate)
        assertEquals(1, heartRateDao.count())

        val stored = heartRateDao.getLatestSampleSync()
        assertNotNull(stored)
        assertEquals(74, stored!!.bpm)
        assertEquals("Amazfit GTR 3", stored.sourceDevice)

        // Sample with different device at same timestamp should NOT be replaced
        val sampleOtherDevice = HeartRateSampleEntity(
            timestamp = 1700000000000L,
            bpm = 72,
            sourceDevice = "Polar H10"
        )
        heartRateDao.insert(sampleOtherDevice)
        assertEquals(2, heartRateDao.count())
    }

    @Test
    fun insertAll_multipleSamples_savesAllCorrectly() = runTest {
        val samples = listOf(
            HeartRateSampleEntity(timestamp = 1000L, bpm = 60, sourceDevice = "Amazfit GTR 3"),
            HeartRateSampleEntity(timestamp = 2000L, bpm = 70, sourceDevice = "Amazfit GTR 3"),
            HeartRateSampleEntity(timestamp = 3000L, bpm = 80, sourceDevice = "Amazfit GTR 3")
        )

        val insertedIds = heartRateDao.insertAll(samples)
        assertEquals(3, insertedIds.size)
        assertEquals(3, heartRateDao.count())
    }

    @Test
    fun getLatestSample_flowEmitsMostRecentSample() = runTest {
        val sampleOld = HeartRateSampleEntity(timestamp = 1000L, bpm = 60, sourceDevice = "Amazfit GTR 3")
        val sampleNewest = HeartRateSampleEntity(timestamp = 5000L, bpm = 95, sourceDevice = "Amazfit GTR 3")
        val sampleMiddle = HeartRateSampleEntity(timestamp = 3000L, bpm = 75, sourceDevice = "Amazfit GTR 3")

        heartRateDao.insertAll(listOf(sampleOld, sampleNewest, sampleMiddle))

        val latest = heartRateDao.getLatestSample().first()
        assertNotNull(latest)
        assertEquals(5000L, latest!!.timestamp)
        assertEquals(95, latest.bpm)
    }

    @Test
    fun getSamplesInRange_filtersAndSortsByTimestampAscending() = runTest {
        val samples = listOf(
            HeartRateSampleEntity(timestamp = 1000L, bpm = 60, sourceDevice = "Amazfit GTR 3"),
            HeartRateSampleEntity(timestamp = 2000L, bpm = 65, sourceDevice = "Amazfit GTR 3"),
            HeartRateSampleEntity(timestamp = 3000L, bpm = 70, sourceDevice = "Amazfit GTR 3"),
            HeartRateSampleEntity(timestamp = 4000L, bpm = 75, sourceDevice = "Amazfit GTR 3"),
            HeartRateSampleEntity(timestamp = 5000L, bpm = 80, sourceDevice = "Amazfit GTR 3")
        )
        heartRateDao.insertAll(samples)

        val inRange = heartRateDao.getSamplesInRange(2000L, 4000L)
        assertEquals(3, inRange.size)
        assertEquals(2000L, inRange[0].timestamp)
        assertEquals(3000L, inRange[1].timestamp)
        assertEquals(4000L, inRange[2].timestamp)

        // Test with Instant overload
        val inRangeInstant = heartRateDao.getSamplesInRange(
            Instant.ofEpochMilli(2000L),
            Instant.ofEpochMilli(4000L)
        )
        assertEquals(3, inRangeInstant.size)
    }

    @Test
    fun dailyActivity_insertAndQueryByDateFlow() = runTest {
        val activity = DailyActivityEntity(
            date = "2026-10-27",
            steps = 8450,
            distanceMeters = 6120,
            caloriesBurned = 430,
            sourceDevice = "Amazfit GTR 3"
        )

        dailyActivityDao.insertOrUpdate(activity)

        val resultString = dailyActivityDao.getActivityForDate("2026-10-27", "Amazfit GTR 3").first()
        assertNotNull(resultString)
        assertEquals(8450, resultString!!.steps)
        assertEquals(6120, resultString.distanceMeters)
        assertEquals(430, resultString.caloriesBurned)

        // Test LocalDate overload
        val resultLocalDate = dailyActivityDao.getActivityForDate(LocalDate.parse("2026-10-27"), "Amazfit GTR 3").first()
        assertNotNull(resultLocalDate)
        assertEquals(8450, resultLocalDate!!.steps)

        val missing = dailyActivityDao.getActivityForDate("2026-10-20", "Amazfit GTR 3").first()
        assertNull(missing)
    }

    @Test
    fun dailyActivity_idempotentUpdateOnDuplicateDateAndDevice() = runTest {
        val activity1 = DailyActivityEntity(
            date = "2026-10-27",
            steps = 3000,
            distanceMeters = 2200,
            caloriesBurned = 150,
            sourceDevice = "Amazfit GTR 3"
        )
        val activityUpdated = DailyActivityEntity(
            date = "2026-10-27",
            steps = 11200,
            distanceMeters = 8400,
            caloriesBurned = 580,
            sourceDevice = "Amazfit GTR 3"
        )

        dailyActivityDao.insertOrUpdate(activity1)
        assertEquals(1, dailyActivityDao.count())

        // Same date and device updates existing entry
        dailyActivityDao.insertOrUpdate(activityUpdated)
        assertEquals(1, dailyActivityDao.count())

        val stored = dailyActivityDao.getActivityForDateSync("2026-10-27", "Amazfit GTR 3")
        assertNotNull(stored)
        assertEquals(11200, stored!!.steps)
        assertEquals(8400, stored.distanceMeters)
        assertEquals(580, stored.caloriesBurned)
    }

    @Test
    fun dailyActivity_getRecentActivities_ordersDescendingByDateWithLimit() = runTest {
        val activities = listOf(
            DailyActivityEntity(date = "2026-10-23", steps = 6000, distanceMeters = 4000, caloriesBurned = 300, sourceDevice = "Amazfit GTR 3"),
            DailyActivityEntity(date = "2026-10-24", steps = 7000, distanceMeters = 5000, caloriesBurned = 350, sourceDevice = "Amazfit GTR 3"),
            DailyActivityEntity(date = "2026-10-25", steps = 8000, distanceMeters = 6000, caloriesBurned = 400, sourceDevice = "Amazfit GTR 3"),
            DailyActivityEntity(date = "2026-10-26", steps = 9000, distanceMeters = 7000, caloriesBurned = 450, sourceDevice = "Amazfit GTR 3"),
            DailyActivityEntity(date = "2026-10-27", steps = 10000, distanceMeters = 8000, caloriesBurned = 500, sourceDevice = "Amazfit GTR 3")
        )
        dailyActivityDao.insertAll(activities)

        val recent = dailyActivityDao.getRecentActivities(limit = 3).first()
        assertEquals(3, recent.size)
        assertEquals("2026-10-27", recent[0].date)
        assertEquals("2026-10-26", recent[1].date)
        assertEquals("2026-10-25", recent[2].date)
    }

    @Test
    fun entityToDomainMapping_convertsCorrectly() {
        val hrSample = HeartRateSample(
            timestamp = Instant.ofEpochMilli(1700000000000L),
            bpm = 72,
            sourceDevice = "Amazfit GTR 3"
        )
        val hrEntity = hrSample.toEntity()
        assertEquals(1700000000000L, hrEntity.timestamp)
        assertEquals(72, hrEntity.bpm)
        assertEquals("Amazfit GTR 3", hrEntity.sourceDevice)
        assertEquals(hrSample, hrEntity.toDomain())

        val activitySummary = DailyActivitySummary(
            date = LocalDate.parse("2026-10-27"),
            steps = 9500,
            distanceMeters = 7200,
            caloriesBurned = 480,
            sourceDevice = "Amazfit GTR 3"
        )
        val activityEntity = activitySummary.toEntity()
        assertEquals("2026-10-27", activityEntity.date)
        assertEquals(9500, activityEntity.steps)
        assertEquals(activitySummary, activityEntity.toDomain())
    }

    @Test
    fun migration1To2_createsNewTablesAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_1_2.db")
        dbFile.delete()

        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration_1_2.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `weight_measurements` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `sourceDevice` TEXT NOT NULL,
                            `externalId` TEXT,
                            `weightKg` REAL NOT NULL,
                            `bodyFatPercentage` REAL,
                            `muscleMassKg` REAL,
                            `impedanceOhms` REAL,
                            `measuredAtEpochMilli` INTEGER NOT NULL,
                            `importedAtEpochMilli` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val openHelper = factory.create(helperConfig)
        val v1Db = openHelper.writableDatabase

        // 1. Insert data into version 1 table
        v1Db.execSQL(
            """
            INSERT INTO weight_measurements (
                sourceDevice, externalId, weightKg, bodyFatPercentage, muscleMassKg, impedanceOhms, measuredAtEpochMilli, importedAtEpochMilli
            ) VALUES ('RENPHO_ELIS_1', 'scale_01', 74.2, 18.2, 57.5, 490.0, 1700000000000, 1700000001000)
            """.trimIndent()
        )
        v1Db.version = 1

        // 2. Run migration MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(v1Db)
        v1Db.version = 2

        // 3. Verify version 1 data remains intact
        val cursorWeight = v1Db.query("SELECT weightKg, sourceDevice FROM weight_measurements")
        assertTrue(cursorWeight.moveToFirst())
        assertEquals(74.2, cursorWeight.getDouble(0), 0.001)
        assertEquals("RENPHO_ELIS_1", cursorWeight.getString(1))
        cursorWeight.close()

        // 4. Verify heart_rate_samples table exists and unique index prevents duplicates
        v1Db.execSQL(
            "INSERT OR REPLACE INTO heart_rate_samples (timestamp, bpm, sourceDevice) VALUES (1000, 70, 'Amazfit GTR 3')"
        )
        v1Db.execSQL(
            "INSERT OR REPLACE INTO heart_rate_samples (timestamp, bpm, sourceDevice) VALUES (1000, 85, 'Amazfit GTR 3')"
        )
        val cursorHr = v1Db.query("SELECT COUNT(*), bpm FROM heart_rate_samples WHERE timestamp = 1000")
        assertTrue(cursorHr.moveToFirst())
        assertEquals(1, cursorHr.getInt(0))
        assertEquals(85, cursorHr.getInt(1))
        cursorHr.close()

        // 5. Verify daily_activity table exists and unique index works
        v1Db.execSQL(
            "INSERT OR REPLACE INTO daily_activity (date, steps, distanceMeters, caloriesBurned, sourceDevice) VALUES ('2026-10-27', 8000, 6000, 400, 'Amazfit GTR 3')"
        )
        v1Db.execSQL(
            "INSERT OR REPLACE INTO daily_activity (date, steps, distanceMeters, caloriesBurned, sourceDevice) VALUES ('2026-10-27', 12000, 9000, 600, 'Amazfit GTR 3')"
        )
        val cursorAct = v1Db.query("SELECT COUNT(*), steps FROM daily_activity WHERE date = '2026-10-27'")
        assertTrue(cursorAct.moveToFirst())
        assertEquals(1, cursorAct.getInt(0))
        assertEquals(12000, cursorAct.getInt(1))
        cursorAct.close()

        v1Db.close()
        openHelper.close()
        dbFile.delete()
    }
}
