package com.salud.personal.ui.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.domain.sleep.RestorativeSleepResult
import com.salud.personal.domain.sleep.SleepAnalysisCalculator
import com.salud.personal.domain.sleep.SleepQualityTier
import com.salud.personal.domain.sleep.SleepStage
import com.salud.personal.domain.sleep.StageBalance
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla de estudio y análisis detallado de sueño y descanso.
 * Presenta hipnograma por fases, indicador de sueño restaurativo (profundo + REM),
 * cálculo de deuda de sueño ajustada por el esfuerzo cardiovascular (TRIMP) e historial de 7 noches.
 */
@Composable
fun SleepDetailScreen(
    todaySleep: SleepSessionEntity?,
    recentSleep: List<SleepSessionEntity> = emptyList(),
    recentWorkouts: List<WorkoutSessionEntity> = emptyList(),
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Si no hay sesión para hoy, tomar el registro más reciente real disponible o null
    val targetSleep = todaySleep ?: recentSleep.firstOrNull()
    val isToday = todaySleep != null

    if (targetSleep == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WhoopColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhoopColors.OnSurface),
                    modifier = Modifier.tactilePress(scaleDown = 0.94f)
                ) {
                    Text("← Volver a Hoy", color = WhoopColors.OnSurface)
                }
            }
            EasySpotlightCard(
                spotlightAccent = WhoopColors.Sleep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SIN REGISTROS DE SUEÑO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary
                    )
                    Text(
                        text = "Conecta y sincroniza tu reloj Amazfit para registrar y analizar tus ciclos de sueño, descanso profundo y fases REM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WhoopColors.OnSurfaceTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val sleep = targetSleep
    val totalMin = sleep.totalSleepMinutes
    val totalHours = totalMin / 60
    val totalRemainingMin = totalMin % 60
    val efficiency = sleep.sleepEfficiencyScore ?: 85
    val totalInBed = (totalMin + sleep.awakeMinutes).coerceAtLeast(1)

    val restorative = SleepAnalysisCalculator.calculateRestorativeSleep(
        deepMinutes = sleep.deepSleepMinutes,
        remMinutes = sleep.remSleepMinutes,
        totalMinutes = totalMin
    )

    val recentTrimp = recentWorkouts.take(2).sumOf { it.trimp ?: 0.0 }
    val sleepNeedHours = SleepAnalysisCalculator.calculateSleepNeed(baseHours = 7.75, recentTrimp = recentTrimp)
    val actualHours = totalMin.toDouble() / 60.0
    val sleepDebtHours = SleepAnalysisCalculator.calculateSleepDebt(sleepNeedHours, actualHours)

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())
    val startFormatted = timeFormatter.format(sleep.startTime)
    val endFormatted = timeFormatter.format(sleep.endTime)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WhoopColors.OnSurface
                    ),
                    modifier = Modifier.tactilePress(scaleDown = 0.94f)
                ) {
                    Text("← Volver a Hoy", color = WhoopColors.OnSurface)
                }
            }
        }

        // Resumen Principal de Sueño con EasyUI Spotlight
        EasySpotlightCard(
            spotlightAccent = WhoopColors.Sleep,
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
                        text = "ESTUDIO DE SUEÑO",
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
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${totalHours}h ${totalRemainingMin}m",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Sleep
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            efficiency >= 85 -> WhoopColors.RecoveryHigh.copy(alpha = 0.15f)
                            efficiency >= 75 -> WhoopColors.Warning.copy(alpha = 0.15f)
                            else -> WhoopColors.Error.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "$efficiency% eficiencia",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                efficiency >= 85 -> WhoopColors.RecoveryHigh
                                efficiency >= 75 -> WhoopColors.Warning
                                else -> WhoopColors.Error
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "$startFormatted → $endFormatted · ${totalInBed / 60}h ${totalInBed % 60}m en cama",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceSecondary
                )
            }
        }

        // Fases del Sueño e Hipnograma
        EasySpotlightCard(
            spotlightAccent = WhoopColors.SleepRem,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "DESGLOSE DE FASES E HIPNOGRAMA",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                // Barra apilada proporcional de fases
                val deepRatio = sleep.deepSleepMinutes.toFloat() / totalInBed
                val remRatio = sleep.remSleepMinutes.toFloat() / totalInBed
                val lightRatio = sleep.lightSleepMinutes.toFloat() / totalInBed
                val awakeRatio = sleep.awakeMinutes.toFloat() / totalInBed

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(WhoopColors.SurfaceElevated, RoundedCornerShape(7.dp))
                ) {
                    if (deepRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(deepRatio)
                                .fillMaxHeight()
                                .background(WhoopColors.SleepDeep, RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp))
                        )
                    }
                    if (remRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(remRatio)
                                .fillMaxHeight()
                                .background(WhoopColors.SleepRem)
                        )
                    }
                    if (lightRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(lightRatio)
                                .fillMaxHeight()
                                .background(WhoopColors.SleepLight)
                        )
                    }
                    if (awakeRatio > 0) {
                        Box(
                            modifier = Modifier
                                .weight(awakeRatio)
                                .fillMaxHeight()
                                .background(WhoopColors.SleepAwake, RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp))
                        )
                    }
                }

                // Lista de Fases con evaluación fisiológica
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StageDetailRow(
                        name = "Sueño Profundo",
                        minutes = sleep.deepSleepMinutes,
                        totalMinutes = totalMin,
                        color = WhoopColors.SleepDeep,
                        balance = SleepAnalysisCalculator.assessStage(SleepStage.DEEP, sleep.deepSleepMinutes, totalMin),
                        targetRange = "15 - 25%",
                        benefit = "Reparación muscular, hormona de crecimiento e inmunidad"
                    )

                    HorizontalDivider(color = WhoopColors.Outline)

                    StageDetailRow(
                        name = "Sueño REM",
                        minutes = sleep.remSleepMinutes,
                        totalMinutes = totalMin,
                        color = WhoopColors.SleepRem,
                        balance = SleepAnalysisCalculator.assessStage(SleepStage.REM, sleep.remSleepMinutes, totalMin),
                        targetRange = "20 - 25%",
                        benefit = "Consolidación de memoria, aprendizaje y equilibrio neural"
                    )

                    HorizontalDivider(color = WhoopColors.Outline)

                    StageDetailRow(
                        name = "Sueño Ligero",
                        minutes = sleep.lightSleepMinutes,
                        totalMinutes = totalMin,
                        color = WhoopColors.SleepLight,
                        balance = SleepAnalysisCalculator.assessStage(SleepStage.LIGHT, sleep.lightSleepMinutes, totalMin),
                        targetRange = "45 - 55%",
                        benefit = "Mantenimiento fisiológico y descanso del sistema cardiovascular"
                    )

                    HorizontalDivider(color = WhoopColors.Outline)

                    StageDetailRow(
                        name = "Despierto / Micro-despertares",
                        minutes = sleep.awakeMinutes,
                        totalMinutes = totalInBed,
                        color = WhoopColors.SleepAwake,
                        balance = SleepAnalysisCalculator.assessStage(SleepStage.AWAKE, sleep.awakeMinutes, totalInBed),
                        targetRange = "< 10%",
                        benefit = "Interrupciones normales entre ciclos circadianos"
                    )
                }
            }
        }

        // Tarjeta de Sueño Restaurativo (Profundo + REM)
        EasySpotlightCard(
            spotlightAccent = WhoopColors.RecoveryHigh,
            modifier = Modifier.fillMaxWidth()
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
                        text = "SUEÑO RESTAURATIVO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (restorative.tier) {
                            SleepQualityTier.EXCELLENT -> WhoopColors.RecoveryHigh.copy(alpha = 0.15f)
                            SleepQualityTier.GOOD -> WhoopColors.Warning.copy(alpha = 0.15f)
                            SleepQualityTier.INSUFFICIENT -> WhoopColors.Error.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = when (restorative.tier) {
                                SleepQualityTier.EXCELLENT -> "Excelente"
                                SleepQualityTier.GOOD -> "Aceptable"
                                SleepQualityTier.INSUFFICIENT -> "Bajo"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = when (restorative.tier) {
                                SleepQualityTier.EXCELLENT -> WhoopColors.RecoveryHigh
                                SleepQualityTier.GOOD -> WhoopColors.Warning
                                SleepQualityTier.INSUFFICIENT -> WhoopColors.Error
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${restorative.restorativeMinutes / 60}h ${restorative.restorativeMinutes % 60}m",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.RecoveryHigh
                    )
                    Text(
                        text = "(${restorative.percentage}% del total)",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = "El 40% o más de sueño restaurativo optimiza la regeneración física y el rendimiento deportivo del día.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }

        // Balance y Deuda de Sueño vs Esfuerzo de Entreno
        EasySpotlightCard(
            spotlightAccent = if (sleepDebtHours <= 0.25) WhoopColors.RecoveryHigh else WhoopColors.Warning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BALANCE Y DEUDA DE SUEÑO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Necesidad Estimada",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        val needH = sleepNeedHours.toInt()
                        val needM = ((sleepNeedHours - needH) * 60).toInt()
                        Text(
                            text = "${needH}h ${needM}m",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        if (recentTrimp > 80.0) {
                            Text(
                                text = "+${if (recentTrimp > 150) "30" else "15"}m por carga de entreno",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.Strain
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Déficit Acumulado",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        val debtMin = (sleepDebtHours * 60).toInt()
                        Text(
                            text = if (debtMin > 0) "${debtMin}m" else "0m (Cumplido)",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = if (debtMin > 30) WhoopColors.Warning else WhoopColors.RecoveryHigh
                        )
                        Text(
                            text = if (debtMin > 0) "Fatiga residual leve" else "Descanso suficiente",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }
                }
            }
        }

        // Historial de 7 Noches
        if (recentSleep.isNotEmpty()) {
            EasySpotlightCard(
                spotlightAccent = WhoopColors.Sleep,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "HISTORIAL DE DESCANSO (7 DÍAS)",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary
                    )

                    Sleep7DaysChart(
                        sessions = recentSleep.take(7).reversed(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StageDetailRow(
    name: String,
    minutes: Int,
    totalMinutes: Int,
    color: Color,
    balance: StageBalance,
    targetRange: String,
    benefit: String
) {
    val pct = if (totalMinutes > 0) ((minutes.toDouble() / totalMinutes) * 100).toInt() else 0
    val hours = minutes / 60
    val remMin = minutes % 60
    val durationText = if (hours > 0) "${hours}h ${remMin}m" else "${remMin}m"

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(10.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$durationText ($pct%)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.OnSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (balance) {
                        StageBalance.OPTIMAL -> WhoopColors.RecoveryHigh.copy(alpha = 0.15f)
                        StageBalance.LOW -> WhoopColors.Warning.copy(alpha = 0.15f)
                        StageBalance.HIGH -> WhoopColors.OnSurfaceTertiary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when (balance) {
                            StageBalance.OPTIMAL -> "Óptimo"
                            StageBalance.LOW -> "Bajo"
                            StageBalance.HIGH -> "Elevado"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        color = when (balance) {
                            StageBalance.OPTIMAL -> WhoopColors.RecoveryHigh
                            StageBalance.LOW -> WhoopColors.Warning
                            StageBalance.HIGH -> WhoopColors.OnSurfaceSecondary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = benefit,
                style = MaterialTheme.typography.labelSmall,
                color = WhoopColors.OnSurfaceTertiary,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                text = "Rango: $targetRange",
                style = MaterialTheme.typography.labelSmall,
                color = WhoopColors.OnSurfaceTertiary
            )
        }
    }
}

@Composable
private fun Sleep7DaysChart(
    sessions: List<SleepSessionEntity>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) return

    val maxHours = 10f
    val targetHours = 8.0f
    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale("es", "ES"))

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bottomMargin = 22.dp.toPx()
        val chartH = h - bottomMargin
        val count = sessions.size
        val barWidth = (w / count.coerceAtLeast(1)) * 0.45f

        // Línea punteada de objetivo 8h
        val targetY = chartH - (targetHours / maxHours) * chartH
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, targetY),
            end = Offset(w, targetY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        sessions.forEachIndexed { i, session ->
            val hours = (session.totalSleepMinutes / 60.0).toFloat().coerceIn(0f, maxHours)
            val barH = (hours / maxHours) * chartH
            val centerX = (w / count) * i + (w / count) / 2
            val left = centerX - barWidth / 2
            val top = chartH - barH

            val barColor = when {
                hours >= 7.5f -> WhoopColors.RecoveryHigh
                hours >= 6.0f -> WhoopColors.Sleep
                else -> WhoopColors.Warning
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        sessions.forEach { s ->
            Text(
                text = s.date.format(dayFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = WhoopColors.OnSurfaceTertiary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SleepDetailScreenPreview() {
    SaludPersonalTheme {
        SleepDetailScreen(todaySleep = null)
    }
}
