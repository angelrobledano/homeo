package com.salud.personal.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.calendar.DailySummary
import com.salud.personal.ui.dashboard.DashboardFormatting
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla de Calendario y Resumen Histórico Diario.
 * Permite navegar mes a mes, ver indicadores de actividad (entrenos, pasos, sueño, peso)
 * en cada casilla y consultar el desglose completo del día seleccionado.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectToday: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    val dayDetailFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
    val summary = state.selectedDaySummary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barra Superior: Volver e ir a Hoy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .tactilePress(scaleDown = 0.88f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = WhoopColors.OnSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            OutlinedButton(
                onClick = onSelectToday,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WhoopColors.Strain
                ),
                modifier = Modifier.tactilePress(scaleDown = 0.94f)
            ) {
                Text("Hoy", fontWeight = FontWeight.Bold, color = WhoopColors.Strain)
            }
        }

        // Tarjeta Calendario Mensual con EasyUI Spotlight
        EasySpotlightCard(
            spotlightAccent = WhoopColors.Strain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Mes y Flechas de Navegación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .tactilePress(scaleDown = 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mes anterior",
                            tint = WhoopColors.OnSurface
                        )
                    }

                    Text(
                        text = state.selectedMonth.format(monthFormatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface
                    )

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .tactilePress(scaleDown = 0.90f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mes siguiente",
                            tint = WhoopColors.OnSurface
                        )
                    }
                }

                // Días de la semana (LUN .. DOM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurfaceTertiary,
                            modifier = Modifier.width(36.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Cuadrícula del Mes
                MonthCalendarGrid(
                    yearMonth = state.selectedMonth,
                    selectedDate = state.selectedDate,
                    dailySummaries = state.dailySummariesMap,
                    onSelectDate = onSelectDate
                )

                // Leyenda de puntos indicadores
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = WhoopColors.RecoveryHigh, label = "Entreno")
                    LegendItem(color = WhoopColors.Strain, label = "Pasos")
                    LegendItem(color = WhoopColors.Sleep, label = "Sueño")
                    LegendItem(color = WhoopColors.Weight, label = "Peso")
                }
            }
        }

        // Resumen Detallado del Día Seleccionado
        if (summary != null) {
            val dateLabel = summary.date.format(dayDetailFormatter).replaceFirstChar { it.uppercase() }
            val isToday = summary.date == LocalDate.now()

            EasySpotlightCard(
                spotlightAccent = if (summary.hasData) WhoopColors.RecoveryHigh else WhoopColors.OnSurfaceTertiary,
                modifier = Modifier.fillMaxWidth()
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
                            text = if (isToday) "RESUMEN DE HOY" else "RESUMEN DEL DÍA",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.OnSurfaceSecondary
                        )
                        if (isToday) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = WhoopColors.Strain.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Hoy",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WhoopColors.Strain,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface
                    )

                    HorizontalDivider(color = WhoopColors.Outline)

                    // 1. Actividad y Pasos
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WhoopColors.Strain))
                                Text(
                                    text = "Actividad y Pasos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhoopColors.OnSurface
                                )
                            }
                            Text(
                                text = "${DashboardFormatting.formatSteps(summary.steps)} pasos",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Strain
                            )
                        }

                        val distanceKm = String.format(Locale.US, "%.2f", summary.distanceMeters / 1000.0)
                        Text(
                            text = "$distanceKm km recorridos · ${summary.caloriesBurned} kcal activas",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }

                    HorizontalDivider(color = WhoopColors.Outline)

                    // 2. Entrenamientos
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WhoopColors.RecoveryHigh))
                            Text(
                                text = "Entrenamientos (${summary.workouts.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WhoopColors.OnSurface
                            )
                        }

                        if (summary.workouts.isNotEmpty()) {
                            summary.workouts.forEach { session ->
                                DayWorkoutItem(session = session)
                            }
                            Text(
                                text = "Esfuerzo total: ${summary.totalWorkoutTrimp.toInt()} pts · ${summary.totalWorkoutMinutes.toInt()} min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WhoopColors.RecoveryHigh
                            )
                        } else {
                            Text(
                                text = "Sin sesiones de entrenamiento registradas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }

                    HorizontalDivider(color = WhoopColors.Outline)

                    // 3. Sueño
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WhoopColors.Sleep))
                            Text(
                                text = "Sueño y Descanso",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WhoopColors.OnSurface
                            )
                        }

                        val sleep = summary.sleepSession
                        if (sleep != null) {
                            val h = sleep.totalSleepMinutes / 60
                            val m = sleep.totalSleepMinutes % 60
                            val eff = sleep.sleepEfficiencyScore ?: 85
                            Text(
                                text = "${h}h ${m}m dormido · $eff% eficiencia",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Sleep
                            )
                            Text(
                                text = "Profundo: ${sleep.deepSleepMinutes}m · REM: ${sleep.remSleepMinutes}m · Ligero: ${sleep.lightSleepMinutes}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        } else {
                            Text(
                                text = "Sin registro nocturno de sueño.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceTertiary
                            )
                        }
                    }

                    // 4. Recuperación y Peso (si existen)
                    if (summary.checkIn != null || summary.weightKg != null) {
                        HorizontalDivider(color = WhoopColors.Outline)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (summary.checkIn != null) {
                                Column {
                                    Text("Recuperación", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                                    Text(
                                        text = "${summary.checkIn.calculatedRecoveryScore}%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = WhoopColors.RecoveryHigh
                                    )
                                }
                            }

                            if (summary.weightKg != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Peso Corporal", style = MaterialTheme.typography.labelSmall, color = WhoopColors.OnSurfaceTertiary)
                                    val fatText = summary.bodyFatPct?.let { " · ${String.format(Locale.US, "%.1f", it)}% grasa" } ?: ""
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", summary.weightKg)} kg$fatText",
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = WhoopColors.Weight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    dailySummaries: Map<LocalDate, DailySummary>,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()

    // En DayOfWeek, MONDAY = 1 .. SUNDAY = 7
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val leadingEmptyDays = startDayOfWeek - 1

    val totalCells = leadingEmptyDays + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = r * 7 + col
                    val dayOfMonth = cellIndex - leadingEmptyDays + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val summary = dailySummaries[date]
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()

                        CalendarDayCell(
                            date = date,
                            dayNumber = dayOfMonth,
                            summary = summary,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onSelectDate(date) }
                        )
                    } else {
                        // Celda vacía fuera de mes
                        Box(modifier = Modifier.size(42.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    dayNumber: Int,
    summary: DailySummary?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> WhoopColors.SurfaceElevated
                    else -> Color.Transparent
                }
            )
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, WhoopColors.Strain, RoundedCornerShape(8.dp))
                } else if (isToday) {
                    Modifier.border(1.dp, WhoopColors.OnSurfaceTertiary, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .tactilePress(scaleDown = 0.90f)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> WhoopColors.OnSurface
                    isToday -> WhoopColors.Strain
                    else -> WhoopColors.OnSurfaceSecondary
                }
            )

            // Puntos de datos disponibles
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (summary?.workouts?.isNotEmpty() == true) {
                    Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(WhoopColors.RecoveryHigh))
                }
                if ((summary?.steps ?: 0) > 0) {
                    Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(WhoopColors.Strain))
                }
                if (summary?.sleepSession != null) {
                    Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(WhoopColors.Sleep))
                }
                if (summary?.weightKg != null) {
                    Box(modifier = Modifier.size(3.5.dp).clip(CircleShape).background(WhoopColors.Weight))
                }
            }
        }
    }
}

@Composable
private fun DayWorkoutItem(session: WorkoutSessionEntity) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = WhoopColors.SurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.OnSurface
                )
                Text(
                    text = "${session.durationMinutes.toInt()} min ${session.avgHeartRate?.let { "· $it ppm" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            if (session.trimp != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = WhoopColors.Strain.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${session.trimp.toInt()} pts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Strain,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WhoopColors.OnSurfaceTertiary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    SaludPersonalTheme {
        CalendarScreen(
            state = CalendarUiState(),
            onSelectDate = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectToday = {}
        )
    }
}
