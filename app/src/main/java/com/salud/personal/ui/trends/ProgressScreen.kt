package com.salud.personal.ui.trends

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.domain.trends.TrendsCalculator
import com.salud.personal.domain.habits.HabitInsight
import com.salud.personal.domain.habits.HabitImpact
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors

/**
 * Pantalla principal de la pestaña "Progreso" (Tendencias a medio y largo plazo).
 * Diseñada para ser intuitiva y transparente: transforma conceptos abstractos (ACWR y TRIMP)
 * en un medidor visual de zonas de esfuerzo, relación entre volumen actual y capacidad habitual,
 * histograma de 4 semanas, comparador semana a semana, historial de FC en reposo con línea base
 * y evolución de peso con Trend Weight (EMA) aislando fluctuaciones de agua.
 */
@Composable
fun ProgressScreen(
    selectedPeriod: ProgressPeriod = ProgressPeriod.LAST_7_DAYS,
    onSelectPeriod: (ProgressPeriod) -> Unit = {},
    acuteLoadTrimp7d: Double = 320.0,
    chronicLoadTrimp28d: Double = 1180.0,
    weeklyLoadsTrimp: List<Double> = listOf(280.0, 310.0, 270.0, 320.0),
    recentWeights: List<Double> = listOf(75.2, 75.0, 74.8, 74.9, 74.6),
    movingAverages: List<Double> = emptyList(),
    weightTrend: TrendsCalculator.WeightTrendResult = TrendsCalculator.WeightTrendResult(0.0, 0.0, 0.0, null, emptyList()),
    rhrTrend: TrendsCalculator.RhrTrendResult = TrendsCalculator.RhrTrendResult(null, 0.0, 0.0, TrendsCalculator.RhrStatus.NORMAL, emptyList()),
    weeklyComparison: TrendsCalculator.WeeklyComparison = TrendsCalculator.WeeklyComparison(0.0, 0.0, 0, 0, 0, 0, 0, 0, 0, null, null, null),
    habitInsights: List<HabitInsight> = emptyList(),
    onOpenPeriodSummary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val acwr = TrendsCalculator.calculateAcwr(acuteLoadTrimp7d, chronicLoadTrimp28d)

    val (acwrColor, acwrLabel, acwrDescription) = when (acwr.riskCategory) {
        TrendsCalculator.AcwrRiskCategory.INSUFFICIENT_DATA -> Triple(
            WhoopColors.OnSurfaceSecondary,
            "Sin Entrenos Suficientes",
            "Registra entrenamientos para calcular la relación entre tu esfuerzo reciente y tu capacidad histórica."
        )
        TrendsCalculator.AcwrRiskCategory.OPTIMAL_SWEETSPOT -> Triple(
            WhoopColors.RecoveryHigh,
            "Zona Óptima de Mejora",
            "Tu esfuerzo semanal supera ligeramente tu promedio mensual. Es el estímulo ideal para progresar sin riesgo de sobreentrenamiento."
        )
        TrendsCalculator.AcwrRiskCategory.UNDER_TRAINING -> Triple(
            WhoopColors.Warning,
            "Descarga / Volumen Bajo",
            "Tu esfuerzo esta semana está por debajo de tu media habitual. Ideal para recuperación activa o descarga planificada."
        )
        TrendsCalculator.AcwrRiskCategory.HIGH_RISK -> Triple(
            WhoopColors.Warning,
            "Carga Alta de Esfuerzo",
            "Has incrementado notablemente el volumen esta semana. Vigila tu descanso y nutrición para evitar fatiga residual."
        )
        TrendsCalculator.AcwrRiskCategory.VERY_HIGH_RISK -> Triple(
            WhoopColors.Error,
            "Alerta de Sobrecarga",
            "Aumento brusco de volumen (+50% sobre tu media mensual). Riesgo elevado de lesión o fatiga acumulada."
        )
    }

    val weeklyCapacityAvg = (acwr.chronicLoad28d / 4).coerceAtLeast(1.0)
    val deltaPct = (((acwr.acuteLoad7d - weeklyCapacityAvg) / weeklyCapacityAvg) * 100).toInt()
    val deltaSign = if (deltaPct >= 0) "+$deltaPct%" else "$deltaPct%"

    val validWeights = recentWeights.filter { it in 35.0..250.0 }
    val computedMa = if (movingAverages.isNotEmpty()) movingAverages else TrendsCalculator.calculateMovingAverage(validWeights, 3)

    var showAcwrExplanation by remember { mutableStateOf(false) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera de la sección Progreso con acceso directo a Balances Cerrados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TENDENCIAS DE RENDIMIENTO",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
                fontWeight = FontWeight.Bold,
                color = WhoopColors.OnSurfaceSecondary
            )

            if (onOpenPeriodSummary != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WhoopColors.Strain.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, WhoopColors.Strain.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .tactilePress(scaleDown = 0.94f)
                        .clickable { onOpenPeriodSummary() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Balances D/S/M/A",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                    }
                }
            }
        }

        // Selector de Granularidad Temporal con soporte de Gestos Swipe
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(selectedPeriod) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragOffset < -40f) {
                                val next = (selectedPeriod.ordinal + 1).coerceAtMost(ProgressPeriod.entries.size - 1)
                                onSelectPeriod(ProgressPeriod.entries[next])
                            } else if (horizontalDragOffset > 40f) {
                                val prev = (selectedPeriod.ordinal - 1).coerceAtLeast(0)
                                onSelectPeriod(ProgressPeriod.entries[prev])
                            }
                            horizontalDragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalDragOffset += dragAmount
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressPeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) WhoopColors.Strain else WhoopColors.Surface,
                    modifier = Modifier
                        .weight(1f)
                        .tactilePress(scaleDown = 0.94f)
                        .clickable { onSelectPeriod(period) }
                ) {
                    val periodSub = if (period.subtitleRes != 0) androidx.compose.ui.res.stringResource(period.subtitleRes) else period.subtitle
                    Text(
                        text = "${period.label} · $periodSub",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) WhoopColors.Background else WhoopColors.OnSurfaceSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // 1. Comparador temporal (7d, 30d, 90d según periodo seleccionado)
        WeeklyComparisonCard(comparison = weeklyComparison, period = selectedPeriod)

        // 2. Tarjeta de Equilibrio de Entrenamiento (ACWR Intuitivo)
        EasySpotlightCard(
            spotlightAccent = acwrColor,
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
                        text = "EQUILIBRIO DE ENTRENAMIENTO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WhoopColors.SurfaceElevated,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                            .tactilePress(scaleDown = 0.92f)
                            .clickable { showAcwrExplanation = !showAcwrExplanation }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (showAcwrExplanation) "Ocultar" else "¿Qué es ACWR?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WhoopColors.Strain,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
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
                            text = String.format(java.util.Locale.US, "%.2f", acwr.ratio),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = acwrColor
                        )
                        Text(
                            text = "ratio",
                            style = MaterialTheme.typography.titleMedium,
                            color = WhoopColors.OnSurfaceSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = acwrColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = acwrLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = acwrColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Medidor Visual de Rango de Zonas
                TrainingLoadZoneGauge(
                    currentRatio = acwr.ratio.toFloat(),
                    accentColor = acwrColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                )

                Text(
                    text = acwrDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceSecondary
                )

                AnimatedVisibility(
                    visible = showAcwrExplanation,
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
                                text = "RATIO DE CARGA AGUDA VS. CRÓNICA (GABBETT)",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Strain
                            )
                            Text(
                                text = "El ACWR evalúa si el estímulo de entrenamiento reciente supera de forma segura tu tolerancia fisiológica construida durante el último mes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurface
                            )
                            Text(
                                text = "• < 0.8 (Descarga): Carga inferior a la habitual. Ideal para recuperación activa.\n" +
                                        "• 0.8 – 1.3 (Sweet Spot Óptimo): Rango ideal para ganar condición física con bajo riesgo de lesión.\n" +
                                        "• 1.3 – 1.5 (Precaución): Sobrecarga rápida. Conviene moderar intensidad.\n" +
                                        "• > 1.5 (Zona de Peligro): Pico agudo brusco (+50%). Multiplica el riesgo de fatiga excesiva o lesión.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = WhoopColors.Outline)

                // Comparación directa entre esfuerzo actual y capacidad construida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Esta semana (7 días)",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = "${acwr.acuteLoad7d.toInt()} pts",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                        Text(
                            text = "Esfuerzo reciente",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Diferencia",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = deltaSign,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = acwrColor
                        )
                        Text(
                            text = "vs tu promedio",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Capacidad media",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = "${weeklyCapacityAvg.toInt()} pts / sem",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        Text(
                            text = "Media en 4 semanas",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }
                }
            }
        }

        // 3. Gráfico de barras de carga semanal (4 semanas)
        if (weeklyLoadsTrimp.isNotEmpty()) {
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
                            text = "HISTORIAL DE ESFUERZO (4 SEMANAS)",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.OnSurfaceSecondary
                        )
                        Text(
                            text = "Puntos de carga",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                    }

                    WeeklyWorkloadBarChart(
                        weeklyLoads = weeklyLoadsTrimp,
                        weeklyAverage = weeklyCapacityAvg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    Text(
                        text = "Línea discontinua: Promedio mensual establecido (${weeklyCapacityAvg.toInt()} pts/semana).",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                }
            }
        }

        // 4. Historial de FC en reposo según periodo con línea base
        RhrTrendCard(rhrTrend = rhrTrend, period = selectedPeriod)

        // 5. Trend Weight (Línea de tendencia de peso sin fluctuaciones de agua)
        TrendWeightCard(
            weightTrend = weightTrend,
            recentWeights = validWeights,
            movingAverages = computedMa,
            period = selectedPeriod
        )

        // 6. Impacto de Hábitos en tu Recuperación (WHOOP Journal Insights)
        HabitInsightsSection(insights = habitInsights)
    }
}

