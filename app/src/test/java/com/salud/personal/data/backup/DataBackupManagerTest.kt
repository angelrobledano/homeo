package com.salud.personal.data.backup

import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.data.repository.WeightDaoInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DataBackupManagerTest {

    private class FakeWeightDao(private val records: List<WeightMeasurementEntity>) : WeightDaoInterface {
        override suspend fun insert(entity: WeightMeasurementEntity): Long = 1L
        override suspend fun getLatest(): WeightMeasurementEntity? = records.firstOrNull()
        override suspend fun getAll(): List<WeightMeasurementEntity> = records
        override fun observeLatest(): Flow<WeightMeasurementEntity?> = flowOf(records.firstOrNull())
        override fun observeAll(): Flow<List<WeightMeasurementEntity>> = flowOf(records)
    }

    private class FakeWorkoutDao(private val records: List<WorkoutSessionEntity>) : WorkoutDao {
        override suspend fun insertWorkout(session: WorkoutSessionEntity): Long = 1L
        override fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>> = flowOf(records)
        override suspend fun getAllWorkoutsList(): List<WorkoutSessionEntity> = records
        override fun getWorkoutsSince(since: Instant): Flow<List<WorkoutSessionEntity>> = flowOf(records)
        override fun getWorkoutsByType(type: WorkoutType): Flow<List<WorkoutSessionEntity>> = flowOf(records)
        override suspend fun getTotalTrimpSince(since: Instant): Double? = null
        override suspend fun getWorkoutById(id: Long): WorkoutSessionEntity? = records.firstOrNull()
        override suspend fun deleteWorkoutById(id: Long) {}
    }

    private class FakeDailyActivityDao(private val records: List<DailyActivityEntity>) : DailyActivityDao {
        override suspend fun insertOrUpdate(entity: DailyActivityEntity): Long = 1L
        override suspend fun insertAll(entities: List<DailyActivityEntity>): List<Long> = emptyList()
        override fun getActivityForDate(date: String, sourceDevice: String): Flow<DailyActivityEntity?> = flowOf(null)
        override suspend fun getActivityForDateSync(date: String, sourceDevice: String): DailyActivityEntity? = null
        override fun getRecentActivities(limit: Int): Flow<List<DailyActivityEntity>> = flowOf(records)
        override fun getAllDailyActivitiesFlow(): Flow<List<DailyActivityEntity>> = flowOf(records)
        override suspend fun getAllDailyActivities(): List<DailyActivityEntity> = records
        override suspend fun count(): Int = records.size
        override suspend fun deleteAll(): Int = 0
    }

    private class FakeHeartRateDao(private val records: List<HeartRateSampleEntity>) : HeartRateDao {
        override suspend fun insert(entity: HeartRateSampleEntity): Long = 1L
        override suspend fun insertAll(entities: List<HeartRateSampleEntity>): List<Long> = emptyList()
        override fun getLatestSample(): Flow<HeartRateSampleEntity?> = flowOf(records.firstOrNull())
        override suspend fun getSamplesInRange(fromTimestamp: Long, toTimestamp: Long): List<HeartRateSampleEntity> = emptyList()
        override suspend fun count(): Int = records.size
        override suspend fun getLatestSampleSync(): HeartRateSampleEntity? = records.firstOrNull()
        override fun getAllSamples(): Flow<List<HeartRateSampleEntity>> = flowOf(records)
        override suspend fun getRecentSamplesList(limit: Int): List<HeartRateSampleEntity> = records
        override suspend fun deleteAll(): Int = 0
    }

    @Test
    fun exportWeightsToCsv_generatesHeadersAndRows() = runTest {
        val fakeWeightDao = FakeWeightDao(
            listOf(
                WeightMeasurementEntity(
                    id = 1L,
                    sourceDevice = "RENPHO",
                    weightKg = 75.4,
                    measuredAtEpochMilli = 1700000000000L,
                    importedAtEpochMilli = 1700000000000L
                )
            )
        )

        val csv = DataBackupManager.exportWeightsToCsv(fakeWeightDao)
        assertTrue(csv.startsWith("id,measuredAtEpochMilli,weightKg"))
        assertTrue(csv.contains("1,1700000000000,75.4"))
    }

    @Test
    fun exportWorkoutsToCsv_generatesValidCsvRows() = runTest {
        val fakeWorkoutDao = FakeWorkoutDao(
            listOf(
                WorkoutSessionEntity(
                    id = 10L,
                    name = "CrossFit Murph",
                    type = WorkoutType.CROSSFIT_WOD,
                    startTime = Instant.parse("2026-11-02T10:00:00Z"),
                    endTime = Instant.parse("2026-11-02T11:00:00Z"),
                    durationMinutes = 60.0,
                    trimp = 85.0,
                    rpe = 8,
                    avgHeartRate = 160,
                    maxHeartRate = 185,
                    caloriesBurned = 600,
                    notes = "Hero WOD"
                )
            )
        )

        val csv = DataBackupManager.exportWorkoutsToCsv(fakeWorkoutDao)
        assertTrue(csv.startsWith("id,name,type"))
        assertTrue(csv.contains("10,CrossFit Murph,CROSSFIT_WOD"))
        assertTrue(csv.contains("85.0"))
    }

    @Test
    fun exportDailyActivityToCsv_containsStepsAndDistance() = runTest {
        val fakeDailyActivityDao = FakeDailyActivityDao(
            listOf(
                DailyActivityEntity(
                    id = 1L,
                    date = "2026-11-02",
                    steps = 8200,
                    distanceMeters = 6100,
                    caloriesBurned = 390,
                    sourceDevice = "Amazfit GTR 3"
                )
            )
        )

        val csv = DataBackupManager.exportDailyActivityToCsv(fakeDailyActivityDao)
        assertTrue(csv.contains("1,2026-11-02,8200,6100,390,Amazfit GTR 3"))
    }

    @Test
    fun exportHeartRateToCsv_containsBpmAndSource() = runTest {
        val fakeHrDao = FakeHeartRateDao(
            listOf(
                HeartRateSampleEntity(
                    id = 1L,
                    sourceDevice = "Amazfit GTR 3",
                    bpm = 72,
                    timestamp = 1700000000000L
                )
            )
        )

        val csv = DataBackupManager.exportHeartRateToCsv(fakeHrDao)
        assertTrue(csv.contains("1,1700000000000,72,Amazfit GTR 3"))
    }

    @Test
    fun exportSleepToCsv_containsStagesAndEfficiency() = runTest {
        val fakeSleepDao = object : com.salud.personal.data.local.dao.SleepSessionDao {
            val records = listOf(
                com.salud.personal.data.local.entity.SleepSessionEntity(
                    id = 1L,
                    date = java.time.LocalDate.of(2026, 11, 2),
                    startTime = Instant.ofEpochMilli(1700000000000L),
                    endTime = Instant.ofEpochMilli(1700028800000L),
                    deepSleepMinutes = 95,
                    lightSleepMinutes = 220,
                    remSleepMinutes = 75,
                    awakeMinutes = 20,
                    totalSleepMinutes = 390,
                    sleepEfficiencyScore = 95,
                    sourceDevice = "Amazfit GTR 3"
                )
            )
            override suspend fun insertOrUpdate(session: com.salud.personal.data.local.entity.SleepSessionEntity): Long = 1L
            override fun observeSleepForDate(date: java.time.LocalDate): Flow<com.salud.personal.data.local.entity.SleepSessionEntity?> = flowOf(records.firstOrNull())
            override suspend fun getSleepForDate(date: java.time.LocalDate): com.salud.personal.data.local.entity.SleepSessionEntity? = records.firstOrNull()
            override fun getRecentSleep(limit: Int): Flow<List<com.salud.personal.data.local.entity.SleepSessionEntity>> = flowOf(records)
            override fun getAllSleep(): Flow<List<com.salud.personal.data.local.entity.SleepSessionEntity>> = flowOf(records)
            override suspend fun getAllSleepList(): List<com.salud.personal.data.local.entity.SleepSessionEntity> = records
            override suspend fun deleteAll(): Int = 0
        }

        val csv = DataBackupManager.exportSleepToCsv(fakeSleepDao)
        assertTrue(csv.startsWith("id,date,startTime,endTime"))
        assertTrue(csv.contains("1,2026-11-02,"))
        assertTrue(csv.contains(",95,220,75,20,390,95,Amazfit GTR 3"))
    }
}
