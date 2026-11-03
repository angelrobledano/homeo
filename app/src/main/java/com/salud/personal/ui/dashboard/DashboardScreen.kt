package com.salud.personal.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.salud.personal.data.local.entity.HeartRateSampleEntity
import com.salud.personal.data.local.entity.WeightMeasurementEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.domain.heartrate.HeartRateZonesCalculator
import com.salud.personal.domain.strain.StrainCapacityTier
import com.salud.personal.domain.strain.StrainStatus
import com.salud.personal.domain.strain.StrainTargetCalculator
import com.salud.personal.domain.strain.StrainTargetRange
import com.salud.personal.domain.vitals.MetricBaselineStatus
import com.salud.personal.domain.vitals.VitalsBaselineCalculator
import com.salud.personal.domain.vitals.VitalsEvaluationResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.data.local.entity.DailyCheckInEntity
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WatchBatteryStatus
import com.salud.personal.domain.model.WeightMeasurement
import com.salud.personal.ui.components.BiometricMetricCard
import com.salud.personal.ui.components.WhoopProgressRing
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import androidx.compose.ui.res.stringResource
import com.salud.personal.R
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/**
 * Pantalla "Hoy" — vista principal del dashboard diario.
 * Usa el sistema de diseño WHOOP oscuro con tarjetas de alto contraste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoyScreen(
    viewModel: DashboardViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenCheckIn: () -> Unit = {},
    onOpenActivityDetail: () -> Unit = {},
    onOpenSleepDetail: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onSync: () -> Unit = {},
    todayCheckIn: DailyCheckInEntity? = null,
    todayWorkouts: List<WorkoutSessionEntity> = emptyList(),
    todayHrSamples: List<HeartRateSampleEntity> = emptyList(),
    allCheckIns: List<DailyCheckInEntity> = emptyList(),
    allWeights: List<WeightMeasurementEntity> = emptyList(),
    allSleeps: List<SleepSessionEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    HoyContent(
        state = state,
        todayCheckIn = todayCheckIn,
        todayWorkouts = todayWorkouts,
        todayHrSamples = todayHrSamples,
        allCheckIns = allCheckIns,
        allWeights = allWeights,
        allSleeps = allSleeps,
        onConnectWatch = { viewModel.connectWatch() },
        onDisconnectWatch = { viewModel.disconnectWatch() },
        onOpenSettings = onOpenSettings,
        onOpenCalendar = onOpenCalendar,
        onOpenCheckIn = onOpenCheckIn,
        onOpenActivityDetail = onOpenActivityDetail,
        onOpenSleepDetail = onOpenSleepDetail,
        onOpenWorkouts = onOpenWorkouts,
        onSync = onSync,
        modifier = modifier
    )
}

// Keep backward compatibility alias
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenSettings: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenCheckIn: () -> Unit = {},
    onOpenActivityDetail: () -> Unit = {},
    onOpenSleepDetail: () -> Unit = {},
    onSync: () -> Unit = {},
    todayCheckIn: DailyCheckInEntity? = null,
    modifier: Modifier = Modifier
) {
    HoyScreen(
        viewModel = viewModel,
        onOpenSettings = onOpenSettings,
        onOpenCalendar = onOpenCalendar,
        onOpenCheckIn = onOpenCheckIn,
        onOpenActivityDetail = onOpenActivityDetail,
        onOpenSleepDetail = onOpenSleepDetail,
        onSync = onSync,
        todayCheckIn = todayCheckIn,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoyContent(
    state: DashboardUiState,
    todayCheckIn: DailyCheckInEntity? = null,
    todayWorkouts: List<WorkoutSessionEntity> = emptyList(),
    todayHrSamples: List<HeartRateSampleEntity> = emptyList(),
    allCheckIns: List<DailyCheckInEntity> = emptyList(),
    allWeights: List<WeightMeasurementEntity> = emptyList(),
    allSleeps: List<SleepSessionEntity> = emptyList(),
    onConnectWatch: () -> Unit = {},
    onDisconnectWatch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenCheckIn: () -> Unit = {},
    onOpenActivityDetail: () -> Unit = {},
    onOpenSleepDetail: () -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val todayStart = remember {
        java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
    }
    val actualTodayWorkouts = remember(todayWorkouts, todayStart) {
        todayWorkouts.filter { it.startTime >= todayStart }
    }

    val currentHour = remember { java.time.LocalTime.now().hour }
    var selectedTimeMode by remember { mutableStateOf<DashboardTimeMode?>(null) }
    val effectiveTimeMode = selectedTimeMode ?: when (currentHour) {
        in 5..11 -> DashboardTimeMode.MORNING
        in 12..20 -> DashboardTimeMode.DAYTIME
        else -> DashboardTimeMode.EVENING
    }

    val todayRecoveryScore = todayCheckIn?.calculatedRecoveryScore ?: 70
    val strainTarget = remember(todayRecoveryScore) {
        StrainTargetCalculator.calculateTarget(todayRecoveryScore)
    }
    val workoutTrimpSum = remember(actualTodayWorkouts) {
        actualTodayWorkouts.sumOf { it.trimp ?: 0.0 }
    }
    val currentDayStrain = remember(state.todayActivity?.steps, workoutTrimpSum) {
        StrainTargetCalculator.calculateDayStrain(state.todayActivity?.steps ?: 0, workoutTrimpSum)
    }

    val vitalsResult = remember(allCheckIns, allSleeps, allWeights, state.latestHeartRate, state.todaySleep, state.latestMeasurement) {
        val rhrHistory = allCheckIns.mapNotNull { it.restingHeartRate }
        val sleepHistory = allCheckIns.map { it.sleepHours }
        val restorativeHistory = allSleeps.mapNotNull { s ->
            if (s.totalSleepMinutes > 0) ((s.deepSleepMinutes + s.remSleepMinutes).toDouble() / s.totalSleepMinutes * 100).toInt() else null
        }
        val weightHistory = allWeights.map { it.weightKg }
        val todayRestorativePct = state.todaySleep?.let { s ->
            if (s.totalSleepMinutes > 0) ((s.deepSleepMinutes + s.remSleepMinutes).toDouble() / s.totalSleepMinutes * 100).toInt() else null
        }

        VitalsBaselineCalculator.evaluate(
            todayRhr = state.latestHeartRate?.bpm,
            rhrHistory = rhrHistory,
            todaySleepHours = state.todaySleep?.let { it.totalSleepMinutes.toDouble() / 60.0 } ?: todayCheckIn?.sleepHours,
            sleepHistory = sleepHistory,
            todayRestorativePct = todayRestorativePct,
            restorativeHistory = restorativeHistory,
            todayWeightKg = state.latestMeasurement?.weightKg,
            weightHistory = weightHistory
        )
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                onSync()
                delay(1200)
                isRefreshing = false
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhoopColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = WhoopColors.Strain,
                    trackColor = WhoopColors.StrainTrack
                )
            }

            if (state.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhoopColors.ErrorContainer
                ) {
                    Text(
                        text = state.errorMessage,
                        color = WhoopColors.Error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Watch device chip + Calendar + Settings + Sync buttons
            WatchDeviceChip(
                connectionState = state.watchConnectionState,
                battery = state.watchBattery,
                onConnect = onConnectWatch,
                onOpenCalendar = onOpenCalendar,
                onOpenSettings = onOpenSettings,
                onSync = {
                    isRefreshing = true
                    coroutineScope.launch {
                        onSync()
                        delay(1200)
                        isRefreshing = false
                    }
                }
            )

            // Hero Card: Estado Diario Unificado (Recuperación, Diana Esfuerzo, Sueño y FC Reposo)
            DayStateHeroCard(
                recoveryScore = todayRecoveryScore,
                currentStrain = currentDayStrain,
                strainTarget = strainTarget,
                sleepMinutes = state.todaySleep?.totalSleepMinutes,
                restingHeartRate = todayCheckIn?.restingHeartRate ?: state.latestHeartRate?.bpm,
                timeMode = effectiveTimeMode,
                onTimeModeChange = { selectedTimeMode = it },
                onOpenCheckIn = onOpenCheckIn,
                onOpenSleepDetail = onOpenSleepDetail
            )

            when (effectiveTimeMode) {
                DashboardTimeMode.MORNING -> {
                    // Enfoque matutino: Check-in prioritario si falta, diana de esfuerzo y sueño
                    MorningCheckInCard(
                        checkIn = todayCheckIn,
                        onOpenCheckIn = onOpenCheckIn
                    )
                    StrainTargetCard(
                        currentStrain = currentDayStrain,
                        target = strainTarget,
                        recoveryScore = todayRecoveryScore
                    )
                    if (state.todaySleep != null) {
                        SleepCard(sleep = state.todaySleep, onClick = onOpenSleepDetail)
                    } else {
                        NoSleepCard(onClick = onOpenSleepDetail)
                    }
                    StepsCard(activity = state.todayActivity, onClick = onOpenActivityDetail)
                    if (actualTodayWorkouts.isNotEmpty()) {
                        TodayWorkoutsCard(workouts = actualTodayWorkouts, onClick = onOpenWorkouts)
                    }
                    VitalsBaselineCard(vitals = vitalsResult)
                    WeightSummaryCard(measurement = state.latestMeasurement, delta7DaysKg = state.delta7DaysKg)
                    HeartRateCard(sample = state.latestHeartRate, samples = todayHrSamples)
                }
                DashboardTimeMode.DAYTIME -> {
                    // Enfoque activo: Entrenamientos del día, pasos acumulados y progreso de esfuerzo
                    if (actualTodayWorkouts.isNotEmpty()) {
                        TodayWorkoutsCard(workouts = actualTodayWorkouts, onClick = onOpenWorkouts)
                    }
                    StepsCard(activity = state.todayActivity, onClick = onOpenActivityDetail)
                    StrainTargetCard(
                        currentStrain = currentDayStrain,
                        target = strainTarget,
                        recoveryScore = todayRecoveryScore
                    )
                    MorningCheckInCard(
                        checkIn = todayCheckIn,
                        onOpenCheckIn = onOpenCheckIn
                    )
                    if (state.todaySleep != null) {
                        SleepCard(sleep = state.todaySleep, onClick = onOpenSleepDetail)
                    }
                    VitalsBaselineCard(vitals = vitalsResult)
                    WeightSummaryCard(measurement = state.latestMeasurement, delta7DaysKg = state.delta7DaysKg)
                    HeartRateCard(sample = state.latestHeartRate, samples = todayHrSamples)
                }
                DashboardTimeMode.EVENING -> {
                    // Enfoque nocturno: Balance de esfuerzo completado, preparación para dormir y descanso
                    if (state.todaySleep != null) {
                        SleepCard(sleep = state.todaySleep, onClick = onOpenSleepDetail)
                    } else {
                        NoSleepCard(onClick = onOpenSleepDetail)
                    }
                    StrainTargetCard(
                        currentStrain = currentDayStrain,
                        target = strainTarget,
                        recoveryScore = todayRecoveryScore
                    )
                    StepsCard(activity = state.todayActivity, onClick = onOpenActivityDetail)
                    if (actualTodayWorkouts.isNotEmpty()) {
                        TodayWorkoutsCard(workouts = actualTodayWorkouts, onClick = onOpenWorkouts)
                    }
                    MorningCheckInCard(
                        checkIn = todayCheckIn,
                        onOpenCheckIn = onOpenCheckIn
                    )
                    VitalsBaselineCard(vitals = vitalsResult)
                    WeightSummaryCard(measurement = state.latestMeasurement, delta7DaysKg = state.delta7DaysKg)
                    HeartRateCard(sample = state.latestHeartRate, samples = todayHrSamples)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

enum class DashboardTimeMode(val title: String, val icon: String) {
    MORNING("Modo Mañana", "🌅"),     // 05:00 - 12:00: Recuperación, Sueño y Prescripción de Esfuerzo
    DAYTIME("Modo Activo", "⚡"),       // 12:00 - 21:00: Esfuerzo en vivo, Pasos y Entrenamientos
    EVENING("Modo Nocturno", "🌙")     // 21:00 - 05:00: Balance diario, Deuda y Preparación de Sueño
}

@Composable
private fun DayStateHeroCard(
    recoveryScore: Int,
    currentStrain: Double,
    strainTarget: StrainTargetRange,
    sleepMinutes: Int?,
    restingHeartRate: Int?,
    timeMode: DashboardTimeMode,
    onTimeModeChange: (DashboardTimeMode) -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenSleepDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recoveryColor = WhoopColors.recoveryColor(recoveryScore)
    val strainRatio = (currentStrain / strainTarget.maxStrain.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    val animatedStrain by animateFloatAsState(
        targetValue = strainRatio,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "heroStrainAnim"
    )

    EasySpotlightCard(
        spotlightAccent = recoveryColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Modo temporal con selector interactivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dash_day_status),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                // Selector de modo temporal interactivo
                val haptic = LocalHapticFeedback.current
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashboardTimeMode.entries.forEach { mode ->
                        val isSelected = mode == timeMode
                        val modeLabel = when (mode) {
                            DashboardTimeMode.MORNING -> stringResource(R.string.dash_mode_morning)
                            DashboardTimeMode.DAYTIME -> stringResource(R.string.dash_mode_active)
                            DashboardTimeMode.EVENING -> stringResource(R.string.dash_mode_night)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) WhoopColors.Strain.copy(alpha = 0.2f) else WhoopColors.SurfaceElevated,
                            border = if (isSelected) BorderStroke(1.dp, WhoopColors.Strain) else null,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { contentDescription = "$modeLabel${if (isSelected) ", seleccionado" else ""}" }
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTimeModeChange(mode)
                                }
                                .tactilePress(scaleDown = 0.92f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text(
                                    text = mode.icon,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

            // Cuerpo Central: Score Recuperación a la izquierda + Columnas métricas a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recuperación (Toque abre check-in)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenCheckIn() }
                        .tactilePress()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = recoveryColor.copy(alpha = 0.15f),
                                style = Stroke(width = 8.dp.toPx())
                            )
                            drawArc(
                                color = recoveryColor,
                                startAngle = -90f,
                                sweepAngle = (recoveryScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$recoveryScore%",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = recoveryColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dash_recovery),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = recoveryColor
                    )
                }

                // Grid de métricas clave (Esfuerzo, Sueño, FC Reposo)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Esfuerzo (Strain)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.dash_strain),
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", currentStrain)} / ${strainTarget.displayRange}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Strain
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(WhoopColors.StrainTrack, RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedStrain)
                                    .fillMaxHeight()
                                    .background(WhoopColors.Strain, RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    // Sueño de anoche
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSleepDetail() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dash_sleep),
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        val sleepText = sleepMinutes?.let {
                            "${it / 60}h ${it % 60}m"
                        } ?: stringResource(R.string.pending)
                        Text(
                            text = sleepText,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = if (sleepMinutes != null) WhoopColors.Sleep else WhoopColors.OnSurfaceTertiary
                        )
                    }

                    // FC Reposo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dash_rhr),
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = if (restingHeartRate != null) "$restingHeartRate lpm" else "-- lpm",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                    }
                }
            }

            // Frase accionable adaptativa según el momento del día
            HorizontalDivider(color = WhoopColors.Outline.copy(alpha = 0.3f))

            val contextText = when (timeMode) {
                DashboardTimeMode.MORNING -> {
                    if (recoveryScore >= 67) "🌅 Mañana: Sistema nervioso plenamente recuperado. Objetivo de esfuerzo alto (hasta ${String.format(Locale.US, "%.1f", strainTarget.maxStrain)} pts)."
                    else if (recoveryScore >= 34) "🌅 Mañana: Capacidad moderada. Enfoque en volumen aeróbico Z2 o fuerza técnica sin fatiga excesiva."
                    else "🌅 Mañana: Recuperación baja. Prioriza regeneración, hidratación y descanso activo."
                }
                DashboardTimeMode.DAYTIME -> {
                    val remaining = (strainTarget.maxStrain - currentStrain).coerceAtLeast(0.0)
                    if (remaining > 0.5) "⚡ Tarde: Faltan ${String.format(Locale.US, "%.1f", remaining)} pts de esfuerzo para completar la diana recomendada."
                    else "⚡ Tarde: ¡Objetivo de esfuerzo diario alcanzado! Modula las cargas restantes del día."
                }
                DashboardTimeMode.EVENING -> {
                    "🌙 Noche: Balance fijado en ${String.format(Locale.US, "%.1f", currentStrain)} pts. Inicia tu desconexión de pantallas para optimizar el descanso."
                }
            }

            Text(
                text = contextText,
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun TodayWorkoutsCard(
    workouts: List<WorkoutSessionEntity>,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (workouts.isEmpty()) return

    EasySpotlightCard(
        spotlightAccent = WhoopColors.Strain,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .tactilePress(scaleDown = 0.98f)
                        .clickable { onClick() }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dash_workouts_today),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (workouts.size == 1) stringResource(R.string.dash_session_singular, workouts.size) else stringResource(R.string.dash_session_plural, workouts.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.Strain,
                    maxLines = 1,
                    softWrap = false
                )
            }

            workouts.forEach { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WhoopColors.Surface, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = session.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeStr = DashboardFormatting.formatTime(session.startTime)
                            Text(
                                text = "$timeStr · ${session.durationMinutes.toInt()} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceSecondary
                            )
                            if (session.rpe > 0) {
                                Text(
                                    text = "RPE ${session.rpe}/10",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WhoopColors.OnSurfaceTertiary
                                )
                            }
                        }
                    }

                    if (session.trimp != null && session.trimp > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WhoopColors.Strain.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${session.trimp.toInt()} pts",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Strain,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorningCheckInCard(
    checkIn: DailyCheckInEntity?,
    onOpenCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completed = checkIn != null
    val score = checkIn?.calculatedRecoveryScore

    EasySpotlightCard(
        spotlightAccent = if (completed) WhoopColors.recoveryColor(score ?: 75) else WhoopColors.Strain,
        onClick = onOpenCheckIn,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (completed) WhoopColors.recoveryColor(score ?: 75).copy(alpha = 0.2f) else WhoopColors.Strain.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (completed) "✓" else "☀️",
                            fontSize = 16.sp
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.dash_checkin_title),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary
                    )
                    Text(
                        text = if (completed) stringResource(R.string.dash_checkin_done, score ?: 75) else stringResource(R.string.dash_checkin_pending),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (completed) WhoopColors.recoveryColor(score ?: 75) else WhoopColors.OnSurface
                    )
                }
            }

            Text(
                text = "→",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WhoopColors.OnSurfaceTertiary
            )
        }
    }
}

@Composable
private fun WatchDeviceChip(
    connectionState: DeviceConnectionState,
    battery: WatchBatteryStatus?,
    onConnect: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (dotColor, statusRes) = when (connectionState) {
        DeviceConnectionState.CONNECTED -> Pair(WhoopColors.RecoveryHigh, R.string.connected)
        DeviceConnectionState.CONNECTING -> Pair(WhoopColors.Warning, R.string.connecting)
        DeviceConnectionState.SCANNING -> Pair(WhoopColors.Warning, R.string.scanning)
        DeviceConnectionState.AUTHENTICATING -> Pair(WhoopColors.Warning, R.string.authenticating)
        DeviceConnectionState.AUTH_FAILED -> Pair(WhoopColors.Error, R.string.auth_failed)
        DeviceConnectionState.DISCONNECTED -> Pair(WhoopColors.OnSurfaceTertiary, R.string.disconnected)
    }
    val statusLabel = stringResource(statusRes)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = WhoopColors.SurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = dotColor, shape = CircleShape)
                    )
                    Text(
                        text = "GTR 3 · $statusLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = WhoopColors.OnSurface
                    )
                }
            }

            if (battery != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = WhoopColors.SurfaceElevated
                ) {
                    val batteryText = if (battery.isCharging) "${battery.percentage}% ⚡" else "${battery.percentage}%"
                    Text(
                        text = batteryText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = WhoopColors.OnSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (connectionState) {
                DeviceConnectionState.DISCONNECTED,
                DeviceConnectionState.AUTH_FAILED -> {
                    OutlinedButton(
                        onClick = onConnect,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WhoopColors.Strain
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            text = if (connectionState == DeviceConnectionState.AUTH_FAILED) stringResource(R.string.retry) else stringResource(R.string.connect),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                DeviceConnectionState.CONNECTING,
                DeviceConnectionState.SCANNING,
                DeviceConnectionState.AUTHENTICATING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = WhoopColors.Strain
                    )
                }
                DeviceConnectionState.CONNECTED -> { /* Quiet — no action needed */ }
            }

            IconButton(
                onClick = onSync,
                modifier = Modifier
                    .size(48.dp)
                    .tactilePress(scaleDown = 0.88f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.sync),
                    tint = WhoopColors.Strain,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onOpenCalendar,
                modifier = Modifier
                    .size(48.dp)
                    .tactilePress(scaleDown = 0.88f)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.calendar),
                    tint = WhoopColors.OnSurfaceTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .tactilePress(scaleDown = 0.88f)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = WhoopColors.OnSurfaceTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StepsCard(
    activity: DailyActivitySummary?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val steps = activity?.steps ?: 0
    val goal = 10_000
    val progress = if (goal > 0) steps.toFloat() / goal else 0f

    EasySpotlightCard(
        spotlightAccent = WhoopColors.Strain,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .tactilePress(scaleDown = 0.96f)
                        .clickable { onClick() }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dash_activity),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Text(
                    text = activity?.sourceDevice ?: "Amazfit GTR 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            WhoopProgressRing(
                progress = progress,
                displayValue = DashboardFormatting.formatSteps(activity?.steps),
                displayUnit = stringResource(R.string.dash_steps).lowercase(),
                accentColor = WhoopColors.Strain,
                trackColor = WhoopColors.StrainTrack,
                ringSize = 160.dp,
                strokeWidth = 10.dp
            )

            // Remaining steps
            val remaining = (goal - steps).coerceAtLeast(0)
            if (remaining > 0 && activity != null) {
                Text(
                    text = "Faltan ${DashboardFormatting.formatSteps(remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = WhoopColors.Strain
                )
            } else if (activity != null && steps >= goal) {
                Text(
                    text = "Meta cumplida ✓",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = WhoopColors.RecoveryHigh
                )
            }

            HorizontalDivider(color = WhoopColors.Outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactMetric(
                    label = stringResource(R.string.dash_distance),
                    value = DashboardFormatting.formatDistance(activity?.distanceMeters)
                )
                CompactMetric(
                    label = stringResource(R.string.dash_calories),
                    value = DashboardFormatting.formatCalories(activity?.caloriesBurned)
                )
            }

            if (onClick != null) {
                Text(
                    text = "Ver desglose por horas →",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.Strain
                )
            }
        }
    }
}

