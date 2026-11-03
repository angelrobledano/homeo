package com.salud.personal.domain.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.salud.personal.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate

object DataExporter {

    suspend fun exportDatabaseToJson(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "Salud Personal")
        root.put("version", "1.0.0")
        root.put("exportedAt", Instant.now().toString())

        // 1. Entrenamientos
        val workouts = database.workoutDao().getAllWorkoutsList()
        val workoutsArr = JSONArray()
        workouts.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("type", w.type.name)
            obj.put("name", w.name)
            obj.put("startTime", w.startTime.toString())
            obj.put("endTime", w.endTime.toString())
            obj.put("durationMinutes", w.durationMinutes)
            obj.put("avgHeartRate", w.avgHeartRate ?: JSONObject.NULL)
            obj.put("maxHeartRate", w.maxHeartRate ?: JSONObject.NULL)
            obj.put("trimp", w.trimp ?: JSONObject.NULL)
            obj.put("caloriesBurned", w.caloriesBurned ?: JSONObject.NULL)
            obj.put("rpe", w.rpe)
            obj.put("notes", w.notes ?: JSONObject.NULL)
            workoutsArr.put(obj)
        }
        root.put("workoutSessions", workoutsArr)

        // 2. Pesajes
        val weights = database.weightDao().getAll()
        val weightsArr = JSONArray()
        weights.forEach { wt ->
            val obj = JSONObject()
            obj.put("id", wt.id)
            obj.put("weightKg", wt.weightKg)
            obj.put("bodyFatPercentage", wt.bodyFatPercentage ?: JSONObject.NULL)
            obj.put("muscleMassKg", wt.muscleMassKg ?: JSONObject.NULL)
            obj.put("measuredAtEpochMilli", wt.measuredAtEpochMilli)
            obj.put("sourceDevice", wt.sourceDevice)
            weightsArr.put(obj)
        }
        root.put("weightMeasurements", weightsArr)

        // 3. Actividad Diaria
        val activities = database.dailyActivityDao().getAllDailyActivities()
        val actArr = JSONArray()
        activities.forEach { a ->
            val obj = JSONObject()
            obj.put("date", a.date)
            obj.put("steps", a.steps)
            obj.put("distanceMeters", a.distanceMeters)
            obj.put("caloriesBurned", a.caloriesBurned)
            obj.put("sourceDevice", a.sourceDevice)
            actArr.put(obj)
        }
        root.put("dailyActivities", actArr)

        // 4. Sueño
        val sleeps = database.sleepSessionDao().getAllSleepList()
        val sleepsArr = JSONArray()
        sleeps.forEach { s ->
            val obj = JSONObject()
            obj.put("date", s.date.toString())
            obj.put("totalSleepMinutes", s.totalSleepMinutes)
            obj.put("deepSleepMinutes", s.deepSleepMinutes)
            obj.put("lightSleepMinutes", s.lightSleepMinutes)
            obj.put("remSleepMinutes", s.remSleepMinutes)
            obj.put("awakeMinutes", s.awakeMinutes)
            obj.put("efficiency", s.sleepEfficiencyScore ?: JSONObject.NULL)
            sleepsArr.put(obj)
        }
        root.put("sleepSessions", sleepsArr)

        // 5. Check-Ins
        val checkIns = database.dailyCheckInDao().getAllCheckInsList()
        val checkInsArr = JSONArray()
        checkIns.forEach { c ->
            val obj = JSONObject()
            obj.put("date", c.date.toString())
            obj.put("energyRating", c.energyRating)
            obj.put("sleepQualityRating", c.sleepQualityRating)
            obj.put("muscleSorenessRating", c.muscleSorenessRating)
            obj.put("readinessRating", c.readinessRating)
            obj.put("restingHeartRate", c.restingHeartRate ?: JSONObject.NULL)
            obj.put("recoveryScore", c.calculatedRecoveryScore ?: JSONObject.NULL)
            checkInsArr.put(obj)
        }
        root.put("dailyCheckIns", checkInsArr)

        val fileName = "salud_personal_backup_${LocalDate.now()}.json"
        val exportFile = File(context.cacheDir, fileName)
        FileOutputStream(exportFile).use { fos ->
            fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        exportFile
    }

    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Copia de Seguridad - Salud Personal")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
