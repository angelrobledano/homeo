package com.salud.personal.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.domain.summary.PeriodSummary
import com.salud.personal.domain.summary.PeriodType
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.WhoopColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PeriodSummaryScreen(
    state: PeriodSummaryUiState,
    onSelectPeriodType: (PeriodType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetToToday: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = state.summary
    val spanishLocale = Locale("es", "ES")
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanishLocale)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
    ) {
        // Cabecera superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = WhoopColors.OnSurface
                    )
                }
                Text(
                    text = "RESUMEN DE RENDIMIENTO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.OnSurface
                )
            }

            IconButton(onClick = onResetToToday) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Ir al periodo actual",
                    tint = WhoopColors.Strain
                )
            }
        }

        // Selector de tipo de periodo: Día | Semana | Mes | Trimestre | Año
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PeriodType.entries.forEach { type ->
                val selected = state.periodType == type
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            color = if (selected) WhoopColors.Strain else WhoopColors.Surface,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { onSelectPeriodType(type) }
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) WhoopColors.Background else WhoopColors.OnSurfaceSecondary
                    )
                }
            }
        }

        // Barra de navegación temporal (Anterior - Título - Siguiente)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .tactilePress()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Periodo anterior",
                    tint = WhoopColors.OnSurfaceSecondary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = summary?.title ?: "Cargando...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.OnSurface
                )
                Text(
                    text = summary?.subtitle ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            IconButton(
                onClick = onNext,
                enabled = state.canGoForward,
                modifier = Modifier
                    .size(48.dp)
                    .tactilePress()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Periodo siguiente",
                    tint = if (state.canGoForward) WhoopColors.OnSurface else WhoopColors.OnSurfaceTertiary.copy(alpha = 0.3f)
                )
            }
        }

        HorizontalDivider(color = WhoopColors.Outline.copy(alpha = 0.2f))

        // Contenido con scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (summary == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Calculando balance...", color = WhoopColors.OnSurfaceTertiary)
                }
            } else {
                // Tarjeta Principal: Carga y Volumen
                EasySpotlightCard(
                    spotlightAccent = WhoopColors.Strain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "VOLUMEN DE ENTRENAMIENTO",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurfaceSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${summary.totalTrimp.toInt()}",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = (-1).sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = WhoopColors.Strain
                                    )
                                    Text(
                                        text = "pts TRIMP",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = WhoopColors.OnSurfaceSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                val hours = (summary.totalDurationMinutes / 60.0).toInt()
                                val mins = (summary.totalDurationMinutes % 60.0).toInt()
                                Text(
                                    text = "${summary.totalWorkouts} sesiones · ${hours}h ${mins}m en movimiento",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhoopColors.OnSurfaceSecondary
                                )
                            }
                        }
                    }
                }

                // Grid 2x2: Métricas Biométricas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Recuperación
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WhoopColors.Surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "RECUPERACIÓN",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val rec = summary.avgRecoveryScore
                            val recColor = when {
                                rec == null -> WhoopColors.OnSurfaceTertiary
                                rec >= 67 -> WhoopColors.RecoveryHigh
                                rec >= 34 -> WhoopColors.RecoveryMedium
                                else -> WhoopColors.RecoveryLow
                            }
                            Text(
                                text = if (rec != null) "$rec%" else "--",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = recColor
                            )
                            Text(
                                text = "Media del periodo",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }

                    // Sueño
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WhoopColors.Surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SUEÑO DIARIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val sleep = summary.avgSleepHours
                            Text(
                                text = if (sleep != null) "${sleep}h" else "--",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Sleep
                            )
                            Text(
                                text = "Promedio por noche",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pasos
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WhoopColors.Surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "PASOS TOTALES",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${summary.totalSteps}",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.OnSurface
                            )
                            Text(
                                text = "~${summary.avgStepsPerDay} pasos/día",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }

                    // Peso
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WhoopColors.Surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "CAMBIO DE PESO",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val delta = summary.weightDeltaKg
                            val deltaColor = when {
                                delta == null -> WhoopColors.OnSurfaceTertiary
                                delta > 0 -> WhoopColors.Warning
                                delta < 0 -> WhoopColors.RecoveryHigh
                                else -> WhoopColors.OnSurface
                            }
                            val deltaStr = when {
                                delta == null -> "--"
                                delta > 0 -> "+${delta} kg"
                                else -> "${delta} kg"
                            }
                            Text(
                                text = deltaStr,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                            Text(
                                text = "Neto en el periodo",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }
                }

                // Días Pico del Periodo
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WhoopColors.Surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "HITOS DESTACADOS DEL PERIODO",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurfaceSecondary
                        )

                        // Pico de Esfuerzo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Mayor Carga", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                                val dateStr = summary.peakStrainDay?.format(dayFormatter)?.replaceFirstChar { it.uppercase() } ?: "Sin sesiones"
                                Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = WhoopColors.OnSurface)
                            }
                            if (summary.peakStrainValue != null) {
                                Text(
                                    text = "${summary.peakStrainValue.toInt()} pts",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = WhoopColors.Strain
                                )
                            }
                        }

                        HorizontalDivider(color = WhoopColors.Outline.copy(alpha = 0.2f))

                        // Pico de Recuperación
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Mayor Recuperación", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                                val dateStr = summary.peakRecoveryDay?.format(dayFormatter)?.replaceFirstChar { it.uppercase() } ?: "Sin check-in"
                                Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = WhoopColors.OnSurface)
                            }
                            if (summary.peakRecoveryValue != null) {
                                Text(
                                    text = "${summary.peakRecoveryValue}%",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = WhoopColors.RecoveryHigh
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
