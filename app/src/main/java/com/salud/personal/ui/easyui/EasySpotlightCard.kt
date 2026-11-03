package com.salud.personal.ui.easyui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.salud.personal.ui.theme.WhoopColors

/**
 * EasyUI Spotlight Card para Jetpack Compose:
 * Tarjeta de superficie oscura refinada con borde sutil (zinc-800 / #27272A),
 * gradiente de iluminación ambiental superior ("spotlight") y esquinas redondeadas.
 */
@Composable
fun EasySpotlightCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = WhoopColors.Surface,
    spotlightAccent: Color? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = if (spotlightAccent != null) {
        Brush.verticalGradient(
            colors = listOf(
                spotlightAccent.copy(alpha = 0.45f),
                WhoopColors.SpotlightBorderMid.copy(alpha = 0.5f),
                WhoopColors.SpotlightBorderBottom.copy(alpha = 0.2f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                WhoopColors.SpotlightBorderTop.copy(alpha = 0.5f),
                WhoopColors.SpotlightBorderMid.copy(alpha = 0.4f),
                WhoopColors.SpotlightBorderDark.copy(alpha = 0.2f)
            )
        )
    }

    val backgroundBrush = if (spotlightAccent != null) {
        Brush.radialGradient(
            colors = listOf(
                spotlightAccent.copy(alpha = 0.08f),
                backgroundColor
            ),
            radius = 600f
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                WhoopColors.SpotlightSurfaceTop,
                backgroundColor
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.tactilePress(scaleDown = 0.98f, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .border(BorderStroke(borderWidth, borderBrush), shape)
            .background(backgroundBrush)
    ) {
        content()
    }
}
