package com.salud.personal.ui.activity

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.data.local.entity.HourlyStepsEntity
import com.salud.personal.domain.activity.StepGoalCalculator
import com.salud.personal.ui.components.WhoopProgressRing
import com.salud.personal.ui.dashboard.DashboardFormatting
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import java.time.LocalDate

/**
 * Pantalla de detalle de actividad diaria y meta de 10.000 pasos.
 */
@Composable
fun ActivityDetailScreen(
    currentSteps: Int,
    goal: Int = StepGoalCalculator.DEFAULT_STEP_GOAL,
    distanceMeters: Int = 0,
    caloriesBurned: Int = 0,
    hourlySteps: List<HourlyStepsEntity> = emptyList(),
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val remainingSteps = StepGoalCalculator.calculateRemainingSteps(currentSteps, goal)
    val progressRatio = StepGoalCalculator.calculateProgressRatio(currentSteps, goal)
    val minutesRemaining = StepGoalCalculator.estimateMinutesRemaining(remainingSteps)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.tactilePress(scaleDown = 0.94f)
                ) {
                    Text("← Volver a Hoy", color = WhoopColors.OnSurface)
                }
            }
        }

        // Main Ring Card con EasySpotlightCard
        EasySpotlightCard(
            spotlightAccent = if (currentSteps >= goal) WhoopColors.RecoveryHigh else WhoopColors.Strain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "META DIARIA DE PASOS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                WhoopProgressRing(
                    progress = progressRatio,
                    displayValue = DashboardFormatting.formatSteps(currentSteps),
                    displayUnit = "de ${DashboardFormatting.formatSteps(goal)}",
                    accentColor = if (currentSteps >= goal) WhoopColors.RecoveryHigh else WhoopColors.Strain,
                    trackColor = WhoopColors.StrainTrack,
                    ringSize = 200.dp,
                    strokeWidth = 14.dp
                )

                if (remainingSteps > 0) {
                    Text(
                        text = "Faltan ${DashboardFormatting.formatSteps(remainingSteps)} pasos (~$minutesRemaining min a paso normal)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = WhoopColors.Strain
                    )
                } else {
                    Text(
                        text = "Meta de 10.000 pasos completada ✓",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.RecoveryHigh
                    )
                }

                HorizontalDivider(color = WhoopColors.Outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Distancia",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = DashboardFormatting.formatDistance(distanceMeters),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Calorías",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = DashboardFormatting.formatCalories(caloriesBurned),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                    }
                }
            }
        }

        // Hourly Histogram Card con EasySpotlightCard
        EasySpotlightCard(
            spotlightAccent = WhoopColors.Strain,
            modifier = Modifier.fillMaxWidth()
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
                        text = "DISTRIBUCIÓN HORARIA",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary
                    )
                    if (hourlySteps.isNotEmpty()) {
                        val activeHours = hourlySteps.count { it.steps > 0 }
                        Text(
                            text = "$activeHours h activas",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.Strain
                        )
                    }
                }

                if (hourlySteps.isNotEmpty()) {
                    HourlyStepsHistogram(hourlySteps = hourlySteps)

                    // Resumen de pico horario
                    val blockTotals = (0 until 12).map { block ->
                        val startHour = block * 2
                        hourlySteps
                            .filter { it.hour == startHour || it.hour == startHour + 1 }
                            .sumOf { it.steps }
                    }
                    val peakBlock = blockTotals.indices.maxByOrNull { blockTotals[it] } ?: 0
                    val peakSteps = blockTotals[peakBlock]

                    if (peakSteps > 0) {
                        val peakStart = peakBlock * 2
                        val peakEnd = peakStart + 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tramo más activo",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Text(
                                text = "${peakStart}:00–${peakEnd}:00 (${DashboardFormatting.formatSteps(peakSteps)} pasos)",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.OnSurface
                            )
                        }
                    }
                } else {
                    HourlyStepsHistogram(hourlySteps = emptyList())
                    Text(
                        text = "Sin desglose horario disponible hoy. Sincroniza Zepp en Health Connect para actualizar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Histograma de barras para las 24 horas del día (agrupadas en 12 bloques de 2h).
 * Incluye inspección táctil (scrubbing/tap) para ver el tramo exacto y su porcentaje.
 */
@Composable
fun HourlyStepsHistogram(
    hourlySteps: List<HourlyStepsEntity>,
    modifier: Modifier = Modifier
) {
    // 12 bloques de 2 horas (0h..22h)
    val blockTotals = (0 until 12).map { block ->
        val startHour = block * 2
        hourlySteps
            .filter { it.hour == startHour || it.hour == startHour + 1 }
            .sumOf { it.steps }
    }
    val totalSteps = blockTotals.sum().coerceAtLeast(1)
    val maxBlockSteps = (blockTotals.maxOrNull() ?: 1).coerceAtLeast(100)
    var selectedBlock by remember { mutableStateOf<Int?>(null) }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "hourlyStepsAnim"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Píldora de inspección táctil (Scrubbing / Tapping)
        selectedBlock?.let { block ->
            val startHour = block * 2
            val endHour = startHour + 2
            val stepsInBlock = blockTotals[block]
            val pct = ((stepsInBlock.toDouble() / totalSteps) * 100).toInt()

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WhoopColors.SurfaceElevated,
                border = BorderStroke(1.dp, WhoopColors.Strain.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tramo $startHour:00–$endHour:00",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Strain
                    )
                    Text(
                        text = "$stepsInBlock pasos ($pct% del total)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface
                    )
                }
            }
        }

        // 1. Fila de barras con etiquetas de pasos numéricas y selección táctil
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (block in 0 until 12) {
                val stepsInBlock = blockTotals[block]
                val isSelected = selectedBlock == block
                val barHeightRatio = if (stepsInBlock > 0) {
                    (stepsInBlock.toFloat() / maxBlockSteps).coerceIn(0.12f, 1f)
                } else {
                    0.06f
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            selectedBlock = if (selectedBlock == block) null else block
                        }
                        .tactilePress(scaleDown = 0.95f)
                ) {
                    if (stepsInBlock > 0) {
                        val label = if (stepsInBlock >= 1000) {
                            String.format(java.util.Locale.US, "%.1fk", stepsInBlock / 1000f)
                        } else {
                            "$stepsInBlock"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = if (isSelected) WhoopColors.Strain else WhoopColors.OnSurfaceSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 14.dp else 12.dp)
                            .fillMaxHeight((barHeightRatio * 0.85f * animProgress).coerceAtLeast(0.02f))
                            .background(
                                color = when {
                                    isSelected -> WhoopColors.Strain
                                    stepsInBlock > 0 -> WhoopColors.Strain.copy(alpha = 0.8f)
                                    else -> WhoopColors.StrainTrack
                                },
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            }
        }

        // 2. Fila de horas alineadas exactamente debajo de cada barra
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (block in 0 until 12) {
                val startHour = block * 2
                val isSelected = selectedBlock == block
                val stepsInBlock = blockTotals[block]
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedBlock = if (selectedBlock == block) null else block
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${startHour}h",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) WhoopColors.Strain else if (stepsInBlock > 0) WhoopColors.OnSurface else WhoopColors.OnSurfaceTertiary
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun ActivityDetailScreenPreview() {
    SaludPersonalTheme {
        val sampleHours = listOf(
            HourlyStepsEntity(date = LocalDate.now(), hour = 8, steps = 1400, sourceDevice = "GTR 3"),
            HourlyStepsEntity(date = LocalDate.now(), hour = 9, steps = 800, sourceDevice = "GTR 3"),
            HourlyStepsEntity(date = LocalDate.now(), hour = 13, steps = 2200, sourceDevice = "GTR 3"),
            HourlyStepsEntity(date = LocalDate.now(), hour = 18, steps = 3100, sourceDevice = "GTR 3")
        )
        ActivityDetailScreen(
            currentSteps = 7500,
            goal = 10000,
            distanceMeters = 5200,
            caloriesBurned = 390,
            hourlySteps = sampleHours
        )
    }
}
