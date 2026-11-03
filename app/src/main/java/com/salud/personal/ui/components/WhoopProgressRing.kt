package com.salud.personal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors

/**
 * Anillo de progreso circular estilo WHOOP con degradado de color,
 * valor numérico central y etiqueta inferior.
 *
 * @param progress Valor de progreso 0.0-1.0 (se clampea automáticamente).
 * @param displayValue Texto grande central (el número).
 * @param displayUnit Unidad mostrada debajo del valor.
 * @param accentColor Color principal del arco de progreso.
 * @param accentColorEnd Color final del degradado (null = sin degradado).
 * @param trackColor Color del track de fondo del anillo.
 * @param ringSize Diámetro total del anillo.
 * @param strokeWidth Grosor del trazo del arco.
 */
@Composable
fun WhoopProgressRing(
    progress: Float,
    displayValue: String,
    displayUnit: String = "",
    accentColor: Color = WhoopColors.Strain,
    accentColorEnd: Color? = null,
    trackColor: Color = WhoopColors.StrainTrack,
    ringSize: Dp = 180.dp,
    strokeWidth: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "ring_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(ringSize)
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweepAngle = animatedProgress * 360f
            if (sweepAngle > 0.5f) {
                val brush = if (accentColorEnd != null) {
                    Brush.sweepGradient(
                        colors = listOf(accentColor, accentColorEnd, accentColor),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                } else {
                    Brush.sweepGradient(
                        colors = listOf(accentColor, accentColor),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
                drawArc(
                    brush = brush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Central text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-1).sp
                ),
                fontWeight = FontWeight.Bold,
                color = WhoopColors.OnSurface,
                textAlign = TextAlign.Center
            )
            if (displayUnit.isNotEmpty()) {
                Text(
                    text = displayUnit,
                    style = MaterialTheme.typography.labelSmall,
                    color = WhoopColors.OnSurfaceSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun WhoopProgressRingStrainPreview() {
    SaludPersonalTheme {
        WhoopProgressRing(
            progress = 0.72f,
            displayValue = "14.2",
            displayUnit = "ESFUERZO",
            accentColor = WhoopColors.Strain,
            accentColorEnd = WhoopColors.StrainDim
        )
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun WhoopProgressRingRecoveryPreview() {
    SaludPersonalTheme {
        WhoopProgressRing(
            progress = 0.82f,
            displayValue = "82%",
            displayUnit = "RECUPERACIÓN",
            accentColor = WhoopColors.RecoveryHigh,
            trackColor = WhoopColors.RecoveryHigh.copy(alpha = 0.1f)
        )
    }
}
