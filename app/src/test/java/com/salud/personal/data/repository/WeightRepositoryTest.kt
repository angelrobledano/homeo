package com.salud.personal.data.repository

import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.toDomain
import com.salud.personal.data.local.entity.toEntity
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class WeightRepositoryTest {

    private class FakeWeightDao : WeightDaoInterface {
        private val records = mutableListOf<WeightMeasurementEntity>()
        private var currentId = 1L

        override suspend fun insert(entity: WeightMeasurementEntity): Long {
            val assignedId = if (entity.id == 0L) currentId++ else entity.id
            val storedEntity = entity.copy(id = assignedId)
            records.removeIf { it.id == assignedId }
            records.add(storedEntity)
            return assignedId
        }

        override suspend fun getLatest(): WeightMeasurementEntity? {
            return records.maxByOrNull { it.measuredAtEpochMilli }
        }

        override suspend fun getAll(): List<WeightMeasurementEntity> {
            return records.sortedByDescending { it.measuredAtEpochMilli }
        }

        override fun observeLatest(): kotlinx.coroutines.flow.Flow<WeightMeasurementEntity?> {
            return kotlinx.coroutines.flow.flowOf(records.maxByOrNull { it.measuredAtEpochMilli })
        }

        override fun observeAll(): kotlinx.coroutines.flow.Flow<List<WeightMeasurementEntity>> {
            return kotlinx.coroutines.flow.flowOf(records.sortedByDescending { it.measuredAtEpochMilli })
        }
    }

    private lateinit var fakeDao: FakeWeightDao
    private lateinit var repository: WeightRepository

    private val baseTime = Instant.parse("2026-10-27T08:00:00Z")

    @Before
    fun setUp() {
        fakeDao = FakeWeightDao()
        repository = WeightRepository(fakeDao)
    }

    @Test
    fun insertMeasurement_savesSuccessfully() = runTest {
        val measurement = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            externalId = "scale-001",
            weightKg = 75.4,
            bodyFatPercentage = 18.5f,
            muscleMassKg = 58.2,
            impedanceOhms = 480.0,
            measuredAt = baseTime,
            importedAt = baseTime.plusSeconds(1)
        )

        val saved = repository.saveMeasurement(measurement)

        assertTrue("La primera medición debe guardarse exitosamente", saved)
        val latest = repository.getLatestMeasurement()
        assertNotNull("Debe existir una última medición", latest)
        assertEquals(75.4, latest!!.weightKg, 0.001)
        assertEquals(ProvenanceSource.RENPHO_ELIS_1, latest.source)
        assertEquals(1, repository.getAllMeasurements().size)
    }

    @Test
    fun duplicateMeasurementWithin10Seconds_isIgnored() = runTest {
        val first = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.4,
            measuredAt = baseTime,
            importedAt = baseTime
        )
        val secondDuplicate = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.42, // Diferencia de 0.02 kg (< 0.1 kg)
            measuredAt = baseTime.plusSeconds(5), // 5 segundos después (<= 10s)
            importedAt = baseTime.plusSeconds(5)
        )

        val firstSaved = repository.saveMeasurement(first)
        val secondSaved = repository.saveMeasurement(secondDuplicate)

        assertTrue(firstSaved)
        assertFalse("La pesada duplicada dentro de 10s debe ser ignorada", secondSaved)
        val all = repository.getAllMeasurements()
        assertEquals(1, all.size)
        assertEquals(75.4, all[0].weightKg, 0.001)
    }

    @Test
    fun measurementAfter10Seconds_isSaved() = runTest {
        val first = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.4,
            measuredAt = baseTime,
            importedAt = baseTime
        )
        val secondAfterWindow = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.4,
            measuredAt = baseTime.plusSeconds(15), // 15 segundos después (> 10s)
            importedAt = baseTime.plusSeconds(15)
        )

        val firstSaved = repository.saveMeasurement(first)
        val secondSaved = repository.saveMeasurement(secondAfterWindow)

        assertTrue(firstSaved)
        assertTrue("La medición posterior a 10s debe guardarse", secondSaved)
        val all = repository.getAllMeasurements()
        assertEquals(2, all.size)
    }

    @Test
    fun measurementWithDifferentWeightWithin10Seconds_isSaved() = runTest {
        val first = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.4,
            measuredAt = baseTime,
            importedAt = baseTime
        )
        val secondDifferentWeight = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.55, // Diferencia de 0.15 kg (>= 0.1 kg)
            measuredAt = baseTime.plusSeconds(4), // Dentro de los 10 segundos
            importedAt = baseTime.plusSeconds(4)
        )

        val firstSaved = repository.saveMeasurement(first)
        val secondSaved = repository.saveMeasurement(secondDifferentWeight)

        assertTrue(firstSaved)
        assertTrue("Medición con peso diferente (>= 0.1kg) debe guardarse", secondSaved)
        val all = repository.getAllMeasurements()
        assertEquals(2, all.size)
    }

    @Test
    fun measurementWithDifferentSourceWithin10Seconds_isSaved() = runTest {
        val first = WeightMeasurement(
            source = ProvenanceSource.RENPHO_ELIS_1,
            weightKg = 75.4,
            measuredAt = baseTime,
            importedAt = baseTime
        )
        val secondDifferentSource = WeightMeasurement(
            source = ProvenanceSource.MANUAL_INPUT,
            weightKg = 75.4,
            measuredAt = baseTime.plusSeconds(2),
            importedAt = baseTime.plusSeconds(2)
        )

        val firstSaved = repository.saveMeasurement(first)
        val secondSaved = repository.saveMeasurement(secondDifferentSource)

        assertTrue(firstSaved)
        assertTrue("Medición con distinta fuente no debe considerarse duplicada", secondSaved)
        val all = repository.getAllMeasurements()
        assertEquals(2, all.size)
    }

    @Test
    fun entityMapping_toDomainAndToEntity_preservesAllFields() {
        val domain = WeightMeasurement(
            id = 42L,
            source = ProvenanceSource.RENPHO_ELIS_1,
            externalId = "ext-999",
            weightKg = 72.8,
            bodyFatPercentage = 16.5f,
            muscleMassKg = 57.0,
            impedanceOhms = 512.0,
            measuredAt = baseTime,
            importedAt = baseTime.plusSeconds(2)
        )

        val entity = domain.toEntity()
        assertEquals(42L, entity.id)
        assertEquals("RENPHO_ELIS_1", entity.sourceDevice)
        assertEquals("ext-999", entity.externalId)
        assertEquals(72.8, entity.weightKg, 0.001)
        assertEquals(16.5f, entity.bodyFatPercentage)
        assertEquals(57.0, entity.muscleMassKg!!, 0.001)
        assertEquals(512.0, entity.impedanceOhms!!, 0.001)
        assertEquals(baseTime.toEpochMilli(), entity.measuredAtEpochMilli)
        assertEquals(baseTime.plusSeconds(2).toEpochMilli(), entity.importedAtEpochMilli)

        val mappedBack = entity.toDomain()
        assertEquals(domain, mappedBack)
    }
}
