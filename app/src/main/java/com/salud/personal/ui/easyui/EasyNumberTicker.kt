package com.salud.personal.ui.easyui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * EasyUI Number Ticker:
 * Transición vertical suave tipo odómetro al actualizar métricas numéricas.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EasyNumberTicker(
    value: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            slideInVertically { height -> height } togetherWith
                slideOutVertically { height -> -height }
        },
        label = "numberTicker",
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            style = style.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
