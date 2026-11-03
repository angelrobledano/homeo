package com.salud.personal.data.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.salud.personal.MainActivity
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.gps.GpsDistanceCalculator
import com.salud.personal.domain.gps.GpsLocationPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class WalkTrackingState(
    val isTracking: Boolean = false,
    val startTime: Instant? = null,
    val elapsedSeconds: Long = 0L,
    val points: List<GpsLocationPoint> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val paceMinutesPerKm: Double = 0.0,
    val caloriesBurned: Int = 0
)

class WalkTrackingService : Service(), LocationListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null
    private var locationManager: LocationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (_trackingState.value.isTracking) return

        val now = Instant.now()
        _trackingState.value = WalkTrackingState(
            isTracking = true,
            startTime = now,
            elapsedSeconds = 0L,
            points = emptyList(),
            totalDistanceMeters = 0.0,
            paceMinutesPerKm = 0.0,
            caloriesBurned = 0
        )

        val notification = buildNotification("Grabando caminata...", "0.00 km · 00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val minTimeMs = 2000L
            val minDistanceM = 2f

            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, this)
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, minDistanceM, this)
                }
            }
        } catch (e: SecurityException) {
            // Permission missing
            stopTracking()
            return
        }

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_trackingState.value.isTracking) {
                delay(1000)
                _trackingState.update { current ->
                    val newElapsed = current.elapsedSeconds + 1
                    val pace = GpsDistanceCalculator.calculatePaceMinutesPerKm(current.totalDistanceMeters, newElapsed)
                    // Heuristic calories: ~55 kcal per km of walk
                    val calories = ((current.totalDistanceMeters / 1000.0) * 55.0).toInt()
                    current.copy(
                        elapsedSeconds = newElapsed,
                        paceMinutesPerKm = pace,
                        caloriesBurned = calories
                    )
                }
                updateNotification()
            }
        }
    }

    private fun stopTracking() {
        timerJob?.cancel()
        timerJob = null

        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            // Ignore
        }

        val finalState = _trackingState.value
        _trackingState.value = WalkTrackingState(isTracking = false)

        if (finalState.isTracking && finalState.startTime != null && finalState.elapsedSeconds >= 10) {
            serviceScope.launch {
                saveWorkoutSession(finalState)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun saveWorkoutSession(state: WalkTrackingState) {
        val start = state.startTime ?: Instant.now().minusSeconds(state.elapsedSeconds)
        val end = Instant.now()
        val durationMin = kotlin.math.round((state.elapsedSeconds / 60.0) * 10.0) / 10.0
        val distKm = kotlin.math.round((state.totalDistanceMeters / 1000.0) * 100.0) / 100.0

        val session = WorkoutSessionEntity(
            type = WorkoutType.ENDURANCE_CARDIO,
            name = "Caminata Exterior GPS",
            startTime = start,
            endTime = end,
            durationMinutes = durationMin,
            avgHeartRate = null,
            maxHeartRate = null,
            trimp = (durationMin * 1.2).coerceAtLeast(1.0),
            caloriesBurned = state.caloriesBurned.coerceAtLeast(10),
            rpe = 5,
            notes = "Distancia: $distKm km · Ritmo medio: ${String.format(java.util.Locale.US, "%.1f", state.paceMinutesPerKm)} min/km"
        )

        try {
            val db = AppDatabase.getInstance(applicationContext)
            db.workoutDao().insertWorkout(session)
        } catch (e: Exception) {
            // Database insert error fallback
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!_trackingState.value.isTracking) return
        if (location.accuracy > 35f) return // Filter inaccurate coordinates

        val newPoint = GpsLocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = location.altitude,
            timestampEpochMs = location.time
        )

        _trackingState.update { current ->
            val updatedPoints = current.points + newPoint
            val updatedDistance = GpsDistanceCalculator.calculateTotalDistance(updatedPoints)
            val pace = GpsDistanceCalculator.calculatePaceMinutesPerKm(updatedDistance, current.elapsedSeconds)
            val calories = ((updatedDistance / 1000.0) * 55.0).toInt()

            current.copy(
                points = updatedPoints,
                totalDistanceMeters = updatedDistance,
                paceMinutesPerKm = pace,
                caloriesBurned = calories
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabación GPS de Caminatas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra la distancia y tiempo transcurrido en segundo plano"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification() {
        val state = _trackingState.value
        if (!state.isTracking) return
        val distKm = String.format(java.util.Locale.US, "%.2f km", state.totalDistanceMeters / 1000.0)
        val minutes = state.elapsedSeconds / 60
        val seconds = state.elapsedSeconds % 60
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        val notification = buildNotification("Caminata GPS en curso", "$distKm · $timeStr")
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_START = "com.salud.personal.action.START_WALK"
        const val ACTION_STOP = "com.salud.personal.action.STOP_WALK"
        private const val NOTIFICATION_ID = 2048
        private const val CHANNEL_ID = "walk_tracking_channel"

        private val _trackingState = MutableStateFlow(WalkTrackingState())
        val trackingState: StateFlow<WalkTrackingState> = _trackingState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, WalkTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WalkTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
