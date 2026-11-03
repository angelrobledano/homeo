package com.salud.personal.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.salud.personal.domain.gps.GpsDistanceCalculator
import com.salud.personal.domain.gps.GpsLocationPoint
import com.salud.personal.ui.dashboard.DashboardFormatting
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.easyui.tactilePress
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors
import android.content.Context
import android.location.LocationManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.salud.personal.data.service.WalkTrackingService

/**
 * Pantalla interactiva de grabación de caminatas largas con GPS
 * y renderizado de mapas OpenStreetMap mediante osmdroid (Compose AndroidView).
 */
@Composable
fun WalkTrackingScreen(
    points: List<GpsLocationPoint> = emptyList(),
    isTracking: Boolean = false,
    elapsedSeconds: Long = 0L,
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveState by WalkTrackingService.trackingState.collectAsState()

    val effectivePoints = if (points.isNotEmpty()) points else liveState.points
    val effectiveIsTracking = if (isTracking) true else liveState.isTracking
    val effectiveElapsed = if (elapsedSeconds > 0) elapsedSeconds else liveState.elapsedSeconds

    val totalDistanceMeters = GpsDistanceCalculator.calculateTotalDistance(effectivePoints)
    val paceMinKm = GpsDistanceCalculator.calculatePaceMinutesPerKm(totalDistanceMeters, effectiveElapsed)

    val handleStart = {
        WalkTrackingService.start(context)
        onStartTracking()
    }
    val handleStop = {
        WalkTrackingService.stop(context)
        onStopTracking()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WhoopColors.Background)
    ) {
        // Map Container with overlaid Back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        val locManager = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val lastLoc = try {
                            locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        } catch (e: SecurityException) {
                            null
                        }

                        val startPoint = when {
                            effectivePoints.isNotEmpty() -> GeoPoint(effectivePoints.last().latitude, effectivePoints.last().longitude)
                            lastLoc != null -> GeoPoint(lastLoc.latitude, lastLoc.longitude)
                            else -> GeoPoint(40.4168, -3.7038) // Madrid def
                        }

                        post {
                            controller.setZoom(16.5)
                            controller.setCenter(startPoint)
                        }

                        if (effectivePoints.size >= 2) {
                            val polyline = Polyline(this)
                            polyline.outlinePaint.color = android.graphics.Color.CYAN
                            polyline.outlinePaint.strokeWidth = 8f
                            effectivePoints.forEach { pt ->
                                polyline.addPoint(GeoPoint(pt.latitude, pt.longitude))
                            }
                            overlays.add(polyline)
                        }
                        onResume()
                    }
                },
                update = { mapView ->
                    if (effectivePoints.isNotEmpty()) {
                        mapView.controller.animateTo(GeoPoint(effectivePoints.last().latitude, effectivePoints.last().longitude))
                        if (effectivePoints.size >= 2) {
                            mapView.overlays.removeAll { it is Polyline }
                            val polyline = Polyline(mapView)
                            polyline.outlinePaint.color = android.graphics.Color.CYAN
                            polyline.outlinePaint.strokeWidth = 8f
                            effectivePoints.forEach { pt ->
                                polyline.addPoint(GeoPoint(pt.latitude, pt.longitude))
                            }
                            mapView.overlays.add(polyline)
                            mapView.invalidate()
                        }
                    }
                },
                onRelease = { mapView ->
                    mapView.onPause()
                    mapView.onDetach()
                },
                modifier = Modifier.fillMaxSize()
            )

            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = WhoopColors.Surface.copy(alpha = 0.85f),
                        contentColor = WhoopColors.OnSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .tactilePress(scaleDown = 0.94f)
                ) {
                    Text("← Volver", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Live HUD Metrics Card con EasySpotlightCard
        EasySpotlightCard(
            spotlightAccent = if (effectiveIsTracking) WhoopColors.RecoveryHigh else WhoopColors.Strain,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAMINATA GPS EN VIVO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = WhoopColors.OnSurfaceSecondary
                    )
                    Text(
                        text = if (effectiveIsTracking) "GRABANDO ●" else "PAUSADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (effectiveIsTracking) WhoopColors.RecoveryHigh else WhoopColors.Warning
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Distance
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Distancia",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        Text(
                            text = DashboardFormatting.formatDistance(totalDistanceMeters.toInt()),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.Strain
                        )
                    }

                    // Pace
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Ritmo",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhoopColors.OnSurfaceTertiary
                        )
                        val paceText = if (paceMinKm > 0.1) String.format(java.util.Locale.US, "%.1f min/km", paceMinKm) else "-- min/km"
                        Text(
                            text = paceText,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = WhoopColors.OnSurface
                        )
                    }
                }

                // Control Button con tactilePress
                Button(
                    onClick = if (effectiveIsTracking) handleStop else handleStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (effectiveIsTracking) WhoopColors.Error else WhoopColors.Strain
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .tactilePress(scaleDown = 0.94f)
                ) {
                    Text(
                        text = if (effectiveIsTracking) "Finalizar Caminata" else "Iniciar Grabación GPS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = WhoopColors.Background
                    )
                }
            }
        }
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun WalkTrackingScreenPreview() {
    SaludPersonalTheme {
        WalkTrackingScreen(
            points = listOf(
                GpsLocationPoint(40.4168, -3.7038),
                GpsLocationPoint(40.4180, -3.7050)
            ),
            isTracking = true,
            elapsedSeconds = 1240L
        )
    }
}
