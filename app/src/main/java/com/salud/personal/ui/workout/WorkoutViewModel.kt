package com.salud.personal.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.salud.personal.data.local.dao.WorkoutDao
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.training.TrainingLoadCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import com.salud.personal.domain.strain.ManualWorkoutEstimator

data class ActiveWorkoutState(
    val isRecording: Boolean = false,
    val type: WorkoutType = WorkoutType.CROSSFIT_WOD,
    val name: String = "WOD",
    val startTime: Instant? = null,
    val elapsedSeconds: Long = 0L,
    val currentHeartRate: Int? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val estimatedTrimp: Double = 0.0,
    val hrSamplesCount: Int = 0
)

class WorkoutViewModel(
    private val workoutDao: WorkoutDao,
    private val maxHeartRateSetting: Int = 190,
    private val restingHeartRateSetting: Int = 50
) : ViewModel() {

    val workouts: StateFlow<List<WorkoutSessionEntity>> = workoutDao.getAllWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeWorkout = MutableStateFlow(ActiveWorkoutState())
    val activeWorkout: StateFlow<ActiveWorkoutState> = _activeWorkout.asStateFlow()

    private val recordedBpmList = mutableListOf<Int>()

    fun startWorkout(type: WorkoutType, name: String) {
        recordedBpmList.clear()
        _activeWorkout.value = ActiveWorkoutState(
            isRecording = true,
            type = type,
            name = name,
            startTime = Instant.now(),
            elapsedSeconds = 0L,
            currentHeartRate = null,
            avgHeartRate = null,
            maxHeartRate = null,
            estimatedTrimp = 0.0,
            hrSamplesCount = 0
        )
    }

    fun onHeartRateReceived(sample: HeartRateSample) {
        val current = _activeWorkout.value
        if (!current.isRecording || current.startTime == null) return

        val bpm = sample.bpm
        recordedBpmList.add(bpm)

        val avg = recordedBpmList.average().toInt()
        val max = recordedBpmList.maxOrNull() ?: bpm
        val durationMinutes = Duration.between(current.startTime, Instant.now()).toMillis() / 60000.0

        val trimp = TrainingLoadCalculator.calculateBanisterTrimp(
            durationMinutes = durationMinutes.coerceAtLeast(0.1),
            avgHr = avg.toDouble(),
            restHr = restingHeartRateSetting.toDouble(),
            maxHr = maxHeartRateSetting.toDouble()
        )

        _activeWorkout.value = current.copy(
            currentHeartRate = bpm,
            avgHeartRate = avg,
            maxHeartRate = max,
            estimatedTrimp = trimp,
            hrSamplesCount = recordedBpmList.size
        )
    }

    fun updateElapsedSeconds(seconds: Long) {
        val current = _activeWorkout.value
        if (current.isRecording) {
            _activeWorkout.value = current.copy(elapsedSeconds = seconds)
        }
    }

    fun finishWorkout(rpe: Int, notes: String? = null) {
        val current = _activeWorkout.value
        if (!current.isRecording || current.startTime == null) return

        val endTime = Instant.now()
        val durationMinutes = (Duration.between(current.startTime, endTime).toMillis() / 60000.0)
            .coerceAtLeast(0.1)

        val session = WorkoutSessionEntity(
            type = current.type,
            name = current.name,
            startTime = current.startTime,
            endTime = endTime,
            durationMinutes = kotlin.math.round(durationMinutes * 10.0) / 10.0,
            avgHeartRate = current.avgHeartRate,
            maxHeartRate = current.maxHeartRate,
            trimp = current.estimatedTrimp,
            caloriesBurned = null,
            rpe = rpe.coerceIn(1, 10),
            notes = notes
        )

        viewModelScope.launch {
            workoutDao.insertWorkout(session)
        }

        _activeWorkout.value = ActiveWorkoutState(isRecording = false)
        recordedBpmList.clear()
    }

    fun cancelWorkout() {
        _activeWorkout.value = ActiveWorkoutState(isRecording = false)
        recordedBpmList.clear()
    }

    fun addManualWorkout(
        type: WorkoutType,
        name: String,
        durationMinutes: Int,
        rpe: Int,
        notes: String? = null
    ) {
        val now = Instant.now()
        val start = now.minus(durationMinutes.toLong(), ChronoUnit.MINUTES)
        val trimp = ManualWorkoutEstimator.estimateTrimp(type, durationMinutes, rpe)
        val calories = ManualWorkoutEstimator.estimateCalories(type, durationMinutes, rpe)

        val session = WorkoutSessionEntity(
            type = type,
            name = name.ifBlank {
                when (type) {
                    WorkoutType.CROSSFIT_WOD -> "WOD CrossFit"
                    WorkoutType.STRENGTH -> "Fuerza / Musculación"
                    WorkoutType.ENDURANCE_CARDIO -> "Cardio / Running"
                    WorkoutType.WALK -> "Caminata"
                    WorkoutType.RECOVERY -> "Recuperación Activa"
                }
            },
            startTime = start,
            endTime = now,
            durationMinutes = durationMinutes.toDouble(),
            avgHeartRate = null,
            maxHeartRate = null,
            trimp = trimp,
            caloriesBurned = calories,
            rpe = rpe.coerceIn(1, 10),
            notes = notes
        )
        viewModelScope.launch {
            workoutDao.insertWorkout(session)
        }
    }
}

class WorkoutViewModelFactory(
    private val workoutDao: WorkoutDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkoutViewModel(workoutDao) as T
    }
}