/**
 * Medidor visual de zonas de esfuerzo (ACWR):
 * Barra continua segmentada con indicador puntero que sitúa el valor del atleta
 * respecto a las 4 zonas fisiológicas (<0.8 Descarga, 0.8-1.3 Óptimo, 1.3-1.5 Alto, >1.5 Sobrecarga).
 */
@Composable
private fun TrainingLoadZoneGauge(
    currentRatio: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animRatio by animateFloatAsState(
        targetValue = currentRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "acwrGaugeAnim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val barH = 10.dp.toPx()
        val topY = 4.dp.toPx()

        // Rango del medidor: 0.4 a 1.9 (ancho 1.5)
        val minR = 0.4f
        val maxR = 1.9f
        val range = maxR - minR

        fun ratioToX(r: Float): Float {
            val clamped = r.coerceIn(minR, maxR)
            return ((clamped - minR) / range) * w
        }

        val x08 = ratioToX(0.8f)
        val x13 = ratioToX(1.3f)
        val x15 = ratioToX(1.5f)

        // 1. Zona Descarga: 0.4 .. 0.8
        drawRoundRect(
            color = WhoopColors.OnSurfaceTertiary,
            topLeft = Offset(0f, topY),
            size = Size(x08, barH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // 2. Zona Óptima: 0.8 .. 1.3
        drawRect(
            color = WhoopColors.RecoveryHigh,
            topLeft = Offset(x08 + 2f, topY),
            size = Size((x13 - x08 - 4f).coerceAtLeast(1f), barH)
        )

        // 3. Zona Carga Alta: 1.3 .. 1.5
        drawRect(
            color = WhoopColors.Warning,
            topLeft = Offset(x13 + 2f, topY),
            size = Size((x15 - x13 - 4f).coerceAtLeast(1f), barH)
        )

        // 4. Zona Sobrecarga: 1.5 .. 1.9
        drawRoundRect(
            color = WhoopColors.Error,
            topLeft = Offset(x15 + 2f, topY),
            size = Size((w - x15 - 2f).coerceAtLeast(1f), barH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Puntero de posición actual animado con resorte
        val pointerX = ratioToX(animRatio)
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("< 0.8 Descarga", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = WhoopColors.OnSurfaceTertiary)
        Text("0.8 - 1.3 Óptimo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = WhoopColors.RecoveryHigh)
        Text("1.3 - 1.5 Alto", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = WhoopColors.Warning)
        Text("> 1.5 Exceso", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = WhoopColors.Error)
    }
}

/**
 * Gráfico de barras de carga semanal (4 semanas) con línea de promedio de capacidad mensual.
 */
@Composable
private fun WeeklyWorkloadBarChart(
    weeklyLoads: List<Double>,
    weeklyAverage: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val maxLoad = (weeklyLoads.maxOrNull() ?: 100.0).coerceAtLeast(weeklyAverage * 1.2).coerceAtLeast(100.0)
    val weekLabels = listOf("Sem -3", "Sem -2", "Sem -1", "Esta sem")

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "weeklyWorkloadAnim"
    )

    val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }

    Box(modifier = modifier) {
        // Línea punteada de media mensual con padding vertical para evitar colisión con etiquetas de texto
        if (weeklyAverage > 0.0) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 28.dp, top = 20.dp)) {
                val chartH = size.height
                val avgY = chartH - ((weeklyAverage / maxLoad).toFloat() * chartH)
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = Offset(0f, avgY),
                    end = Offset(size.width, avgY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dashPathEffect
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyLoads.forEachIndexed { index, load ->
                val fraction = (load / maxLoad).toFloat().coerceIn(0.06f, 1.0f)
                val isCurrentWeek = index == weeklyLoads.lastIndex
                val barColor = if (isCurrentWeek) WhoopColors.Strain else WhoopColors.OnSurfaceSecondary.copy(alpha = 0.5f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = "${load.toInt()} pts",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        fontWeight = if (isCurrentWeek) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentWeek) WhoopColors.Strain else WhoopColors.OnSurfaceTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .fillMaxHeight((fraction * 0.62f * animProgress).coerceAtLeast(0.02f))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(barColor, barColor.copy(alpha = 0.4f))
                                ),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = weekLabels.getOrElse(index) { "S$index" },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = if (isCurrentWeek) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrentWeek) WhoopColors.OnSurface else WhoopColors.OnSurfaceTertiary
                    )
                }
            }
        }
    }
}

