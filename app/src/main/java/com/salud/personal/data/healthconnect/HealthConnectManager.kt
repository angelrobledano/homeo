package com.salud.personal.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.HourlyStepsDao
import com.salud.personal.data.local.dao.SleepSessionDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.DailyActivityEntity
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import com.salud.personal.data.local.entity.HourlyStepsEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

data class HealthConnectSyncSummary(
    val weights: Int = 0,
    val sleepSessions: Int = 0,
    val activityDays: Int = 0,
    val heartRateSamples: Int = 0,
    val workouts: Int = 0
) {
    val total: Int
        get() = weights + sleepSessions + activityDays + heartRateSamples + workouts
}

/**
 * Gestor de integración con Google Health Connect.
 * Sincroniza de forma idempotente las mediciones de salud escritas por aplicaciones externas:
 * - Zepp (Amazfit GTR 3): pasos, sueño, frecuencia cardíaca, entrenamientos.
 * - Renpho: peso y composición corporal.
 */
class HealthConnectManager(
    private val context: Context,
    private val weightRepository: WeightRepository,
    private val sleepSessionDao: SleepSessionDao? = null,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val heartRateDao: HeartRateDao? = null,
    private val workoutDao: WorkoutDao? = null,
    private val hourlyStepsDao: HourlyStepsDao? = null
) {
    val healthConnectClient: HealthConnectClient? by lazy {
        if (isAvailable()) {
            try {
                HealthConnectClient.getOrCreate(context)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun getGrantedPermissions(): Set<String> {
        val client = healthConnectClient ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun hasPermissions(required: Set<String> = permissions): Boolean {
        val granted = getGrantedPermissions()
        return granted.containsAll(required)
    }

    suspend fun hasAnyPermission(): Boolean {
        return getGrantedPermissions().isNotEmpty()
    }

    /**
     * Sincroniza todas las fuentes de datos autorizadas (peso, sueño, pasos, pulso, entrenamientos).
     */
    suspend fun syncAllHealthRecords(
        sincePeriod: Instant = Instant.now().minus(30, ChronoUnit.DAYS)
    ): HealthConnectSyncSummary {
        val granted = getGrantedPermissions()
        var weights = 0
        var sleep = 0
        var activity = 0
        var heartRate = 0
        var workouts = 0

        if (granted.contains(HealthPermission.getReadPermission(WeightRecord::class))) {
            weights = syncWeightRecords(sincePeriod)
        }
        if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
            sleep = syncSleepRecords(sincePeriod)
        }
        if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
            activity = syncActivityRecords(sincePeriod)
        }
        if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) {
            heartRate = syncHeartRateRecords(sincePeriod)
        }
        if (granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) {
            workouts = syncWorkoutRecords(sincePeriod)
        }

        return HealthConnectSyncSummary(
            weights = weights,
            sleepSessions = sleep,
            activityDays = activity,
            heartRateSamples = heartRate,
            workouts = workouts
        )
    }

    /**
     * Sincroniza sesiones de sueño y fases desde Health Connect (Zepp / Amazfit).
     */
    suspend fun syncSleepRecords(
        sincePeriod: Instant = Instant.now().minus(30, ChronoUnit.DAYS)
    ): Int {
        val client = healthConnectClient ?: return 0
        val dao = sleepSessionDao ?: return 0
        val endTime = Instant.now()
        val timeFilter = TimeRangeFilter.between(sincePeriod, endTime)

        val response = try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
        } catch (e: Exception) {
            return 0
        }

        var imported = 0
        val zoneId = ZoneId.systemDefault()

        for (record in response.records) {
            val sessionDate = record.endTime.atZone(zoneId).toLocalDate()

            var deepMinutes = 0
            var lightMinutes = 0
            var remMinutes = 0
            var awakeMinutes = 0

            for (stage in record.stages) {
                val dur = Duration.between(stage.startTime, stage.endTime).toMinutes().toInt()
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deepMinutes += dur
                    SleepSessionRecord.STAGE_TYPE_REM -> remMinutes += dur
                    SleepSessionRecord.STAGE_TYPE_LIGHT,
                    SleepSessionRecord.STAGE_TYPE_SLEEPING -> lightMinutes += dur
                    SleepSessionRecord.STAGE_TYPE_AWAKE,
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awakeMinutes += dur
                }
            }

            val totalStagesSleep = deepMinutes + lightMinutes + remMinutes
            val rawDuration = Duration.between(record.startTime, record.endTime).toMinutes().toInt()
            val finalTotalSleep = if (totalStagesSleep > 0) totalStagesSleep else (rawDuration - awakeMinutes).coerceAtLeast(0)

            if (finalTotalSleep < 30) continue // Descartar micro-siestas inválidas

            val efficiency = if (finalTotalSleep + awakeMinutes > 0) {
                ((finalTotalSleep.toDouble() / (finalTotalSleep + awakeMinutes)) * 100).roundToInt().coerceIn(0, 100)
            } else 85

            val pkg = record.metadata.dataOrigin.packageName
            val sourceLabel = if (pkg.contains("huami") || pkg.contains("amazfit")) "Zepp (Amazfit GTR 3)" else "Health Connect"

            val entity = SleepSessionEntity(
                date = sessionDate,
                startTime = record.startTime,
                endTime = record.endTime,
                deepSleepMinutes = deepMinutes,
                lightSleepMinutes = if (lightMinutes == 0 && deepMinutes == 0 && remMinutes == 0) finalTotalSleep else lightMinutes,
                remSleepMinutes = remMinutes,
                awakeMinutes = awakeMinutes,
                totalSleepMinutes = finalTotalSleep,
                sleepEfficiencyScore = efficiency,
                sourceDevice = sourceLabel
            )

            dao.insertOrUpdate(entity)
            imported++
        }

        return imported
    }

    /**
     * Sincroniza pasos, distancia y calorías diarias desde Health Connect (Zepp / Amazfit).
     * También desglosa y almacena los pasos agrupados por hora en [HourlyStepsDao].
     */
    suspend fun syncActivityRecords(
        sincePeriod: Instant = Instant.now().minus(30, ChronoUnit.DAYS)
    ): Int {
        val client = healthConnectClient ?: return 0
        if (dailyActivityDao == null && hourlyStepsDao == null) return 0
        val endTime = Instant.now()
        val timeFilter = TimeRangeFilter.between(sincePeriod, endTime)

        val stepsRecords = try {
            readAllRecords(client, StepsRecord::class, timeFilter)
        } catch (e: Exception) {
            return 0
        }

        val distanceRecords = try {
            readAllRecords(client, DistanceRecord::class, timeFilter)
        } catch (e: Exception) {
            emptyList()
        }

        val caloriesRecords = try {
            readAllRecords(client, TotalCaloriesBurnedRecord::class, timeFilter)
        } catch (e: Exception) {
            emptyList()
        }

        val zoneId = ZoneId.systemDefault()
        val stepsByDate = stepsRecords.groupBy { it.endTime.atZone(zoneId).toLocalDate() }
        val distByDate = distanceRecords.groupBy { it.endTime.atZone(zoneId).toLocalDate() }
        val calByDate = caloriesRecords.groupBy { it.endTime.atZone(zoneId).toLocalDate() }

        var imported = 0

        if (dailyActivityDao != null) {
            for ((date, allDateRecords) in stepsByDate) {
                val hasZepp = allDateRecords.any {
                    val pkg = it.metadata.dataOrigin.packageName.lowercase()
                    pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")
                }
                val records = if (hasZepp) {
                    allDateRecords.filter {
                        val pkg = it.metadata.dataOrigin.packageName.lowercase()
                        pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")
                    }
                } else {
                    allDateRecords
                }

                val totalSteps = records.sumOf { it.count }.toInt()
                val dateDistRecords = distByDate[date]?.let { dists ->
                    if (hasZepp) dists.filter {
                        val pkg = it.metadata.dataOrigin.packageName.lowercase()
                        pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")
                    } else dists
                }
                val dateCalRecords = calByDate[date]?.let { cals ->
                    if (hasZepp) cals.filter {
                        val pkg = it.metadata.dataOrigin.packageName.lowercase()
                        pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")
                    } else cals
                }

                val totalDistance = dateDistRecords?.sumOf { it.distance.inMeters.toInt() }?.takeIf { it > 0 } ?: (totalSteps * 0.75).toInt()
                val totalCalories = dateCalRecords?.sumOf { it.energy.inKilocalories.toInt() }?.takeIf { it > 0 } ?: (totalSteps * 0.04).toInt()

                val deviceLabel = if (hasZepp) "Amazfit GTR 3" else "Health Connect"

                dailyActivityDao.insertOrUpdate(
                    DailyActivityEntity(
                        date = date.toString(),
                        steps = totalSteps,
                        distanceMeters = totalDistance,
                        caloriesBurned = totalCalories,
                        sourceDevice = deviceLabel
                    )
                )
                imported++
            }
        }

        if (hourlyStepsDao != null && stepsRecords.isNotEmpty()) {
            val hourlyEntities = aggregateHourlySteps(stepsRecords, zoneId)
            if (hourlyEntities.isNotEmpty()) {
                hourlyStepsDao.insertOrUpdateAll(hourlyEntities)
            }
        }

        return imported
    }

    private suspend fun <T : androidx.health.connect.client.records.Record> readAllRecords(
        client: HealthConnectClient,
        recordType: kotlin.reflect.KClass<T>,
        timeFilter: TimeRangeFilter
    ): List<T> {
        val allRecords = mutableListOf<T>()
        var pageToken: String? = null
        var pagesRead = 0
        do {
            val request = ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = timeFilter,
                pageToken = pageToken
            )
            val response = client.readRecords(request)
            allRecords.addAll(response.records)
            pageToken = response.pageToken
            pagesRead++
        } while (pageToken != null && pagesRead < 50)
        return allRecords
    }

    /**
     * Sincroniza muestras de frecuencia cardíaca continua desde Health Connect (Zepp / Amazfit).
     */
    suspend fun syncHeartRateRecords(
        sincePeriod: Instant = Instant.now().minus(7, ChronoUnit.DAYS)
    ): Int {
        val client = healthConnectClient ?: return 0
        val dao = heartRateDao ?: return 0
        val endTime = Instant.now()
        val timeFilter = TimeRangeFilter.between(sincePeriod, endTime)

        val response = try {
            client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeFilter))
        } catch (e: Exception) {
            return 0
        }

        val entities = mutableListOf<HeartRateSampleEntity>()

        for (record in response.records) {
            val pkg = record.metadata.dataOrigin.packageName
            val deviceLabel = if (pkg.contains("huami") || pkg.contains("amazfit")) "Amazfit GTR 3" else "Health Connect"

            for (sample in record.samples) {
                val bpm = sample.beatsPerMinute.toInt()
                if (bpm in 35..230) {
                    entities.add(
                        HeartRateSampleEntity(
                            timestamp = sample.time.toEpochMilli(),
                            bpm = bpm,
                            sourceDevice = deviceLabel
                        )
                    )
                }
            }
        }

        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }

        return entities.size
    }

    /**
     * Sincroniza sesiones de entrenamiento desde Health Connect (Zepp / Amazfit).
     */
    suspend fun syncWorkoutRecords(
        sincePeriod: Instant = Instant.now().minus(30, ChronoUnit.DAYS)
    ): Int {
        val client = healthConnectClient ?: return 0
        val dao = workoutDao ?: return 0
        val endTime = Instant.now()
        val timeFilter = TimeRangeFilter.between(sincePeriod, endTime)

        val response = try {
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, timeFilter))
        } catch (e: Exception) {
            return 0
        }

        val existingWorkouts = dao.getAllWorkoutsList().toMutableList()
        var imported = 0

        for (record in response.records) {
            val durationMin = Duration.between(record.startTime, record.endTime).toSeconds() / 60.0
            if (durationMin < 2.0) continue

            // Deduplicación en memoria: no insertar si ya existe un entreno a ±5 minutos
            val exists = existingWorkouts.any {
                abs(Duration.between(it.startTime, record.startTime).seconds) < 300
            }
            if (exists) continue

            val (workoutType, workoutName) = mapExerciseRecord(record.exerciseType, record.title)

            // Filtrar caminatas automáticas menores de 15 min (pertenecen a pasos diarios, no a sesiones de entrenamiento)
            if (workoutType == WorkoutType.WALK && durationMin < 15.0) continue

            var avgHr: Int? = null
            var maxHr: Int? = null
            var calories: Int? = null

            // Extraer frecuencia cardíaca real durante el entreno si está en Health Connect
            try {
                val hrResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                    )
                )
                val bpms = hrResponse.records.flatMap { it.samples }.map { it.beatsPerMinute.toInt() }.filter { it in 40..220 }
                if (bpms.isNotEmpty()) {
                    avgHr = bpms.average().toInt()
                    maxHr = bpms.maxOrNull()
                }
            } catch (_: Exception) { }

            // Extraer calorías quemadas totales durante el intervalo si existen
            try {
                val calResponse = client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(record.startTime, record.endTime)
                    )
                )
                val totalKcal = calResponse.records.sumOf { it.energy.inKilocalories }.toInt()
                if (totalKcal > 0) {
                    calories = totalKcal
                }
            } catch (_: Exception) { }

            val trimp = if (avgHr != null && avgHr > 60) {
                com.salud.personal.domain.training.TrainingLoadCalculator.calculateBanisterTrimp(
                    durationMinutes = durationMin,
                    avgHr = avgHr.toDouble(),
                    restHr = 60.0,
                    maxHr = (maxHr ?: 190).toDouble()
                )
            } else {
                durationMin * when (workoutType) {
                    WorkoutType.CROSSFIT_WOD -> 2.2
                    WorkoutType.STRENGTH -> 1.8
                    WorkoutType.ENDURANCE_CARDIO -> 2.0
                    WorkoutType.WALK -> 1.0
                    WorkoutType.RECOVERY -> 0.8
                }
            }

            val entity = WorkoutSessionEntity(
                type = workoutType,
                name = workoutName,
                startTime = record.startTime,
                endTime = record.endTime,
                durationMinutes = durationMin,
                avgHeartRate = avgHr,
                maxHeartRate = maxHr,
                trimp = trimp,
                caloriesBurned = calories,
                rpe = when (workoutType) {
                    WorkoutType.CROSSFIT_WOD -> 8
                    WorkoutType.STRENGTH -> 7
                    WorkoutType.ENDURANCE_CARDIO -> 7
                    WorkoutType.WALK -> 4
                    WorkoutType.RECOVERY -> 3
                },
                notes = "Sincronizado vía Health Connect (Zepp)"
            )

            val insertedId = dao.insertWorkout(entity)
            existingWorkouts.add(entity.copy(id = insertedId))
            imported++
        }

        cleanDuplicateWorkouts()
        return imported
    }

    /**
     * Sanea la base de datos eliminando entrenamientos duplicados a ±5 minutos y
     * reclasificando actividades genéricas Zepp a Crosstraining.
     */
    suspend fun cleanDuplicateWorkouts(): Int {
        val dao = workoutDao ?: return 0
        val all = dao.getAllWorkoutsList()
        val toDelete = mutableListOf<Long>()

        for (i in all.indices) {
            val w1 = all[i]
            if (toDelete.contains(w1.id)) continue

            for (j in (i + 1) until all.size) {
                val w2 = all[j]
                if (toDelete.contains(w2.id)) continue

                if (abs(Duration.between(w1.startTime, w2.startTime).seconds) < 300) {
                    // Conflicto duplicado: conservar el de mayor especificidad o con FC
                    val keeper = if (w1.type == WorkoutType.CROSSFIT_WOD && w2.type != WorkoutType.CROSSFIT_WOD) {
                        w1
                    } else if (w2.type == WorkoutType.CROSSFIT_WOD && w1.type != WorkoutType.CROSSFIT_WOD) {
                        w2
                    } else if ((w1.avgHeartRate ?: 0) >= (w2.avgHeartRate ?: 0)) {
                        w1
                    } else {
                        w2
                    }
                    val discard = if (keeper == w1) w2 else w1
                    toDelete.add(discard.id)
                }
            }
        }

        for (id in toDelete) {
            dao.deleteWorkoutById(id)
        }

        // Actualizar nombres y tipos de actividades genéricas que son crosstraining
        val remaining = dao.getAllWorkoutsList()
        for (w in remaining) {
            if (w.type == WorkoutType.WALK && w.durationMinutes < 15.0) {
                dao.deleteWorkoutById(w.id)
            } else if (w.name == "Actividad Zepp" && w.durationMinutes >= 20.0) {
                dao.insertWorkout(
                    w.copy(
                        type = WorkoutType.CROSSFIT_WOD,
                        name = "Crosstraining Zepp",
                        rpe = 8,
                        trimp = if ((w.trimp ?: 0.0) < 100.0) w.durationMinutes * 2.2 else w.trimp
                    )
                )
            }
        }

        // Limpiar pasos duplicados del sensor del teléfono cuando existan mediciones del reloj Amazfit
        try {
            hourlyStepsDao?.cleanDuplicatePhoneSteps()
        } catch (_: Exception) { }

        return toDelete.size
    }

    /**
     * Sincroniza lecturas de peso desde Health Connect (Renpho).
     */
    suspend fun syncWeightRecords(
        sincePeriod: Instant = Instant.now().minus(90, ChronoUnit.DAYS)
    ): Int {
        val client = healthConnectClient ?: return 0
        val endTime = Instant.now()
        val timeFilter = TimeRangeFilter.between(sincePeriod, endTime)

        val weightResponse = try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
        } catch (e: Exception) {
            return 0
        }

        val bodyFatResponse = try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
        } catch (e: Exception) {
            null
        }

        var newImportedCount = 0

        for (record in weightResponse.records) {
            val weightKg = record.weight.inKilograms
            if (weightKg !in 10.0..300.0) continue

            val matchingFat = bodyFatResponse?.records?.minByOrNull {
                abs(Duration.between(it.time, record.time).seconds)
            }?.takeIf {
                abs(Duration.between(it.time, record.time).seconds) <= 600
            }

            val fatPct = matchingFat?.percentage?.value?.toFloat()?.coerceIn(3.0f, 70.0f)

            val measurement = WeightMeasurement(
                source = ProvenanceSource.HEALTH_CONNECT,
                externalId = record.metadata.id,
                weightKg = round(weightKg * 100.0) / 100.0,
                bodyFatPercentage = fatPct,
                measuredAt = record.time,
                importedAt = Instant.now()
            )

            val saved = weightRepository.saveMeasurement(measurement)
            if (saved) {
                newImportedCount++
            }
        }

        return newImportedCount
    }

    companion object {
        /**
         * Mapea un ExerciseSessionRecord de Health Connect a WorkoutType y nombre en la app.
         * Detecta de forma robusta crosstraining, bootcamp, funcional, fuerza y cardio.
         */
        fun mapExerciseRecord(exerciseType: Int, rawTitle: String?): Pair<WorkoutType, String> {
            val cleanTitle = rawTitle?.trim()?.takeIf { it.isNotBlank() }
            val lowerTitle = cleanTitle?.lowercase() ?: ""

            // 1. Detección semántica por palabras clave en título
            if (lowerTitle.contains("cross") || lowerTitle.contains("wod") || lowerTitle.contains("bootcamp") || lowerTitle.contains("funcional") || lowerTitle.contains("hiit")) {
                return Pair(WorkoutType.CROSSFIT_WOD, cleanTitle ?: "Crosstraining Zepp")
            }
            if (lowerTitle.contains("fuerza") || lowerTitle.contains("pesas") || lowerTitle.contains("gym") || lowerTitle.contains("strength")) {
                return Pair(WorkoutType.STRENGTH, cleanTitle ?: "Fuerza Zepp")
            }
            if (lowerTitle.contains("correr") || lowerTitle.contains("running") || lowerTitle.contains("bici") || lowerTitle.contains("ciclismo") || lowerTitle.contains("nadar") || lowerTitle.contains("natación")) {
                return Pair(WorkoutType.ENDURANCE_CARDIO, cleanTitle ?: "Cardio Zepp")
            }
            if (lowerTitle.contains("caminata") || lowerTitle.contains("walk") || lowerTitle.contains("paseo")) {
                return Pair(WorkoutType.WALK, cleanTitle ?: "Caminata Zepp")
            }
            if (lowerTitle.contains("yoga") || lowerTitle.contains("estiramiento") || lowerTitle.contains("movilidad") || lowerTitle.contains("pilates")) {
                return Pair(WorkoutType.RECOVERY, cleanTitle ?: "Movilidad / Yoga")
            }

            // 2. Mapeo exhaustivo por constante de Health Connect
            return when (exerciseType) {
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING ->
                    Pair(WorkoutType.WALK, cleanTitle ?: "Caminata Zepp")

                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
                ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE ->
                    Pair(WorkoutType.ENDURANCE_CARDIO, cleanTitle ?: "Cardio Zepp")

                ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP,
                ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS ->
                    Pair(WorkoutType.CROSSFIT_WOD, cleanTitle ?: "Crosstraining Zepp")

                ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS ->
                    Pair(WorkoutType.CROSSFIT_WOD, cleanTitle ?: "Calistenia Zepp")

                ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING ->
                    Pair(WorkoutType.CROSSFIT_WOD, cleanTitle ?: "HIIT Zepp")

                ExerciseSessionRecord.EXERCISE_TYPE_BOXING,
                ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS ->
                    Pair(WorkoutType.CROSSFIT_WOD, cleanTitle ?: "Artes Marciales / WOD")

                ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING ->
                    Pair(WorkoutType.STRENGTH, cleanTitle ?: "Fuerza Zepp")

                ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
                ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
                ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING ->
                    Pair(WorkoutType.RECOVERY, cleanTitle ?: "Movilidad / Estiramientos")

                else -> {
                    // EXERCISE_TYPE_OTHER_WORKOUT (0) o genérico
                    if (cleanTitle != null && cleanTitle != "Otros") {
                        Pair(WorkoutType.CROSSFIT_WOD, cleanTitle)
                    } else {
                        Pair(WorkoutType.CROSSFIT_WOD, "Crosstraining / WOD")
                    }
                }
            }
        }

        /**
         * Agrupa registros [StepsRecord] en entidades [HourlyStepsEntity] acumulando pasos por hora del día.
         * Determinista y desacoplado del cliente Health Connect para facilitar pruebas unitarias.
         */
        fun aggregateHourlySteps(
            records: List<StepsRecord>,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): List<HourlyStepsEntity> {
            // Si existen registros de Zepp/Amazfit, priorizarlos sobre el sensor del móvil para evitar duplicación
            val hasZepp = records.any {
                val pkg = it.metadata.dataOrigin.packageName.lowercase()
                pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")
            }
            val filteredRecords = if (hasZepp) {
                records.filter {
                    val pkg = it.metadata.dataOrigin.packageName.lowercase()
                    pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")
                }
            } else {
                records
            }

            val hourlyMap = mutableMapOf<Triple<LocalDate, Int, String>, Int>()
            for (record in filteredRecords) {
                val zonedDateTime = record.startTime.atZone(zoneId)
                val date = zonedDateTime.toLocalDate()
                val hour = zonedDateTime.hour
                val pkg = record.metadata.dataOrigin.packageName.lowercase()
                val deviceLabel = if (pkg.contains("huami") || pkg.contains("amazfit") || pkg.contains("zepp")) {
                    "Amazfit GTR 3"
                } else {
                    "Health Connect"
                }
                val key = Triple(date, hour, deviceLabel)
                hourlyMap[key] = (hourlyMap[key] ?: 0) + record.count.toInt()
            }
            return hourlyMap.map { (key, steps) ->
                HourlyStepsEntity(
                    date = key.first,
                    hour = key.second,
                    steps = steps,
                    sourceDevice = key.third
                )
            }.sortedWith(compareBy({ it.date }, { it.hour }))
        }
    }
}
