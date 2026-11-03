package com.salud.personal.ui.coach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.domain.coach.CoachHeuristicEngine
import com.salud.personal.domain.coach.CoachIntelligence
import com.salud.personal.domain.coach.UserHealthSnapshot
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors

/**
 * Pantalla principal del Coach: Centro de Decisión Diaria para el atleta.
 * Proporciona:
 * 1. Prescripción directa de entrenamiento (objetivo de esfuerzo y RPE recomendado).
 * 2. Planificador de descanso (necesidad biológica, deuda y hora óptima de acostarse).
 * 3. Monitor de sobrecarga y prevención (relación ACWR y fatiga aguda).
 * 4. Respuestas en 1 toque a preguntas críticas del día.
 * 5. Consultor contextual sin conexión.
 */
@Composable
fun CoachScreen(
    snapshot: UserHealthSnapshot,
    modifier: Modifier = Modifier
) {
    val intelligence: CoachIntelligence = remember(snapshot) {
        CoachHeuristicEngine.generateIntelligence(snapshot)
    }

    val recoveryColor = WhoopColors.recoveryColor(snapshot.recoveryScore)
    var selectedSmartQuery by remember { mutableStateOf<String?>("¿Debo entrenar hoy o descansar?") }
    var expandedFaqId by remember { mutableStateOf<String?>("train_today") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera Resumen Biométrico del Atleta
        BiometricStatusStrip(snapshot = snapshot, recoveryColor = recoveryColor)

        // 1. Prescripción de Entrenamiento para Hoy
        WorkoutPrescriptionCard(
            prescription = intelligence.prescription,
            recoveryColor = recoveryColor
        )

        // 2. Planificador de Sueño y Ritmo Circadiano
        SleepPlannerCard(sleepPlan = intelligence.sleepPlan)

        // 3. Monitor de Sobrecarga y Prevención
        LoadGuardCard(loadGuard = intelligence.loadGuard)

        // 4. Correlaciones Causa-Efecto de Hábitos en Biomarcadores
        if (intelligence.habitCorrelationAlerts.isNotEmpty()) {
            HabitCorrelationSection(alerts = intelligence.habitCorrelationAlerts)
        }

        // 5. Consultas Biométricas Rápidas en 1 Toque (Chips Inteligentes)
        AthleteSmartQueryChipsCard(
            snapshot = snapshot,
            selectedQuestion = selectedSmartQuery,
            onSelectQuestion = { q ->
                selectedSmartQuery = if (selectedSmartQuery == q) null else q
            }
        )

        // 6. Transparencia metodológica y rigor no clínico
        Surface(
            color = WhoopColors.Surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WhoopColors.OnSurfaceTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Metodología determinista basada en fisiología del deporte (Carga Banister TRIMP, ratio agudo:crónico ACWR, escala logarítmica de esfuerzo 0-21 y arquitectura de sueño). Esta aplicación proporciona análisis personal de rendimiento; no constituye diagnóstico, prescripción médica ni sustituto del criterio facultativo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun BiometricStatusStrip(
    snapshot: UserHealthSnapshot,
    recoveryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusPill(
            label = "RECUP.",
            value = "${snapshot.recoveryScore}%",
            valueColor = recoveryColor,
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            label = "FC REPOSO",
            value = snapshot.restingHeartRate?.let { "$it lpm" } ?: "--",
            valueColor = WhoopColors.OnSurface,
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            label = "SUEÑO",
            value = String.format(java.util.Locale.US, "%.1fh", snapshot.sleepHours),
            valueColor = WhoopColors.Sleep,
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            label = "CARGA 7D",
            value = "${snapshot.recent7DaysTrimp.toInt()} pts",
            valueColor = WhoopColors.Strain,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = WhoopColors.Surface,
        border = BorderStroke(1.dp, WhoopColors.Outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = WhoopColors.OnSurfaceTertiary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun WorkoutPrescriptionCard(
    prescription: com.salud.personal.domain.coach.WorkoutPrescription,
    recoveryColor: Color
) {
    EasySpotlightCard(
        spotlightAccent = recoveryColor,
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
                    text = "PRESCRIPCIÓN DE HOY",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = recoveryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = prescription.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = recoveryColor
            )

            // Cajas métricas de Esfuerzo objetivo e Intensidad (con altura intrínseca simétrica)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhoopColors.SurfaceElevated,
                    border = BorderStroke(1.dp, WhoopColors.Outline),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "OBJETIVO DE ESFUERZO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = prescription.targetStrainRange,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhoopColors.SurfaceElevated,
                    border = BorderStroke(1.dp, WhoopColors.Outline),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "INTENSIDAD MÁXIMA",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        val rpeLines = prescription.recommendedRpe.split("\n", limit = 2)
                        Text(
                            text = rpeLines.firstOrNull() ?: prescription.recommendedRpe,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = recoveryColor
                        )
                        if (rpeLines.size > 1) {
                            Text(
                                text = rpeLines[1],
                                style = MaterialTheme.typography.labelSmall,
                                color = WhoopColors.OnSurfaceSecondary
                            )
                        }
                    }
                }
            }

            // Actividad sugerida
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ACTIVIDAD SUGERIDA",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = WhoopColors.OnSurfaceTertiary
                )
                Text(
                    text = prescription.suggestedActivity,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WhoopColors.OnSurface
                )
            }

            HorizontalDivider(color = WhoopColors.Outline)

            Text(
                text = prescription.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun SleepPlannerCard(
    sleepPlan: com.salud.personal.domain.coach.SleepCoachPlan
) {
    EasySpotlightCard(
        spotlightAccent = WhoopColors.Sleep,
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
                    text = "PLANIFICADOR DE DESCANSO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = WhoopColors.Sleep,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Hora sugerida para dormir",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = sleepPlan.targetBedtime,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Sleep
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Necesidad de sueño",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = sleepPlan.sleepNeedFormatted,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface
                    )
                    Text(
                        text = sleepPlan.sleepDebtFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.Warning
                    )
                }
            }

            // Hitos circadiano en fila
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WhoopColors.SurfaceElevated, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircadianMilestone(
                    time = sleepPlan.caffeineCutoff,
                    label = "Fin cafeína",
                    description = "Aclarar adenosina"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(WhoopColors.Outline)
                )
                CircadianMilestone(
                    time = sleepPlan.dinnerCutoff,
                    label = "Cena ligera",
                    description = "Bajar FC nocturna"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(WhoopColors.Outline)
                )
                CircadianMilestone(
                    time = sleepPlan.targetBedtime,
                    label = "A la cama",
                    description = "Recuperar 100%"
                )
            }

            Text(
                text = sleepPlan.tip,
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun CircadianMilestone(
    time: String,
    label: String,
    description: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = WhoopColors.Sleep
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = WhoopColors.OnSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = WhoopColors.OnSurfaceTertiary
        )
    }
}