@Composable
private fun StrainTargetCard(
    currentStrain: Double,
    target: StrainTargetRange,
    recoveryScore: Int?,
    modifier: Modifier = Modifier
) {
    val status = StrainTargetCalculator.assessStatus(currentStrain, target)
    val statusColor = when (status) {
        StrainStatus.OPTIMAL -> WhoopColors.RecoveryHigh
        StrainStatus.BELOW_TARGET -> WhoopColors.Warning
        StrainStatus.OVER_STRAIN -> WhoopColors.Error
    }
    val statusLabel = when (status) {
        StrainStatus.OPTIMAL -> stringResource(R.string.dash_strain_optimal)
        StrainStatus.BELOW_TARGET -> stringResource(R.string.dash_strain_below)
        StrainStatus.OVER_STRAIN -> stringResource(R.string.dash_strain_over)
    }

    EasySpotlightCard(
        spotlightAccent = WhoopColors.Strain,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OBJETIVO DE ESFUERZO DIARIO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", currentStrain),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Strain
                    )
                    Text(
                        text = "pts",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rango Recomendado",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = target.displayRange,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface
                    )
                }
            }

            // Barra visual de esfuerzo con rango óptimo y puntero
            StrainTargetProgressBar(
                current = currentStrain.toFloat(),
                minTarget = target.minStrain.toFloat(),
                maxTarget = target.maxStrain.toFloat(),
                statusColor = statusColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            // Diagnóstico y consejo
            val adviceText = when (status) {
                StrainStatus.BELOW_TARGET -> {
                    val remaining = String.format(java.util.Locale.US, "%.1f", target.minStrain - currentStrain)
                    "Faltan $remaining pts para tu estímulo óptimo de hoy según tu recuperación (${recoveryScore ?: 70}%)."
                }
                StrainStatus.OPTIMAL -> "Estás dentro del rango óptimo de adaptación biológica. Prioriza descanso."
                StrainStatus.OVER_STRAIN -> "Has superado la carga recomendada. Hidratación y descanso prioritarios."
            }
            Text(
                text = adviceText,
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun StrainTargetProgressBar(
    current: Float,
    minTarget: Float,
    maxTarget: Float,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val animCurrent by animateFloatAsState(
        targetValue = current,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "strainProgressBarAnim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val barH = 10.dp.toPx()
        val topY = (size.height - barH) / 2f
        val maxScale = 21.0f // WHOOP max strain scale

        fun strainToX(s: Float): Float {
            val clamped = s.coerceIn(0f, maxScale)
            return (clamped / maxScale) * w
        }

        // Fondo barra completa (0 .. 21)
        drawRoundRect(
            color = WhoopColors.SurfaceElevated,
            topLeft = Offset(0f, topY),
            size = Size(w, barH),
            cornerRadius = CornerRadius(barH / 2, barH / 2)
        )

        // Progreso acumulado actual animado
        val xCur = strainToX(animCurrent)
        if (xCur > 0) {
            drawRoundRect(
                color = WhoopColors.Strain,
                topLeft = Offset(0f, topY),
                size = Size(xCur, barH),
                cornerRadius = CornerRadius(barH / 2, barH / 2)
            )
        }

        // Ventana objetivo (minTarget .. maxTarget): se dibuja encima para que siempre sea visible
        val xMin = strainToX(minTarget)
        val xMax = strainToX(maxTarget)
        val targetWidth = (xMax - xMin).coerceAtLeast(6f)
        drawRoundRect(
            color = WhoopColors.RecoveryHigh.copy(alpha = 0.35f),
            topLeft = Offset(xMin, topY),
            size = Size(targetWidth, barH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = WhoopColors.RecoveryHigh,
            topLeft = Offset(xMin, topY - 1.dp.toPx()),
            size = Size(targetWidth, barH + 2.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Puntero circular con borde blanco y centro del color de estado
        val pointerX = xCur.coerceIn(6.dp.toPx(), w - 6.dp.toPx())
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(pointerX, topY + barH / 2)
        )
        drawCircle(
            color = statusColor,
            radius = 4.dp.toPx(),
            center = Offset(pointerX, topY + barH / 2)
        )
    }
}

@Composable
private fun VitalsBaselineCard(
    vitals: VitalsEvaluationResult,
    modifier: Modifier = Modifier
) {
    val cardColor = if (vitals.allInBaseline) WhoopColors.RecoveryHigh else WhoopColors.Warning

    EasySpotlightCard(
        spotlightAccent = cardColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VITALES Y RANGO BASE",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor.copy(alpha = 0.15f)
                ) {
                    val statusText = if (vitals.allInBaseline) {
                        "✓ ${vitals.metricsInBaseline}/${vitals.totalMetricsEvaluated} en rango"
                    } else if (vitals.totalMetricsEvaluated > 0) {
                        "${vitals.totalMetricsEvaluated - vitals.metricsInBaseline} fuera de rango"
                    } else {
                        "Sin datos suficientes"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = cardColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (vitals.alerts.isNotEmpty()) {
                vitals.alerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WhoopColors.Warning.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 14.sp)
                        Text(
                            text = alert,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhoopColors.Warning
                        )
                    }
                }
            }

            // Grid de biomarcadores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                vitals.evaluations.take(4).forEach { metric ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = metric.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary,
                            maxLines = 1
                        )
                        Text(
                            text = metric.currentValue,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = if (metric.status == MetricBaselineStatus.IN_BASELINE) WhoopColors.OnSurface else WhoopColors.Warning
                        )
                        Text(
                            text = metric.baselineRangeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartRateCard(
    sample: HeartRateSample?,
    samples: List<HeartRateSampleEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val zones = remember(samples) {
        HeartRateZonesCalculator.calculateZones(samples)
    }

    EasySpotlightCard(
        spotlightAccent = WhoopColors.HeartRate,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FRECUENCIA CARDÍACA 24/7",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                val timestampText = sample?.let {
                    DashboardFormatting.formatTime(it.timestamp)
                } ?: "--:--"
                Text(
                    text = timestampText,
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sample?.bpm?.toString() ?: "--",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.HeartRate
                    )
                    Text(
                        text = "ppm",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (zones.totalSamples > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MÍN", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                            Text("${zones.minBpm}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = WhoopColors.RecoveryHigh)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MED", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                            Text("${zones.avgBpm}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = WhoopColors.OnSurface)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MÁX", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                            Text("${zones.maxBpm}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = WhoopColors.Error)
                        }
                    }
                }
            }

            // Barra apilada de las 5 zonas cardiovasculares
            if (zones.totalSamples > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DISTRIBUCIÓN EN ZONAS DE ESFUERZO",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(WhoopColors.SurfaceElevated, RoundedCornerShape(5.dp))
                    ) {
                        if (zones.zone1Pct > 0) Box(modifier = Modifier.weight(zones.zone1Pct.toFloat()).fillMaxHeight().background(WhoopColors.Zone1Recovery, RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)))
                        if (zones.zone2Pct > 0) Box(modifier = Modifier.weight(zones.zone2Pct.toFloat()).fillMaxHeight().background(WhoopColors.Zone2Aerobic))
                        if (zones.zone3Pct > 0) Box(modifier = Modifier.weight(zones.zone3Pct.toFloat()).fillMaxHeight().background(WhoopColors.Zone3Tempo))
                        if (zones.zone4Pct > 0) Box(modifier = Modifier.weight(zones.zone4Pct.toFloat()).fillMaxHeight().background(WhoopColors.Zone4Threshold))
                        if (zones.zone5Pct > 0) Box(modifier = Modifier.weight(zones.zone5Pct.toFloat()).fillMaxHeight().background(WhoopColors.Zone5Peak, RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Z1 ${zones.zone1Pct}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace), color = WhoopColors.Zone1Recovery)
                        Text("Z2 ${zones.zone2Pct}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace), color = WhoopColors.Zone2Aerobic)
                        Text("Z3 ${zones.zone3Pct}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace), color = WhoopColors.Zone3Tempo)
                        Text("Z4 ${zones.zone4Pct}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace), color = WhoopColors.Zone4Threshold)
                        Text("Z5 ${zones.zone5Pct}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace), color = WhoopColors.Zone5Peak)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightSummaryCard(
    measurement: WeightMeasurement?,
    delta7DaysKg: Double?,
    modifier: Modifier = Modifier
) {
    val deltaText = delta7DaysKg?.let {
        "7d: ${DashboardFormatting.formatDelta(it)}"
    }
    val deltaColor = when {
        delta7DaysKg == null -> WhoopColors.OnSurfaceTertiary
        delta7DaysKg < -0.0001 -> WhoopColors.RecoveryHigh
        delta7DaysKg > 0.0001 -> WhoopColors.Warning
        else -> WhoopColors.OnSurfaceSecondary
    }

    EasySpotlightCard(
        spotlightAccent = WhoopColors.Weight,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dash_biological_weight),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                val sourceText = if (measurement != null) {
                    "${measurement.source.displayName} · ${DashboardFormatting.formatTimestamp(measurement.measuredAt)}"
                } else "RENPHO Elis 1"
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rawWeight = DashboardFormatting.formatWeight(measurement?.weightKg)
                    val numericWeight = if (rawWeight.endsWith(" kg")) rawWeight.removeSuffix(" kg") else rawWeight
                    Text(
                        text = numericWeight,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Weight
                    )
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (deltaText != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = deltaColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (measurement != null) {
                HorizontalDivider(color = WhoopColors.Outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactMetric(
                        label = "Grasa",
                        value = DashboardFormatting.formatPercentage(measurement.bodyFatPercentage)
                    )
                    CompactMetric(
                        label = "Músculo",
                        value = DashboardFormatting.formatWeight(measurement.muscleMassKg)
                    )
                    val impedanceFormatted = measurement.impedanceOhms?.let {
                        "${it.toInt()} Ω"
                    } ?: "-- Ω"
                    CompactMetric(
                        label = "Impedancia",
                        value = impedanceFormatted
                    )
                }
            } else {
                Text(
                    text = "Súbete a la báscula para registrar peso y bioimpedancia automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WhoopColors.OnSurfaceTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.Bold,
            color = WhoopColors.OnSurface
        )
    }
}

@Composable
private fun SleepCard(
    sleep: SleepSessionEntity,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalMin = sleep.totalSleepMinutes
    val hours = totalMin / 60
    val minutes = totalMin % 60
    val efficiency = sleep.sleepEfficiencyScore ?: 85
    val totalInBed = (totalMin + sleep.awakeMinutes).coerceAtLeast(1)

    EasySpotlightCard(
        spotlightAccent = WhoopColors.Sleep,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .tactilePress(scaleDown = 0.96f)
                        .clickable { onClick() }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUEÑO Y RECUPERACIÓN",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Text(
                    text = sleep.sourceDevice,
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.Sleep
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = WhoopColors.Sleep.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$efficiency% eficiencia",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.Sleep,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Stacked sleep stages bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                val deepRatio = sleep.deepSleepMinutes.toFloat() / totalInBed
                val remRatio = sleep.remSleepMinutes.toFloat() / totalInBed
                val lightRatio = sleep.lightSleepMinutes.toFloat() / totalInBed
                val awakeRatio = sleep.awakeMinutes.toFloat() / totalInBed

                if (deepRatio > 0f) {
                    Box(modifier = Modifier.weight(deepRatio).fillMaxHeight().background(WhoopColors.SleepDeep))
                }
                if (remRatio > 0f) {
                    Box(modifier = Modifier.weight(remRatio).fillMaxHeight().background(WhoopColors.SleepRem))
                }
                if (lightRatio > 0f) {
                    Box(modifier = Modifier.weight(lightRatio).fillMaxHeight().background(WhoopColors.SleepLight))
                }
                if (awakeRatio > 0f) {
                    Box(modifier = Modifier.weight(awakeRatio).fillMaxHeight().background(WhoopColors.SleepAwake))
                }
            }

            // Stage metrics breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StageItem(label = "Profundo", minutes = sleep.deepSleepMinutes, color = WhoopColors.SleepDeep)
                StageItem(label = "REM", minutes = sleep.remSleepMinutes, color = WhoopColors.SleepRem)
                StageItem(label = "Ligero", minutes = sleep.lightSleepMinutes, color = WhoopColors.SleepLight)
                StageItem(label = "Despierto", minutes = sleep.awakeMinutes, color = WhoopColors.SleepAwake)
            }
        }
    }
}

@Composable
private fun NoSleepCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EasySpotlightCard(
        spotlightAccent = WhoopColors.Sleep,
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUEÑO Y DESCANSO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Text(
                    text = "Pendiente de sincronizar",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            Text(
                text = "--:--",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp
                ),
                fontWeight = FontWeight.Bold,
                color = WhoopColors.OnSurfaceTertiary
            )

            Text(
                text = "Sin registro de sueño sincronizado hoy",
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceTertiary
            )
        }
    }
}

