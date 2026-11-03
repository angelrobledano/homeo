package com.salud.personal.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.data.backup.DataBackupManager
import com.salud.personal.data.local.AppDatabase
import com.salud.personal.domain.export.DataExporter
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.WhoopColors
import kotlinx.coroutines.launch

/**
 * Pantalla de Ajustes, Diagnóstico de Dispositivos y Backup CSV/JSON con EasyUI.
 */
@Composable
fun SettingsScreen(
    gtr3Connected: Boolean = true,
    gtr3Battery: Int = 85,
    healthConnectAvailable: Boolean = true,
    onSyncHealthConnect: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportFeedback by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

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
                modifier = Modifier.tactilePress(scaleDown = 0.94f)
            ) {
                Text("← Volver a Hoy", color = WhoopColors.OnSurface)
            }
        }

        // Connected Devices Status Card con EasyUI Spotlight
        EasySpotlightCard(
            spotlightAccent = if (gtr3Connected) WhoopColors.RecoveryHigh else WhoopColors.Warning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DISPOSITIVOS VINCULADOS",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Amazfit GTR 3 (Zepp OS)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        Text(
                            text = if (gtr3Connected) "Conectado · Batería: $gtr3Battery%" else "Desconectado",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gtr3Connected) WhoopColors.RecoveryHigh else WhoopColors.OnSurfaceTertiary
                        )
                    }
                }

                HorizontalDivider(color = WhoopColors.Outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Báscula RENPHO Elis 1",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        Text(
                            text = "Escucha BLE automática activa (0xFFE0)",
                            style = MaterialTheme.typography.bodySmall,
                            color = WhoopColors.RecoveryHigh
                        )
                    }
                }

                HorizontalDivider(color = WhoopColors.Outline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Google Health Connect",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                        Text(
                            text = if (healthConnectAvailable) "Sincronización con Renpho Health (Peso y Grasa)" else "Health Connect no disponible en este dispositivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (healthConnectAvailable) WhoopColors.RecoveryHigh else WhoopColors.OnSurfaceTertiary
                        )
                    }

                    if (healthConnectAvailable) {
                        OutlinedButton(
                            onClick = onSyncHealthConnect,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = WhoopColors.SurfaceElevated,
                                contentColor = WhoopColors.Strain
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.tactilePress(scaleDown = 0.94f)
                        ) {
                            Text("Sincronizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Data Backup & Export Card con EasyUI Spotlight
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
                Text(
                    text = "EXPORTACIÓN Y PRIVACIDAD",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )

                Text(
                    text = "Tus datos biométricos residen únicamente en este dispositivo. Puedes exportar el historial completo a archivos CSV abiertos (pesos, entrenamientos, pasos y pulso).",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhoopColors.OnSurfaceTertiary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val db = AppDatabase.getInstance(context)
                                        val files = DataBackupManager.exportAllToFiles(context, db)
                                        exportFeedback = "4 archivos CSV creados: ${files.joinToString { it.name }}"
                                        Toast.makeText(context, "Exportación CSV completada", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        exportFeedback = "Error al exportar: ${e.message}"
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhoopColors.SurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = if (isExporting) "Exportando..." else "Exportar CSV",
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (!isExporting) {
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val db = AppDatabase.getInstance(context)
                                        val file = DataExporter.exportDatabaseToJson(context, db)
                                        val shareIntent = DataExporter.createShareIntent(context, file)
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Guardar o compartir copia JSON"))
                                        exportFeedback = "Backup JSON generado: ${file.name}"
                                    } catch (e: Exception) {
                                        exportFeedback = "Error al exportar JSON: ${e.message}"
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhoopColors.Strain),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .tactilePress(scaleDown = 0.94f)
                    ) {
                        Text(
                            text = "Backup JSON ↗",
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Background
                        )
                    }
                }

                if (exportFeedback != null) {
                    Text(
                        text = exportFeedback ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.RecoveryHigh
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