@Composable
private fun LoadGuardCard(
    loadGuard: com.salud.personal.domain.coach.LoadGuardStatus
) {
    val guardColor = when {
        !loadGuard.isCalibrated -> WhoopColors.OnSurfaceTertiary
        loadGuard.isOverreaching -> WhoopColors.Error
        else -> WhoopColors.RecoveryHigh
    }

    EasySpotlightCard(
        spotlightAccent = guardColor,
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
                    text = "EQUILIBRIO DE CARGA Y RIESGO",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = guardColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = guardColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, guardColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "ACWR ${loadGuard.acwrRatioText}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = guardColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = loadGuard.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurface
                )
            }

            Text(
                text = loadGuard.advice,
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceSecondary
            )
        }
    }
}

@Composable
private fun ActionableFaqSection(
    faqs: List<com.salud.personal.domain.coach.QuickFaqItem>,
    expandedId: String?,
    onToggleExpand: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "DECISIONES RÁPIDAS DEL ATLETA",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            fontWeight = FontWeight.SemiBold,
            color = WhoopColors.OnSurfaceSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        faqs.forEach { faq ->
            val isExpanded = faq.id == expandedId
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WhoopColors.Surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) WhoopColors.Strain else WhoopColors.Outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .tactilePress(scaleDown = 0.98f)
                    .clickable { onToggleExpand(faq.id) }
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
                        Text(
                            text = faq.question,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExpanded) WhoopColors.Strain else WhoopColors.OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Contraer" else "Expandir",
                            tint = WhoopColors.OnSurfaceTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(
                                color = WhoopColors.Outline,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Text(
                                text = faq.answer,
                                style = MaterialTheme.typography.bodySmall,
                                color = WhoopColors.OnSurfaceSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AthleteSmartQueryChipsCard(
    snapshot: UserHealthSnapshot,
    selectedQuestion: String?,
    onSelectQuestion: (String) -> Unit
) {
    val suggestedQuestions = listOf(
        "¿Debo entrenar hoy o descansar?",
        "¿Fuerza o cardio hoy?",
        "¿Cómo compenso mi deuda de sueño?",
        "¿Qué RPE es seguro tolerar hoy?",
        "¿Por qué mi recuperación es ${snapshot.recoveryScore}%?",
        "¿A qué hora debo cenar e ir a dormir?"
    )

    EasySpotlightCard(
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
                    text = "CONSULTAS BIOMÉTRICAS RÁPIDAS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Text(
                    text = "En 1 toque",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedQuestions.chunked(2).forEach { rowQuestions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowQuestions.forEach { question ->
                            val isSelected = selectedQuestion == question
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) WhoopColors.Strain else WhoopColors.SurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) WhoopColors.Strain else WhoopColors.Outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .tactilePress(scaleDown = 0.95f)
                                    .clickable {
                                        onSelectQuestion(question)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) WhoopColors.Background else WhoopColors.OnSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        if (rowQuestions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedQuestion != null) {
                val answer = CoachHeuristicEngine.answerQuestion(selectedQuestion, snapshot)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhoopColors.SurfaceElevated,
                    border = BorderStroke(1.dp, WhoopColors.Strain.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedQuestion,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhoopColors.OnSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCorrelationSection(
    alerts: List<com.salud.personal.domain.coach.HabitCorrelationAlert>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "CORRELACIONES DE HÁBITOS DETECTADAS",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
            fontWeight = FontWeight.SemiBold,
            color = WhoopColors.OnSurfaceSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        alerts.forEach { alert ->
            EasySpotlightCard(
                spotlightAccent = WhoopColors.Warning,
                modifier = Modifier.fillMaxWidth()
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = alert.icon, fontSize = 20.sp)
                            Text(
                                text = alert.habitTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.OnSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WhoopColors.Warning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = alert.metricDeltaText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WhoopColors.Warning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = alert.advice,
                        style = MaterialTheme.typography.bodySmall,
                        color = WhoopColors.OnSurfaceSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun CoachScreenPreview() {
    SaludPersonalTheme {
        val sample = UserHealthSnapshot(
            stepsToday = 6200,
            recoveryScore = 78,
            restingHeartRate = 49,
            sleepHours = 7.8,
            recent7DaysTrimp = 240.0,
            lastWorkoutType = "CROSSFIT_WOD",
            weightTrendKgDelta = -0.4,
            sleepNeedHours = 7.8,
            targetStrainMin = 12.0,
            targetStrainMax = 15.0,
            rpeRecommendation = 8,
            targetBedtime = "23:15"
        )
        CoachScreen(snapshot = sample)
    }
}
