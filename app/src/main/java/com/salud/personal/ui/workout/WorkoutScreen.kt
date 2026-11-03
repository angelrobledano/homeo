package com.salud.personal.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.training.TrainingLoadCalculator
import com.salud.personal.ui.dashboard.DashboardFormatting
import com.salud.personal.ui.easyui.EasyNumberTicker
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class WorkoutTimeFilter(val label: String, val days: Long?, val cardTitle: String) {
    LAST_7_DAYS("7 días", 7L, "CARGA DE ENTRENAMIENTO (7 DÍAS)"),
    LAST_30_DAYS("30 días", 30L, "CARGA DE ENTRENAMIENTO (30 DÍAS)"),
    ALL("Histórico", null, "CARGA TOTAL ACUMULADA")
}

enum class WorkoutCategoryFilter(val label: String, val type: WorkoutType?) {
    ALL("Todos", null),
    CROSSFIT("CrossFit / WOD", WorkoutType.CROSSFIT_WOD),
    STRENGTH("Fuerza", WorkoutType.STRENGTH),
    CARDIO("Cardio", WorkoutType.ENDURANCE_CARDIO),
    WALK("Caminatas", WorkoutType.WALK)
}

/**
 * Pantalla principal de la pestaña "Entrenamiento".
 * Integra el control de sesión en vivo (TRIMP, FC instantánea/máxima), métricas del periodo
 * y un historial paginado con filtros para evitar sobrecarga visual y lag en scroll.
 */