/**
 * Comparador "Semana actual vs. Semana anterior" (WHOOP Weekly Performance Review).
 */
@Composable
private fun WeeklyComparisonCard(
    comparison: TrendsCalculator.WeeklyComparison,
    period: ProgressPeriod = ProgressPeriod.LAST_7_DAYS,
    modifier: Modifier = Modifier
) {
    val (titleText, subtitleText) = when (period) {
        ProgressPeriod.LAST_7_DAYS -> "ESTA SEMANA VS. ANTERIOR" to "7d vs 7d previos"
        ProgressPeriod.LAST_30_DAYS -> "ESTE MES VS. ANTERIOR" to "30d vs 30d previos"
        ProgressPeriod.LAST_90_DAYS -> "ESTE TRIMESTRE VS. ANTERIOR" to "90d vs 90d previos"
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
                    text = titleText,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // 2x2 grid de baldosas de telemetría comparativa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val trimpSign = if (comparison.trimpDeltaPct >= 0) "+${comparison.trimpDeltaPct}%" else "${comparison.trimpDeltaPct}%"
                val trimpColor = if (comparison.trimpDeltaPct >= 0) WhoopColors.Strain else WhoopColors.OnSurfaceSecondary
                ComparisonTile(
                    title = "CARGA TOTAL",
                    currentValue = "${comparison.currentWeekTrimp.toInt()} pts",
                    previousValue = "vs ${comparison.previousWeekTrimp.toInt()} pts",
                    deltaBadge = trimpSign,
                    badgeColor = trimpColor,
                    modifier = Modifier.weight(1f)
                )

                val stepsSign = if (comparison.stepsDeltaPct >= 0) "+${comparison.stepsDeltaPct}%" else "${comparison.stepsDeltaPct}%"
                val stepsColor = if (comparison.stepsDeltaPct >= 0) WhoopColors.RecoveryHigh else WhoopColors.Warning
                ComparisonTile(
                    title = "PASOS DIARIOS",
                    currentValue = "${comparison.currentAvgSteps}",
                    previousValue = "vs ${comparison.previousAvgSteps}",
                    deltaBadge = stepsSign,
                    badgeColor = stepsColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val curSleepH = comparison.currentAvgSleepMinutes / 60
                val curSleepM = comparison.currentAvgSleepMinutes % 60
                val prevSleepH = comparison.previousAvgSleepMinutes / 60
                val prevSleepM = comparison.previousAvgSleepMinutes % 60
                val sleepSign = if (comparison.sleepDeltaMinutes >= 0) "+${comparison.sleepDeltaMinutes}m" else "${comparison.sleepDeltaMinutes}m"
                val sleepColor = if (comparison.sleepDeltaMinutes >= 0) WhoopColors.Sleep else WhoopColors.Warning
                ComparisonTile(
                    title = "SUEÑO PROMEDIO",
                    currentValue = "${curSleepH}h ${curSleepM}m",
                    previousValue = "vs ${prevSleepH}h ${prevSleepM}m",
                    deltaBadge = sleepSign,
                    badgeColor = sleepColor,
                    modifier = Modifier.weight(1f)
                )

                val rhrDeltaStr = comparison.rhrDeltaBpm?.let {
                    if (it > 0) "+$it lpm" else "$it lpm"
                } ?: "--"
                val rhrColor = comparison.rhrDeltaBpm?.let {
                    if (it <= 0.0) WhoopColors.RecoveryHigh else WhoopColors.Warning
                } ?: WhoopColors.OnSurfaceTertiary
                ComparisonTile(
                    title = "FC REPOSO PROM.",
                    currentValue = comparison.currentAvgRhr?.let { "$it lpm" } ?: "--",
                    previousValue = comparison.previousAvgRhr?.let { "vs $it lpm" } ?: "vs --",
                    deltaBadge = rhrDeltaStr,
                    badgeColor = rhrColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ComparisonTile(
    title: String,
    currentValue: String,
    previousValue: String,
    deltaBadge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhoopColors.SurfaceElevated,
        border = BorderStroke(1.dp, WhoopColors.Outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = WhoopColors.OnSurfaceTertiary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = deltaBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = currentValue,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = WhoopColors.OnSurface
            )
            Text(
                text = previousValue,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = WhoopColors.OnSurfaceTertiary
            )
        }
    }
}

/**
 * Historial de FC en reposo a 30 días con línea base y evaluación de fatiga.
 */
@Composable
private fun RhrTrendCard(
    rhrTrend: TrendsCalculator.RhrTrendResult,
    period: ProgressPeriod = ProgressPeriod.LAST_7_DAYS,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusLabel) = when (rhrTrend.status) {
        TrendsCalculator.RhrStatus.EXCELLENT -> Pair(WhoopColors.RecoveryHigh, "Excelente")
        TrendsCalculator.RhrStatus.NORMAL -> Pair(WhoopColors.OnSurfaceSecondary, "Estable")
        TrendsCalculator.RhrStatus.ELEVATED -> Pair(WhoopColors.Warning, "Fatiga / Elevado")
        TrendsCalculator.RhrStatus.HIGH_ALERT -> Pair(WhoopColors.Error, "Alerta Fatiga")
    }

    EasySpotlightCard(
        spotlightAccent = statusColor,
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
                    text = "FC EN REPOSO Y LÍNEA BASE",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Text(
                    text = "Historial ${period.label.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            if (rhrTrend.latestRhr != null) {
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
                            text = "${rhrTrend.latestRhr}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = (-1).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        Text(
                            text = "lpm",
                            style = MaterialTheme.typography.titleMedium,
                            color = WhoopColors.OnSurfaceSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val deltaSign = if (rhrTrend.deltaFromBaseline > 0) "+${rhrTrend.deltaFromBaseline}" else "${rhrTrend.deltaFromBaseline}"
                        Text(
                            text = "$deltaSign vs base (${rhrTrend.baselineRhr} lpm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                RhrTrendChart(
                    points = rhrTrend.points30d,
                    baselineRhr = rhrTrend.baselineRhr,
                    statusColor = statusColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Medición diaria",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceSecondary
                    )
                    Text(
                        text = "--- Línea base (${rhrTrend.baselineRhr} lpm)",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                }
            } else {
                Text(
                    text = "Registra mediciones de pulso o check-ins matutinos para trazar tu línea base fisiológica.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }
    }
}

/**
 * Gráfico continuo de 30 días de FC en reposo con línea base horizontal.
 */
@Composable
private fun RhrTrendChart(
    points: List<TrendsCalculator.DailyRhrPoint>,
    baselineRhr: Double,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "rhrTrendAnim"
    )

    val minRhr = ((points.minOfOrNull { it.rhr }?.toDouble() ?: 50.0).coerceAtMost(baselineRhr) - 3).coerceAtLeast(35.0)
    val maxRhr = ((points.maxOfOrNull { it.rhr }?.toDouble() ?: 70.0).coerceAtLeast(baselineRhr) + 3).coerceAtMost(120.0)
    val range = (maxRhr - minRhr).coerceAtLeast(6.0)
    val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val rhrPath = remember { Path() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingEnd = 16f
        val paddingVertical = 16f
        val usableHeight = height - (paddingVertical * 2)

        fun getX(index: Int): Float {
            if (points.size <= 1) return (width - paddingEnd) / 2f
            return (index.toFloat() / (points.size - 1)) * (width - paddingEnd)
        }

        fun getY(value: Double): Float {
            val normalized = ((value - minRhr) / range).toFloat().coerceIn(0f, 1f)
            val rawY = (height - paddingVertical) - (normalized * usableHeight)
            val baseY = height - paddingVertical
            return baseY + (rawY - baseY) * animProgress
        }

        // Línea base discontinua horizontal
        val baseY = getY(baselineRhr)
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(0f, baseY),
            end = Offset(width - paddingEnd, baseY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dashPathEffect
        )

        // Trazado de puntos de FC
        rhrPath.reset()
        points.forEachIndexed { index, pt ->
            val x = getX(index)
            val y = getY(pt.rhr.toDouble())
            if (index == 0) rhrPath.moveTo(x, y) else rhrPath.lineTo(x, y)
        }

        drawPath(
            path = rhrPath,
            color = statusColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Puntos individuales
        points.forEachIndexed { index, pt ->
            val x = getX(index)
            val y = getY(pt.rhr.toDouble())
            val isLast = index == points.lastIndex
            drawCircle(
                color = WhoopColors.Background,
                radius = if (isLast) 5.dp.toPx() else 3.5.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = if (isLast) statusColor else statusColor.copy(alpha = 0.8f),
                radius = if (isLast) 3.5.dp.toPx() else 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Tarjeta de Trend Weight (EMA) aislando fluctuaciones de agua y glucógeno (estilo MacroFactor).
 */
@Composable
private fun TrendWeightCard(
    weightTrend: TrendsCalculator.WeightTrendResult,
    recentWeights: List<Double>,
    movingAverages: List<Double>,
    period: ProgressPeriod = ProgressPeriod.LAST_7_DAYS,
    modifier: Modifier = Modifier
) {
    EasySpotlightCard(
        spotlightAccent = WhoopColors.Weight,
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
                    text = "TENDENCIA DE PESO (EMA)",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Text(
                    text = "Filtro de agua · ${period.label.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            if (weightTrend.points.isNotEmpty()) {
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
                            text = String.format(java.util.Locale.US, "%.1f", weightTrend.currentTrendKg),
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
                        Spacer(modifier = Modifier.width(6.dp))
                        if (weightTrend.weeklyRateKg != null) {
                            val rateSign = if (weightTrend.weeklyRateKg > 0) "+${weightTrend.weeklyRateKg}" else "${weightTrend.weeklyRateKg}"
                            Text(
                                text = "$rateSign kg/sem",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        } else {
                            Text(
                                text = "Estabilizando",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceTertiary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Water noise badge
                    val noiseSign = if (weightTrend.waterNoiseKg > 0) "+${weightTrend.waterNoiseKg}" else "${weightTrend.waterNoiseKg}"
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = WhoopColors.SurfaceElevated,
                        border = BorderStroke(1.dp, WhoopColors.Outline),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "Pesaje: ${weightTrend.currentRawKg} ($noiseSign kg agua)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WhoopColors.OnSurfaceSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Gráfico continuo de 2 capas: puntos tenues de pesajes reales + curva continua de tendencia
                TrendWeightChart(
                    points = weightTrend.points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "● Puntos: Pesajes reales (ruido)",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = "— Curva: Tendencia real filtrada",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.Weight
                    )
                }
            } else if (recentWeights.isNotEmpty()) {
                Text(
                    text = "Registra pesajes regulares para calcular la curva de tendencia amortiguada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            } else {
                Text(
                    text = "No hay pesajes suficientes registrados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }
    }
}

/**
 * Gráfico continuo de doble capa:
 * 1. Puntos dispersos tenues para pesajes reales con ruido hídrico.
 * 2. Curva sólida luminosa con gradiente vertical para la tendencia fisiológica real.
 */
@Composable
private fun TrendWeightChart(
    points: List<TrendsCalculator.TrendWeightPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = WhoopColors.SurfaceElevated,
            modifier = modifier.fillMaxWidth()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text(
                    text = "Se requieren al menos 2 pesajes para trazar la tendencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }
        return
    }

    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "trendWeightAnim"
    )

    val minW = (points.minOf { minOf(it.rawWeightKg, it.trendWeightKg) }) - 0.5
    val maxW = (points.maxOf { maxOf(it.rawWeightKg, it.trendWeightKg) }) + 0.5
    val range = (maxW - minW).coerceAtLeast(0.5)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Indicador de punto inspeccionado al tocar/deslizar
        scrubbedIndex?.let { idx ->
            val pt = points[idx]
            val noise = pt.rawWeightKg - pt.trendWeightKg
            val noiseSign = if (noise >= 0) "+${String.format(java.util.Locale.US, "%.2f", noise)}" else String.format(java.util.Locale.US, "%.2f", noise)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WhoopColors.SurfaceElevated,
                border = BorderStroke(1.dp, WhoopColors.Weight.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pt.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = "${pt.rawWeightKg} kg real · ${pt.trendWeightKg} kg tendencia ($noiseSign kg agua)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Weight
                    )
                }
            }
        }

        val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
        val trendPath = remember { Path() }
        val fillPath = remember { Path() }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val paddingEnd = 16.dp.toPx()
                            val chartWidth = size.width - paddingEnd
                            val ratio = (offset.x / chartWidth).coerceIn(0f, 1f)
                            val idx = (ratio * (points.size - 1)).toInt().coerceIn(0, points.lastIndex)
                            scrubbedIndex = idx
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val paddingEnd = 16f
            val paddingVertical = 16f
            val usableHeight = height - (paddingVertical * 2)

            fun getX(index: Int): Float {
                return (index.toFloat() / (points.size - 1)) * (width - paddingEnd)
            }

            fun getY(weight: Double): Float {
                val normalized = ((weight - minW) / range).toFloat().coerceIn(0f, 1f)
                val rawY = (height - paddingVertical) - (normalized * usableHeight)
                val baseY = height - paddingVertical
                return baseY + (rawY - baseY) * animProgress
            }

            // Gridlines
            val maxGridY = paddingVertical
            val minGridY = height - paddingVertical

            drawLine(
                color = WhoopColors.Outline.copy(alpha = 0.4f),
                start = Offset(0f, maxGridY),
                end = Offset(width - paddingEnd, maxGridY),
                strokeWidth = 1f,
                pathEffect = gridDashEffect
            )
            drawLine(
                color = WhoopColors.Outline.copy(alpha = 0.4f),
                start = Offset(0f, minGridY),
                end = Offset(width - paddingEnd, minGridY),
                strokeWidth = 1f,
                pathEffect = gridDashEffect
            )

            // 1. Capa de pesajes crudos (puntos tenues dispersos)
            points.forEachIndexed { index, pt ->
                val x = getX(index)
                val y = getY(pt.rawWeightKg)
                drawCircle(
                    color = WhoopColors.OnSurfaceTertiary.copy(alpha = 0.70f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 2. Capa de tendencia real (Curva luminosa con gradiente)
            trendPath.reset()
            fillPath.reset()

            points.forEachIndexed { index, pt ->
                val x = getX(index)
                val y = getY(pt.trendWeightKg)
                if (index == 0) {
                    trendPath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    trendPath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(getX(points.size - 1), height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WhoopColors.Weight.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    startY = maxGridY,
                    endY = height
                )
            )

            drawPath(
                path = trendPath,
                color = WhoopColors.Weight,
                style = Stroke(width = 3.dp.toPx())
            )

            // Línea de inspección vertical si hay punto seleccionado
            scrubbedIndex?.let { idx ->
                val inspectX = getX(idx)
                drawLine(
                    color = WhoopColors.Strain.copy(alpha = 0.6f),
                    start = Offset(inspectX, 0f),
                    end = Offset(inspectX, height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                val inspectY = getY(points[idx].trendWeightKg)
                drawCircle(
                    color = WhoopColors.Background,
                    radius = 7.dp.toPx(),
                    center = Offset(inspectX, inspectY)
                )
                drawCircle(
                    color = WhoopColors.Strain,
                    radius = 5.dp.toPx(),
                    center = Offset(inspectX, inspectY)
                )
            }

            // Puntero final de la tendencia actual
            val lastIndex = points.lastIndex
            val lastX = getX(lastIndex)
            val lastY = getY(points.last().trendWeightKg)
            drawCircle(
                color = WhoopColors.Background,
                radius = 6.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = WhoopColors.Weight,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

@Composable
private fun HabitInsightsSection(
    insights: List<HabitInsight>,
    modifier: Modifier = Modifier
) {
    EasySpotlightCard(
        spotlightAccent = WhoopColors.SleepRem,
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
                    text = "IMPACTO DE HÁBITOS (JOURNAL)",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                if (insights.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WhoopColors.SleepRem.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${insights.size} correlaciones",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.SleepRem,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (insights.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WhoopColors.SurfaceElevated, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sin correlaciones aún",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurface
                    )
                    Text(
                        text = "Registra tus hábitos diarios en el check-in matutino para calcular su impacto en recuperación y sueño.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    insights.forEach { insight ->
                        val impactColor = when (insight.impact) {
                            HabitImpact.POSITIVE -> WhoopColors.RecoveryHigh
                            HabitImpact.NEGATIVE -> WhoopColors.Error
                            HabitImpact.NEUTRAL -> WhoopColors.OnSurfaceSecondary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WhoopColors.SurfaceElevated, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = insight.habit.icon, fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = insight.habit.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = WhoopColors.OnSurface
                                    )
                                    val deltaBadgeText = when {
                                        insight.recoveryDelta != 0 -> "${if (insight.recoveryDelta > 0) "+" else ""}${insight.recoveryDelta}% recup"
                                        insight.rhrDelta != 0.0 -> "${if (insight.rhrDelta > 0) "+" else ""}${insight.rhrDelta} lpm"
                                        else -> "Neutro"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = impactColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = deltaBadgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = impactColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = insight.insightSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhoopColors.OnSurfaceSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressScreenPreview() {
    SaludPersonalTheme {
        ProgressScreen()
    }
}
