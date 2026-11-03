package com.salud.personal.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.strain.ManualWorkoutEstimator
import com.salud.personal.ui.theme.WhoopColors
import kotlin.math.roundToInt

@Composable
fun ManualWorkoutDialog(
    onDismiss: () -> Unit,
    onSaveWorkout: (type: WorkoutType, name: String, durationMinutes: Int, rpe: Int, notes: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(WorkoutType.CROSSFIT_WOD) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var rpe by remember { mutableIntStateOf(7) }

    val estimatedTrimp = remember(selectedType, durationMinutes, rpe) {
        ManualWorkoutEstimator.estimateTrimp(selectedType, durationMinutes, rpe)
    }
    val estimatedCalories = remember(selectedType, durationMinutes, rpe) {
        ManualWorkoutEstimator.estimateCalories(selectedType, durationMinutes, rpe)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WhoopColors.Surface,
            tonalElevation = 6.dp,
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
                        text = "REGISTRAR ENTRENO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurfaceSecondary
                    )
                    Text(
                        text = "Sin reloj / Manual",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.Strain
                    )
                }

                // 1. Selector de disciplina
                Text(
                    text = "DISCIPLINA",
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceTertiary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf(
                        WorkoutType.CROSSFIT_WOD to "CrossFit",
                        WorkoutType.STRENGTH to "Fuerza",
                        WorkoutType.ENDURANCE_CARDIO to "Cardio",
                        WorkoutType.WALK to "Caminar"
                    )
                    types.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(
                                    color = if (isSelected) WhoopColors.Strain else WhoopColors.SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = type }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) WhoopColors.Background else WhoopColors.OnSurface
                            )
                        }
                    }
                }

                HorizontalDivider(color = WhoopColors.Outline.copy(alpha = 0.3f))

                // 2. Duración
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DURACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = "$durationMinutes min",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.OnSurface
                    )
                }
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { durationMinutes = it.roundToInt() },
                    valueRange = 10f..120f,
                    steps = 21,
                    colors = SliderDefaults.colors(
                        thumbColor = WhoopColors.Strain,
                        activeTrackColor = WhoopColors.Strain,
                        inactiveTrackColor = WhoopColors.SurfaceElevated
                    )
                )

                // 3. RPE Esfuerzo Percibido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTENSIDAD (RPE 1-10)",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                    Text(
                        text = "$rpe / 10",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = WhoopColors.Strain
                    )
                }
                val rpeDesc = when (rpe) {
                    in 1..3 -> "Ligero · Recuperación activa"
                    in 4..6 -> "Moderado · Aeróbico constante"
                    in 7..8 -> "Duro · 1-2 repeticiones en recámara"
                    9 -> "Muy duro · Límite muscular"
                    else -> "Máximo · Esfuerzo exhaustivo"
                }
                Text(
                    text = rpeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceSecondary
                )
                Slider(
                    value = rpe.toFloat(),
                    onValueChange = { rpe = it.roundToInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = WhoopColors.Strain,
                        activeTrackColor = WhoopColors.Strain,
                        inactiveTrackColor = WhoopColors.SurfaceElevated
                    )
                )

                // 4. Previsualización de Carga Biomecánica
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WhoopColors.SurfaceElevated, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CARGA ESTIMADA",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = "${estimatedTrimp.toInt()} pts TRIMP",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CALORÍAS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = "~$estimatedCalories kcal",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                    }
                }

                // Botones Cancelar / Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar", color = WhoopColors.OnSurfaceSecondary)
                    }
                    Button(
                        onClick = {
                            val defaultName = when (selectedType) {
                                WorkoutType.CROSSFIT_WOD -> "WOD CrossFit"
                                WorkoutType.STRENGTH -> "Fuerza / Musculación"
                                WorkoutType.ENDURANCE_CARDIO -> "Cardio / Running"
                                WorkoutType.WALK -> "Caminata"
                                WorkoutType.RECOVERY -> "Recuperación Activa"
                            }
                            onSaveWorkout(selectedType, defaultName, durationMinutes, rpe, null)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhoopColors.Strain),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Guardar", color = WhoopColors.Background, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