@Composable
private fun StageItem(
    label: String,
    minutes: Int,
    color: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = WhoopColors.OnSurfaceTertiary
            )
        }
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = WhoopColors.OnSurface
        )
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
fun HoyContentPreview() {
    SaludPersonalTheme {
        HoyContent(
            state = DashboardUiState(
                watchConnectionState = DeviceConnectionState.CONNECTED,
                watchBattery = WatchBatteryStatus(85, false, Instant.now()),
                todayActivity = DailyActivitySummary(
                    date = LocalDate.now(),
                    steps = 7420,
                    distanceMeters = 4800,
                    caloriesBurned = 340,
                    sourceDevice = "Amazfit GTR 3"
                ),
                latestHeartRate = HeartRateSample(
                    timestamp = Instant.now(),
                    bpm = 72,
                    sourceDevice = "Amazfit GTR 3"
                ),
                latestMeasurement = WeightMeasurement(
                    id = 1L,
                    source = ProvenanceSource.RENPHO_ELIS_1,
                    weightKg = 74.85,
                    bodyFatPercentage = 18.2f,
                    muscleMassKg = 58.10,
                    impedanceOhms = 502.0,
                    measuredAt = Instant.now(),
                    importedAt = Instant.now()
                ),
                delta7DaysKg = -0.40
            )
        )
    }
}

// Backward compatibility: keep old preview
@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
fun DashboardContentPreview() {
    HoyContentPreview()
}
