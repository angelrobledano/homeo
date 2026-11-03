package com.salud.personal.data.backup

import android.content.Context
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.data.local.dao.DailyActivityDao
import com.salud.personal.data.local.dao.HeartRateDao
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.repository.WeightDaoInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.StringWriter

object DataBackupManager {

    /**
     * Exporta las mediciones de peso de la báscula Renpho a formato CSV.
     */
    suspend fun exportWeightsToCsv(weightDao: WeightDaoInterface): String = withContext(Dispatchers.IO) {
        val measurements = weightDao.getAll()
        val writer = StringWriter()
        writer.append("id,measuredAtEpochMilli,weightKg,bodyFatPercentage,muscleMassKg,impedanceOhms,source\n")
        for (m in measurements) {
            writer.append("${m.id},${m.measuredAtEpochMilli},${m.weightKg},${m.bodyFatPercentage ?: ""},${m.muscleMassKg ?: ""},${m.impedanceOhms ?: ""},${m.sourceDevice}\n")
        }
        writer.toString()
    }

    suspend fun exportWeightsToCsv(database: AppDatabase): String = exportWeightsToCsv(database.weightDao())

    /**
     * Exporta las sesiones de entrenamiento a formato CSV.
     */
    suspend fun exportWorkoutsToCsv(workoutDao: WorkoutDao): String = withContext(Dispatchers.IO) {
        val workouts = workoutDao.getAllWorkoutsList()
        val writer = StringWriter()
        writer.append("id,name,type,startTime,endTime,durationMinutes,trimp,rpe,avgHeartRate,maxHeartRate,caloriesBurned,notes\n")
        for (w in workouts) {
            val safeName = w.name.replace(",", " ")
            val safeNotes = (w.notes ?: "").replace(",", " ").replace("\n", " ")
            writer.append("${w.id},$safeName,${w.type},${w.startTime},${w.endTime},${w.durationMinutes},${w.trimp ?: 0.0},${w.rpe ?: ""},${w.avgHeartRate ?: ""},${w.maxHeartRate ?: ""},${w.caloriesBurned ?: ""},$safeNotes\n")
        }
        writer.toString()
    }

    suspend fun exportWorkoutsToCsv(database: AppDatabase): String = exportWorkoutsToCsv(database.workoutDao())

    /**
     * Exporta la actividad diaria agregada del reloj a formato CSV.
     */
    suspend fun exportDailyActivityToCsv(dailyActivityDao: DailyActivityDao): String = withContext(Dispatchers.IO) {
        val activities = dailyActivityDao.getAllDailyActivities()
        val writer = StringWriter()
        writer.append("id,date,steps,distanceMeters,caloriesBurned,sourceDevice\n")
        for (a in activities) {
            writer.append("${a.id},${a.date},${a.steps},${a.distanceMeters},${a.caloriesBurned},${a.sourceDevice}\n")
        }
        writer.toString()
    }

    suspend fun exportDailyActivityToCsv(database: AppDatabase): String = exportDailyActivityToCsv(database.dailyActivityDao())

    /**
     * Exporta las muestras de frecuencia cardíaca a formato CSV.
     */
    suspend fun exportHeartRateToCsv(heartRateDao: HeartRateDao): String = withContext(Dispatchers.IO) {
        val samples = heartRateDao.getRecentSamplesList(5000)
        val writer = StringWriter()
        writer.append("id,timestamp,bpm,sourceDevice\n")
        for (s in samples) {
            writer.append("${s.id},${s.timestamp},${s.bpm},${s.sourceDevice}\n")
        }
        writer.toString()
    }

    suspend fun exportHeartRateToCsv(database: AppDatabase): String = exportHeartRateToCsv(database.heartRateDao())

    /**
     * Exporta las sesiones de sueño a formato CSV.
     */
    suspend fun exportSleepToCsv(sleepSessionDao: com.salud.personal.data.local.dao.SleepSessionDao): String = withContext(Dispatchers.IO) {
        val sessions = sleepSessionDao.getAllSleepList()
        val writer = StringWriter()
        writer.append("id,date,startTime,endTime,deepSleepMinutes,lightSleepMinutes,remSleepMinutes,awakeMinutes,totalSleepMinutes,efficiencyScore,sourceDevice\n")
        for (s in sessions) {
            writer.append("${s.id},${s.date},${s.startTime},${s.endTime},${s.deepSleepMinutes},${s.lightSleepMinutes},${s.remSleepMinutes},${s.awakeMinutes},${s.totalSleepMinutes},${s.sleepEfficiencyScore ?: ""},${s.sourceDevice}\n")
        }
        writer.toString()
    }

    suspend fun exportSleepToCsv(database: AppDatabase): String = exportSleepToCsv(database.sleepSessionDao())

    /**
     * Escribe todos los archivos CSV en el directorio local de la app y devuelve la lista de archivos creados.
     */
    suspend fun exportAllToFiles(context: Context, database: AppDatabase): List<File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }

        val weightsFile = File(exportDir, "pesos_renpho.csv").apply {
            writeText(exportWeightsToCsv(database.weightDao()))
        }

        val workoutsFile = File(exportDir, "entrenamientos.csv").apply {
            writeText(exportWorkoutsToCsv(database.workoutDao()))
        }

        val activityFile = File(exportDir, "actividad_diaria.csv").apply {
            writeText(exportDailyActivityToCsv(database.dailyActivityDao()))
        }

        val hrFile = File(exportDir, "frecuencia_cardiaca.csv").apply {
            writeText(exportHeartRateToCsv(database.heartRateDao()))
        }

        val sleepFile = File(exportDir, "sueno.csv").apply {
            writeText(exportSleepToCsv(database.sleepSessionDao()))
        }

        listOf(weightsFile, workoutsFile, activityFile, hrFile, sleepFile)
    }
}