@Composable
fun WorkoutScreen(
    workouts: List<WorkoutSessionEntity> = emptyList(),
    activeWorkout: ActiveWorkoutState = ActiveWorkoutState(),
    onStartWorkout: (WorkoutType, String) -> Unit = { _, _ -> },
    onFinishWorkout: (Int, String?) -> Unit = { _, _ -> },
    onCancelWorkout: () -> Unit = {},
    onAddManualWorkout: ((WorkoutType, String, Int, Int, String?) -> Unit)? = null,
    onOpenGpsWalk: (() -> Unit)? = null,
    onSyncHealthConnect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var timeFilter by remember { mutableStateOf(WorkoutTimeFilter.LAST_7_DAYS) }
    var categoryFilter by remember { mutableStateOf(WorkoutCategoryFilter.ALL) }
    var visibleLimit by remember { mutableIntStateOf(5) }
    var showTrimpExplanation by remember { mutableStateOf(false) }
    var showManualWorkoutDialog by remember { mutableStateOf(false) }
    var horizontalFilterDrag by remember { mutableFloatStateOf(0f) }

    if (showManualWorkoutDialog && onAddManualWorkout != null) {
        ManualWorkoutDialog(
            onDismiss = { showManualWorkoutDialog = false },
            onSaveWorkout = { type, name, duration, rpe, notes ->
                onAddManualWorkout(type, name, duration, rpe, notes)
            }
        )
    }

    val filteredWorkouts = remember(workouts, timeFilter, categoryFilter) {
        val now = Instant.now()
        workouts.filter { session ->
            val matchesTime = when (val days = timeFilter.days) {
                null -> true
                else -> session.startTime >= now.minus(days, ChronoUnit.DAYS)
            }
            val matchesCategory = when (val targetType = categoryFilter.type) {
                null -> true
                else -> session.type == targetType
            }
            matchesTime && matchesCategory
        }
    }

    val visibleWorkouts = remember(filteredWorkouts, visibleLimit) {
        filteredWorkouts.take(visibleLimit)
    }

    val periodTrimp = remember(filteredWorkouts) {
        filteredWorkouts.sumOf { it.trimp ?: 0.0 }
    }
    val periodMinutes = remember(filteredWorkouts) {
        filteredWorkouts.sumOf { it.durationMinutes }
    }
    val avgTrimp = remember(filteredWorkouts, periodTrimp) {
        if (filteredWorkouts.isNotEmpty()) periodTrimp / filteredWorkouts.size else 0.0
    }

    val weeklyLoadLevel = remember(periodTrimp, timeFilter) {
        if (timeFilter == WorkoutTimeFilter.LAST_7_DAYS) {
            TrainingLoadCalculator.evaluateWeeklyLoad(periodTrimp)
        } else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Panel de Control / Grabador de Sesión Activa
            ActiveWorkoutControlCard(
                activeWorkout = activeWorkout,
                onStartWorkout = onStartWorkout,
                onFinishWorkout = onFinishWorkout,
                onCancelWorkout = onCancelWorkout,
                onOpenGpsWalk = onOpenGpsWalk,
                onOpenManualWorkout = if (onAddManualWorkout != null) { { showManualWorkoutDialog = true } } else null
            )
        }

        item {
            // Header: Carga de entrenamiento contextualizada con EasyUI Spotlight
            EasySpotlightCard(
                spotlightAccent = WhoopColors.Strain,
                modifier = Modifier.fillMaxWidth()
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
                            text = timeFilter.cardTitle,
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.OnSurfaceSecondary,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (weeklyLoadLevel != null) {
                            val (badgeColor, badgeBg) = when (weeklyLoadLevel) {
                                TrainingLoadCalculator.LoadLevel.RECOVERY -> Pair(WhoopColors.Strain, WhoopColors.Strain.copy(alpha = 0.15f))
                                TrainingLoadCalculator.LoadLevel.OPTIMAL -> Pair(WhoopColors.RecoveryHigh, WhoopColors.RecoveryHigh.copy(alpha = 0.15f))
                                TrainingLoadCalculator.LoadLevel.HIGH -> Pair(WhoopColors.RecoveryMedium, WhoopColors.RecoveryMedium.copy(alpha = 0.15f))
                                TrainingLoadCalculator.LoadLevel.EXTREME -> Pair(WhoopColors.RecoveryLow, WhoopColors.RecoveryLow.copy(alpha = 0.15f))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = weeklyLoadLevel.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EasyNumberTicker(
                            value = String.format(Locale.US, "%.0f", periodTrimp),
                            style = MaterialTheme.typography.displayMedium.copy(
                                letterSpacing = (-1).sp
                            ),
                            color = WhoopColors.Strain
                        )
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "pts TRIMP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WhoopColors.OnSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WhoopColors.SurfaceElevated,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                                        .tactilePress(scaleDown = 0.92f)
                                        .clickable { showTrimpExplanation = !showTrimpExplanation }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (showTrimpExplanation) "Ocultar" else "¿Qué es?",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WhoopColors.Strain,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Impulso Cardiovascular Banister",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }

                    if (weeklyLoadLevel != null) {
                        Text(
                            text = weeklyLoadLevel.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhoopColors.OnSurfaceSecondary
                        )

                        val levelColor = when (weeklyLoadLevel) {
                            TrainingLoadCalculator.LoadLevel.RECOVERY -> WhoopColors.Strain
                            TrainingLoadCalculator.LoadLevel.OPTIMAL -> WhoopColors.RecoveryHigh
                            TrainingLoadCalculator.LoadLevel.HIGH -> WhoopColors.RecoveryMedium
                            TrainingLoadCalculator.LoadLevel.EXTREME -> WhoopColors.RecoveryLow
                        }

                        // Barra visual de zonas de carga semanal con cursor de posición actual
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            WeeklyLoadSpectrumBar(
                                currentLoad = periodTrimp,
                                accentColor = levelColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("0", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                                Text("150 (Manten.)", style = MaterialTheme.typography.labelSmall, color = WhoopColors.Strain)
                                Text("450 (Óptimo)", style = MaterialTheme.typography.labelSmall, color = WhoopColors.RecoveryHigh)
                                Text("650+ (Sobrecarga)", style = MaterialTheme.typography.labelSmall, color = WhoopColors.RecoveryLow)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showTrimpExplanation,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WhoopColors.SurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "¿CÓMO SE MIDE EL ESFUERZO TRIMP?",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = WhoopColors.Strain
                                )
                                Text(
                                    text = "El Impulso de Entrenamiento (TRIMP) mide el impacto fisiológico real en tu sistema cardiovascular multiplicando la duración por la intensidad del pulso cardíaco.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhoopColors.OnSurface
                                )
                                Text(
                                    text = "• Pondera exponencialmente el tiempo en zonas anaeróbicas (Z4 y Z5). 1h de paseo suave suma ~35 pts, mientras que 1h de Crosstraining/WOD intenso suma ~130 pts.\n• Rango semanal óptimo: 150 a 450 pts para estimular adaptación y supercompensación sin sobreentrenar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhoopColors.OnSurfaceSecondary
                                )
                            }
                        }
                    }

                    // Tira estadística rápida de 3 columnas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WhoopColors.Surface, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SESIONES",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Text(
                                text = "${filteredWorkouts.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.OnSurface
                            )
                        }

                        val hours = (periodMinutes / 60).toInt()
                        val mins = (periodMinutes % 60).toInt()
                        val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                        Column {
                            Text(
                                text = "TIEMPO TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.OnSurface
                            )
                        }

                        Column {
                            Text(
                                text = "MEDIA / SESIÓN",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Text(
                                text = "${avgTrimp.toInt()} pts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Strain
                            )
                        }
                    }
                }
            }
        }

        // Filtros de Periodo Temporal y Deporte
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Selector de Periodo Temporal con soporte de Gestos Swipe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(timeFilter) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (horizontalFilterDrag < -40f) {
                                        val next = (timeFilter.ordinal + 1).coerceAtMost(WorkoutTimeFilter.entries.size - 1)
                                        timeFilter = WorkoutTimeFilter.entries[next]
                                        visibleLimit = 5
                                    } else if (horizontalFilterDrag > 40f) {
                                        val prev = (timeFilter.ordinal - 1).coerceAtLeast(0)
                                        timeFilter = WorkoutTimeFilter.entries[prev]
                                        visibleLimit = 5
                                    }
                                    horizontalFilterDrag = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalFilterDrag += dragAmount
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutTimeFilter.entries.forEach { filter ->
                        val selected = timeFilter == filter
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) WhoopColors.Strain else WhoopColors.Surface,
                            modifier = Modifier
                                .weight(1f)
                                .tactilePress(scaleDown = 0.94f)
                                .clickable {
                                    timeFilter = filter
                                    visibleLimit = 5
                                }
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) WhoopColors.Background else WhoopColors.OnSurfaceSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Selector de Categoría Deportiva (Scroll horizontal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutCategoryFilter.entries.forEach { cat ->
                        val selected = categoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) WhoopColors.SurfaceElevated else WhoopColors.Surface,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, WhoopColors.Strain) else null,
                            modifier = Modifier
                                .tactilePress(scaleDown = 0.94f)
                                .clickable {
                                    categoryFilter = cat
                                    visibleLimit = 5
                                }
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) WhoopColors.Strain else WhoopColors.OnSurfaceSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORIAL (${filteredWorkouts.size})",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                if (onSyncHealthConnect != null) {
                    TextButton(
                        onClick = onSyncHealthConnect,
                        modifier = Modifier.tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Sincronizar Zepp",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.Strain
                        )
                    }
                }
            }
        }

        if (filteredWorkouts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WhoopColors.Surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sin entrenamientos en el periodo seleccionado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WhoopColors.OnSurfaceTertiary,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(visibleWorkouts, key = { it.id }) { session ->
                WorkoutSessionCard(session = session)
            }

            if (filteredWorkouts.size > visibleWorkouts.size) {
                item {
                    val remaining = filteredWorkouts.size - visibleWorkouts.size
                    OutlinedButton(
                        onClick = { visibleLimit += 10 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactilePress(scaleDown = 0.97f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WhoopColors.Strain
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhoopColors.Strain.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Mostrar más (+10) · Quedan $remaining",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else if (visibleLimit > 5 && filteredWorkouts.size > 5) {
                item {
                    TextButton(
                        onClick = { visibleLimit = 5 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactilePress(scaleDown = 0.96f)
                    ) {
                        Text(
                            text = "Mostrar menos (primeros 5)",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActiveWorkoutControlCard(
    activeWorkout: ActiveWorkoutState,
    onStartWorkout: (WorkoutType, String) -> Unit,
    onFinishWorkout: (Int, String?) -> Unit,
    onCancelWorkout: () -> Unit,
    onOpenGpsWalk: (() -> Unit)? = null,
    onOpenManualWorkout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EasySpotlightCard(
        spotlightAccent = if (activeWorkout.isRecording) WhoopColors.HeartRate else WhoopColors.Strain,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!activeWorkout.isRecording) {
                // Estado inactivo: Botones rápidos para lanzar WOD CrossFit o Endurance
                Text(
                    text = "NUEVA SESIÓN",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onStartWorkout(WorkoutType.CROSSFIT_WOD, "CrossFit WOD") },
                        colors = ButtonDefaults.buttonColors(containerColor = WhoopColors.Strain),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "CrossFit",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    OutlinedButton(
                        onClick = { onStartWorkout(WorkoutType.ENDURANCE_CARDIO, "Endurance") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Endurance",
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.OnSurface
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onOpenGpsWalk != null) {
                        OutlinedButton(
                            onClick = onOpenGpsWalk,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = WhoopColors.SurfaceElevated,
                                contentColor = WhoopColors.Strain
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .tactilePress(scaleDown = 0.94f)
                        ) {
                            Text(
                                text = "GPS Exterior ↗",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (onOpenManualWorkout != null) {
                        OutlinedButton(
                            onClick = onOpenManualWorkout,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = WhoopColors.SurfaceElevated,
                                contentColor = WhoopColors.OnSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .tactilePress(scaleDown = 0.94f)
                        ) {
                            Text(
                                text = "+ Manual RPE",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                // Estado en grabación activa
                var elapsedSeconds by remember { mutableStateOf(0L) }

                LaunchedEffect(activeWorkout.startTime) {
                    while (true) {
                        val start = activeWorkout.startTime ?: Instant.now()
                        elapsedSeconds = java.time.Duration.between(start, Instant.now()).seconds
                        delay(1000)
                    }
                }

                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EN VIVO · ${activeWorkout.name.uppercase()}",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.RecoveryHigh
                    )

                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = WhoopColors.OnSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FC ACTUAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = activeWorkout.currentHeartRate?.let { "$it ppm" } ?: "-- ppm",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.HeartRate
                        )
                    }

                    Column {
                        Text(
                            text = "FC MEDIA / MÁX",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = "${activeWorkout.avgHeartRate ?: "--"} / ${activeWorkout.maxHeartRate ?: "--"}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.OnSurfaceSecondary
                        )
                    }

                    Column {
                        Text(
                            text = "ESFUERZO EN VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", activeWorkout.estimatedTrimp),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onFinishWorkout(8, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = WhoopColors.RecoveryHigh),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Finalizar",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    OutlinedButton(
                        onClick = onCancelWorkout,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Descartar",
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.RecoveryLow
                        )
                    }
                }
            }
        }
    }
}

private fun formatSmartDate(instant: Instant?): String {
    if (instant == null) return "--"
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val time = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES")).withZone(zone).format(instant)
    return when {
        date == today -> "Hoy, $time"
        date == today.minusDays(1) -> "Ayer, $time"
        date.year == today.year -> DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("es", "ES")).withZone(zone).format(instant)
        else -> DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("es", "ES")).withZone(zone).format(instant)
    }
}

@Composable
fun WorkoutSessionCard(
    session: WorkoutSessionEntity,
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeColor) = when (session.type) {
        WorkoutType.CROSSFIT_WOD -> Pair("WOD", WhoopColors.Strain)
        WorkoutType.STRENGTH -> Pair("FUERZA", WhoopColors.RecoveryMedium)
        WorkoutType.ENDURANCE_CARDIO -> Pair("CARDIO", WhoopColors.RecoveryLow)
        WorkoutType.WALK -> Pair("PASEO", WhoopColors.OnSurfaceTertiary)
        WorkoutType.RECOVERY -> Pair("RECUP.", WhoopColors.RecoveryHigh)
    }

    EasySpotlightCard(
        spotlightAccent = badgeColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface,
                        maxLines = 1
                    )
                }
                Text(
                    text = formatSmartDate(session.startTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.durationMinutes.toInt()} min · RPE ${session.rpe}/10",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WhoopColors.OnSurfaceSecondary
                )
                if (session.trimp != null && session.trimp > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WhoopColors.Strain.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.0f", session.trimp)} pts",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (session.avgHeartRate != null || (session.caloriesBurned != null && session.caloriesBurned > 0)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session.avgHeartRate != null) {
                        Text(
                            text = "FC: ${session.avgHeartRate} ppm ${session.maxHeartRate?.let { "(máx $it)" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.HeartRate
                        )
                    }
                    if (session.caloriesBurned != null && session.caloriesBurned > 0) {
                        Text(
                            text = "· ${session.caloriesBurned} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceSecondary
                        )
                    }
                }
            }

            if (!session.notes.isNullOrBlank()) {
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }
    }
}

/**
 * Barra continua de espectro de carga con zonas fisiológicas y cursor indicador del atleta.
 */
@Composable
private fun WeeklyLoadSpectrumBar(
    currentLoad: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animLoad by animateFloatAsState(
        targetValue = currentLoad.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "weeklyLoadSpectrumAnim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val barH = 8.dp.toPx()
        val topY = (size.height - barH) / 2f
        val maxScale = 800f

        fun trimpToX(t: Float): Float {
            return (t.coerceIn(0f, maxScale) / maxScale) * w
        }

        val x150 = trimpToX(150f)
        val x450 = trimpToX(450f)
        val x650 = trimpToX(650f)

        // 0-150 Mantenimiento (Cyan)
        drawRoundRect(
            color = WhoopColors.Strain.copy(alpha = 0.5f),
            topLeft = Offset(0f, topY),
            size = Size(x150, barH),
            cornerRadius = CornerRadius(barH / 2, barH / 2)
        )
        // 150-450 Óptimo (Verde)
        drawRect(
            color = WhoopColors.RecoveryHigh,
            topLeft = Offset(x150 + 2f, topY),
            size = Size((x450 - x150 - 4f).coerceAtLeast(1f), barH)
        )
        // 450-650 Alta (Ámbar)
        drawRect(
            color = WhoopColors.RecoveryMedium,
            topLeft = Offset(x450 + 2f, topY),
            size = Size((x650 - x450 - 4f).coerceAtLeast(1f), barH)
        )
        // 650+ Excesiva (Rojo)
        drawRoundRect(
            color = WhoopColors.RecoveryLow.copy(alpha = 0.7f),
            topLeft = Offset(x650 + 2f, topY),
            size = Size((w - x650 - 2f).coerceAtLeast(1f), barH),
            cornerRadius = CornerRadius(barH / 2, barH / 2)
        )

        // Puntero / cursor de carga actual animado
        val pointerX = trimpToX(animLoad).coerceIn(6.dp.toPx(), w - 6.dp.toPx())
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(pointerX, topY + barH / 2)
        )
        drawCircle(
            color = accentColor,
            radius = 4.dp.toPx(),
            center = Offset(pointerX, topY + barH / 2)
        )
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun WorkoutScreenPreview() {
    SaludPersonalTheme {
        val now = Instant.now()
        val dummyList = listOf(
            WorkoutSessionEntity(
                id = 1L,
                type = WorkoutType.CROSSFIT_WOD,
                name = "Fran (21-15-9)",
                startTime = now.minus(3, ChronoUnit.HOURS),
                endTime = now.minus(3, ChronoUnit.HOURS).plus(6, ChronoUnit.MINUTES),
                durationMinutes = 6.2,
                avgHeartRate = 175,
                maxHeartRate = 191,
                trimp = 28.4,
                caloriesBurned = 140,
                rpe = 10,
                notes = "Thrusters 43kg + Pull-ups unbroken"
            ),
            WorkoutSessionEntity(
                id = 2L,
                type = WorkoutType.ENDURANCE_CARDIO,
                name = "5km Carrera continua",
                startTime = now.minus(28, ChronoUnit.HOURS),
                endTime = now.minus(28, ChronoUnit.HOURS).plus(26, ChronoUnit.MINUTES),
                durationMinutes = 26.0,
                avgHeartRate = 154,
                maxHeartRate = 168,
                trimp = 45.2,
                caloriesBurned = 310,
                rpe = 7,
                notes = "Ritmo estable 5:12/km"
            )
        )
        WorkoutScreen(workouts = dummyList)
    }
}
