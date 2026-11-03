package com.salud.personal.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.salud.personal.MainActivity
import com.salud.personal.R
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.domain.strain.StrainTargetCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class SaludAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val database = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            updateAllWidgets(context, appWidgetManager, appWidgetIds, database)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SaludAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val database = AppDatabase.getInstance(context)
                CoroutineScope(Dispatchers.IO).launch {
                    updateAllWidgets(context, appWidgetManager, appWidgetIds, database)
                }
            }
        }

        private suspend fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            database: AppDatabase
        ) {
            val today = LocalDate.now()
            val zoneId = ZoneId.systemDefault()

            // 1. Recuperación
            val checkIn = database.dailyCheckInDao().getCheckInForDate(today)
            val recoveryScore = checkIn?.calculatedRecoveryScore

            // 2. Pasos
            val allActs = database.dailyActivityDao().getAllDailyActivities()
            val todayAct = allActs.firstOrNull {
                try { LocalDate.parse(it.date) == today } catch (e: Exception) { false }
            }
            val steps = todayAct?.steps ?: 0

            // 3. Esfuerzo / Strain
            val workouts28d = database.workoutDao().getAllWorkoutsList()
            val todayWorkouts = workouts28d.filter {
                it.startTime.atZone(zoneId).toLocalDate() == today
            }
            val todayTrimp = todayWorkouts.sumOf { it.trimp ?: 0.0 }
            val currentStrain = StrainTargetCalculator.calculateDayStrain(steps, todayTrimp)

            // Intent para abrir MainActivity al pulsar el widget
            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_salud_glance)

                // Recovery
                val recText = if (recoveryScore != null) "$recoveryScore%" else "--%"
                val recColor = when {
                    recoveryScore == null -> 0xFF8F96A3.toInt()
                    recoveryScore >= 67 -> 0xFF00E676.toInt()
                    recoveryScore >= 34 -> 0xFFFFD600.toInt()
                    else -> 0xFFFF5252.toInt()
                }
                views.setTextViewText(R.id.widget_recovery_val, recText)
                views.setTextColor(R.id.widget_recovery_val, recColor)

                // Strain
                val strainText = String.format(Locale.US, "%.1f", currentStrain)
                views.setTextViewText(R.id.widget_strain_val, strainText)

                // Steps
                val stepsText = if (steps >= 1000) {
                    String.format(Locale.US, "%.1fk", steps / 1000.0)
                } else {
                    "$steps"
                }
                views.setTextViewText(R.id.widget_steps_val, stepsText)

                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
