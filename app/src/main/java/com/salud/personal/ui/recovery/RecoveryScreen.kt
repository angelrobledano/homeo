package com.salud.personal.ui.recovery

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.domain.recovery.RecoveryCalculator
import com.salud.personal.ui.components.WhoopProgressRing
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.WhoopColors

/**
 * Pantalla de recuperación y check-in matutino de sensaciones con estética EasyUI.
 */
@Composable
fun RecoveryScreen(
    currentRhr: Int = 50,
    baselineRhr: Int = 49,
    sleepHours: Double = 7.5,
    initialLateMeal: Boolean = false,
    initialLateCaffeine: Boolean = false,
    initialAlcohol: Boolean = false,
    initialHighStress: Boolean = false,
    initialSupplements: Boolean = false,
    initialScreenBeforeBed: Boolean = false,
    onSaveCheckIn: (
        energy: Int,
        sleepQuality: Int,
        soreness: Int,
        readiness: Int,
        lateMeal: Boolean,
        lateCaffeine: Boolean,
        alcohol: Boolean,
        highStress: Boolean,
        supplements: Boolean,
        screenBeforeBed: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var energy by remember { mutableFloatStateOf(4f) }
    var sleepQuality by remember { mutableFloatStateOf(4f) }
    var soreness by remember { mutableFloatStateOf(2f) }
    var readiness by remember { mutableFloatStateOf(4f) }

    var lateMeal by remember { mutableStateOf(initialLateMeal) }
    var lateCaffeine by remember { mutableStateOf(initialLateCaffeine) }
    var alcohol by remember { mutableStateOf(initialAlcohol) }
    var highStress by remember { mutableStateOf(initialHighStress) }
    var supplements by remember { mutableStateOf(initialSupplements) }
    var screenBeforeBed by remember { mutableStateOf(initialScreenBeforeBed) }

    val recoveryResult = RecoveryCalculator.calculateRecovery(
        currentRhr = currentRhr,
        baselineRhr = baselineRhr,
        sleepHours = sleepHours,
        subjectiveRating = readiness.toInt()
    )

    val recoveryColor = WhoopColors.recoveryColor(recoveryResult.score)

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

        // Recovery Score Ring Card con EasyUI Spotlight
        EasySpotlightCard(
            spotlightAccent = recoveryColor,
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
                    text = "RECUPERACIÓN ESTIMADA",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                WhoopProgressRing(
                    progress = recoveryResult.score / 100f,
                    displayValue = "${recoveryResult.score}%",
                    displayUnit = "ESTADO FÍSICO",
                    accentColor = recoveryColor,
                    trackColor = recoveryColor.copy(alpha = 0.15f),
                    ringSize = 150.dp,
                    strokeWidth = 10.dp
                )

                Text(
                    text = "Factor principal: ${recoveryResult.primaryDriver}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
            }
        }

        // Check-in Sliders Card con EasyUI Spotlight
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
                Text(
                    text = "CHECK-IN MATUTINO DE SENSACIONES",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                CheckInSliderItem(
                    label = "Nivel de energía subjetiva",
                    value = energy,
                    onValueChange = { energy = it }
                )

                HorizontalDivider(color = WhoopColors.Outline)

                CheckInSliderItem(
                    label = "Calidad percibida del sueño",
                    value = sleepQuality,
                    onValueChange = { sleepQuality = it }
                )

                HorizontalDivider(color = WhoopColors.Outline)

                CheckInSliderItem(
                    label = "Dolor muscular / Fatiga (1=nada, 5=muy alto)",
                    value = soreness,
                    onValueChange = { soreness = it }
                )

                HorizontalDivider(color = WhoopColors.Outline)

                CheckInSliderItem(
                    label = "Predisposición a entrenar fuerte",
                    value = readiness,
                    onValueChange = { readiness = it }
                )

                HorizontalDivider(color = WhoopColors.Outline)

                // Diario de Hábitos estilo WHOOP Journal
                Text(
                    text = "DIARIO DE HÁBITOS (IMPACTO EN DESCANSO)",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitToggleChip(
                            icon = "🌙",
                            title = "Cena tardía",
                            subtitle = "<2h de dormir",
                            selected = lateMeal,
                            onToggle = { lateMeal = it },
                            modifier = Modifier.weight(1f)
                        )
                        HabitToggleChip(
                            icon = "☕",
                            title = "Cafeína tarde",
                            subtitle = "> 17:00h",
                            selected = lateCaffeine,
                            onToggle = { lateCaffeine = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitToggleChip(
                            icon = "🍷",
                            title = "Alcohol",
                            subtitle = "Consumo anoche",
                            selected = alcohol,
                            onToggle = { alcohol = it },
                            modifier = Modifier.weight(1f)
                        )
                        HabitToggleChip(
                            icon = "⚡",
                            title = "Estrés alto",
                            subtitle = "Tensión mental",
                            selected = highStress,
                            onToggle = { highStress = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitToggleChip(
                            icon = "💊",
                            title = "Suplementos",
                            subtitle = "Magnesio/Creatina",
                            selected = supplements,
                            onToggle = { supplements = it },
                            modifier = Modifier.weight(1f)
                        )
                        HabitToggleChip(
                            icon = "📱",
                            title = "Pantallas",
                            subtitle = "Uso en la cama",
                            selected = screenBeforeBed,
                            onToggle = { screenBeforeBed = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSaveCheckIn(
                            energy.toInt(),
                            sleepQuality.toInt(),
                            soreness.toInt(),
                            readiness.toInt(),
                            lateMeal,
                            lateCaffeine,
                            alcohol,
                            highStress,
                            supplements,
                            screenBeforeBed
                        )
                        Toast.makeText(context, "Check-in y hábitos guardados ✓", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhoopColors.Strain),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactilePress(scaleDown = 0.94f)
                ) {
                    Text(
                        text = "Guardar check-in de hoy",
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Background
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CheckInSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = WhoopColors.OnSurfaceSecondary
            )
            Text(
                text = "${value.toInt()}/5",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = WhoopColors.Strain
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = WhoopColors.Strain,
                activeTrackColor = WhoopColors.Strain,
                inactiveTrackColor = WhoopColors.Outline
            )
        )
    }
}

@Composable
private fun HabitToggleChip(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) WhoopColors.Strain.copy(alpha = 0.2f) else WhoopColors.SurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) WhoopColors.Strain else WhoopColors.Outline
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .tactilePress(scaleDown = 0.96f)
            .clickable { onToggle(!selected) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) WhoopColors.Strain else WhoopColors.OnSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = WhoopColors.OnSurfaceTertiary,
                    maxLines = 1
                )
            }
            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = WhoopColors.Strain
                )
            }
        }
    }
}
