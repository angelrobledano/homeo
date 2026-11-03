package com.salud.personal.data.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.WeightRecord
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.repository.WeightDaoInterface
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManagerTest {

    private class FakeWeightDao : WeightDaoInterface {
        val list = mutableListOf<WeightMeasurementEntity>()
        val flow = MutableStateFlow<List<WeightMeasurementEntity>>(emptyList())

        override suspend fun insert(entity: WeightMeasurementEntity): Long {
            list.add(entity)
            flow.value = list.toList()
            return list.size.toLong()
        }

        override suspend fun getLatest(): WeightMeasurementEntity? = list.lastOrNull()
        override suspend fun getAll(): List<WeightMeasurementEntity> = list.toList()
        override fun observeLatest(): Flow<WeightMeasurementEntity?> = flowOf(list.lastOrNull())
        override fun observeAll(): Flow<List<WeightMeasurementEntity>> = flow
    }

    private lateinit var fakeDao: FakeWeightDao
    private lateinit var repository: WeightRepository

    @Before
    fun setUp() {
        fakeDao = FakeWeightDao()
        repository = WeightRepository(fakeDao)
    }

    @Test
    fun permissions_containsWeightAndBodyFat() {
        val expectedWeight = HealthPermission.getReadPermission(WeightRecord::class)
        val expectedBodyFat = HealthPermission.getReadPermission(BodyFatRecord::class)

        val set = setOf(expectedWeight, expectedBodyFat)
        assertTrue(set.contains("android.permission.health.READ_WEIGHT"))
        assertTrue(set.contains("android.permission.health.READ_BODY_FAT"))
    }

    @Test
    fun permissions_containsZeppHealthMetrics() {
        val permissions = setOf(
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.StepsRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.SleepSessionRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.HeartRateRecord::class),
            HealthPermission.getReadPermission(androidx.health.connect.client.records.ExerciseSessionRecord::class)
        )

        assertTrue(permissions.contains("android.permission.health.READ_STEPS"))
        assertTrue(permissions.contains("android.permission.health.READ_SLEEP"))
        assertTrue(permissions.contains("android.permission.health.READ_HEART_RATE"))
        assertTrue(permissions.contains("android.permission.health.READ_EXERCISE"))
    }

    @Test
    fun syncSummary_calculatesTotalAccurately() {
        val summary = HealthConnectSyncSummary(
            weights = 2,
            sleepSessions = 7,
            activityDays = 14,
            heartRateSamples = 500,
            workouts = 4
        )
        assertEquals(527, summary.total)
    }

    @Test
    fun weightMeasurement_preservesExternalIdAndSource() = runTest {
        val now = Instant.now()
        val measurement = WeightMeasurement(
            source = ProvenanceSource.HEALTH_CONNECT,
            externalId = "hc-uuid-12345",
            weightKg = 74.8,
            bodyFatPercentage = 16.2f,
            measuredAt = now,
            importedAt = now
        )

        val saved = repository.saveMeasurement(measurement)
        assertTrue(saved)

        val latest = repository.getLatestMeasurement()
        assertEquals(ProvenanceSource.HEALTH_CONNECT, latest?.source)
        assertEquals("hc-uuid-12345", latest?.externalId)
        assertEquals(74.8, latest?.weightKg ?: 0.0, 0.01)
        assertEquals(16.2f, latest?.bodyFatPercentage ?: 0.0f, 0.01f)
    }

    @Test
    fun weightMeasurement_filtersDuplicatesWithinWindow() = runTest {
        val now = Instant.now()
        val m1 = WeightMeasurement(
            source = ProvenanceSource.HEALTH_CONNECT,
            externalId = "hc-uuid-1",
            weightKg = 75.0,
            measuredAt = now,
            importedAt = now
        )
        val m2 = WeightMeasurement(
            source = ProvenanceSource.HEALTH_CONNECT,
            externalId = "hc-uuid-2",
            weightKg = 75.0,
            measuredAt = now.plusSeconds(5),
            importedAt = now
        )

        assertTrue(repository.saveMeasurement(m1))
        // Duplicate weight within 10s from same source should be rejected
        assertFalse(repository.saveMeasurement(m2))
    }

    @Test
    fun mapExerciseRecord_bootcampAndExerciseClass_mapsToCrossfitWod() {
        val (typeBootcamp, nameBootcamp) = HealthConnectManager.mapExerciseRecord(
            exerciseType = androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP,
            rawTitle = null
        )
        assertEquals(com.salud.personal.data.local.entity.WorkoutType.CROSSFIT_WOD, typeBootcamp)
        assertEquals("Crosstraining Zepp", nameBootcamp)

        val (typeClass, nameClass) = HealthConnectManager.mapExerciseRecord(
            exerciseType = androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            rawTitle = null
        )
        assertEquals(com.salud.personal.data.local.entity.WorkoutType.CROSSFIT_WOD, typeClass)
        assertEquals("Crosstraining Zepp", nameClass)
    }

    @Test
    fun mapExerciseRecord_titleWithCrosstraining_mapsToCrossfitWod() {
        val (type, name) = HealthConnectManager.mapExerciseRecord(
            exerciseType = 0,
            rawTitle = "Crosstraining WOD Fran"
        )
        assertEquals(com.salud.personal.data.local.entity.WorkoutType.CROSSFIT_WOD, type)
        assertEquals("Crosstraining WOD Fran", name)
    }

    @Test
    fun mapExerciseRecord_standardSports_mapsAccurately() {
        val (walkType, walkName) = HealthConnectManager.mapExerciseRecord(
            exerciseType = androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            rawTitle = null
        )
        assertEquals(com.salud.personal.data.local.entity.WorkoutType.WALK, walkType)
        assertEquals("Caminata Zepp", walkName)

        val (strType, strName) = HealthConnectManager.mapExerciseRecord(
            exerciseType = androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
            rawTitle = null
        )
        assertEquals(com.salud.personal.data.local.entity.WorkoutType.STRENGTH, strType)
        assertEquals("Fuerza Zepp", strName)
    }

    @Test
    fun aggregateHourlySteps_combinesStepsInSameHourAndSeparatesDifferentHours() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val baseDate = LocalDate.of(2026, 11, 3)
        val time10_15 = baseDate.atTime(10, 15).atZone(zoneId).toInstant()
        val time10_45 = baseDate.atTime(10, 45).atZone(zoneId).toInstant()
        val time11_20 = baseDate.atTime(11, 20).atZone(zoneId).toInstant()

        val r1 = androidx.health.connect.client.records.StepsRecord(
            startTime = time10_15,
            startZoneOffset = null,
            endTime = time10_15.plusSeconds(300),
            endZoneOffset = null,
            count = 450,
            metadata = androidx.health.connect.client.records.metadata.Metadata(
                dataOrigin = androidx.health.connect.client.records.metadata.DataOrigin("com.huami.midong")
            )
        )
        val r2 = androidx.health.connect.client.records.StepsRecord(
            startTime = time10_45,
            startZoneOffset = null,
            endTime = time10_45.plusSeconds(300),
            endZoneOffset = null,
            count = 350,
            metadata = androidx.health.connect.client.records.metadata.Metadata(
                dataOrigin = androidx.health.connect.client.records.metadata.DataOrigin("com.huami.midong")
            )
        )
        val r3 = androidx.health.connect.client.records.StepsRecord(
            startTime = time11_20,
            startZoneOffset = null,
            endTime = time11_20.plusSeconds(300),
            endZoneOffset = null,
            count = 1200,
            metadata = androidx.health.connect.client.records.metadata.Metadata(
                dataOrigin = androidx.health.connect.client.records.metadata.DataOrigin("com.huami.midong")
            )
        )

        val aggregated = HealthConnectManager.aggregateHourlySteps(listOf(r1, r2, r3), zoneId)
        assertEquals(2, aggregated.size)

        val h10 = aggregated.first { it.hour == 10 }
        assertEquals(800, h10.steps)
        assertEquals(baseDate, h10.date)
        assertEquals("Amazfit GTR 3", h10.sourceDevice)

        val h11 = aggregated.first { it.hour == 11 }
        assertEquals(1200, h11.steps)
        assertEquals(baseDate, h11.date)
        assertEquals("Amazfit GTR 3", h11.sourceDevice)
    }

    @Test
    fun aggregateHourlySteps_emptyList_returnsEmpty() {
        val aggregated = HealthConnectManager.aggregateHourlySteps(emptyList())
        assertTrue(aggregated.isEmpty())
    }

    @Test
    fun aggregateHourlySteps_prioritizesWearableOverPhonePedometer() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val baseDate = LocalDate.of(2026, 11, 3)
        val time10_00 = baseDate.atTime(10, 0).atZone(zoneId).toInstant()

        val rZepp = androidx.health.connect.client.records.StepsRecord(
            startTime = time10_00,
            startZoneOffset = null,
            endTime = time10_00.plusSeconds(1800),
            endZoneOffset = null,
            count = 3604,
            metadata = androidx.health.connect.client.records.metadata.Metadata(
                dataOrigin = androidx.health.connect.client.records.metadata.DataOrigin("com.huami.midong")
            )
        )
        val rPhone = androidx.health.connect.client.records.StepsRecord(
            startTime = time10_00,
            startZoneOffset = null,
            endTime = time10_00.plusSeconds(1800),
            endZoneOffset = null,
            count = 4148,
            metadata = androidx.health.connect.client.records.metadata.Metadata(
                dataOrigin = androidx.health.connect.client.records.metadata.DataOrigin("com.google.android.apps.fitness")
            )
        )

        val aggregated = HealthConnectManager.aggregateHourlySteps(listOf(rZepp, rPhone), zoneId)
        assertEquals(1, aggregated.size)
        assertEquals(3604, aggregated[0].steps)
        assertEquals("Amazfit GTR 3", aggregated[0].sourceDevice)
    }
}
