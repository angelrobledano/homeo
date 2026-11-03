package com.salud.personal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.ui.easyui.EasyNumberTicker
import com.salud.personal.ui.easyui.EasySpotlightCard
import com.salud.personal.ui.theme.SaludPersonalTheme
import com.salud.personal.ui.theme.WhoopColors

@Composable
fun BiometricMetricCard(
    label: String,
    value: String,
    unit: String = "",
    delta: String? = null,
    deltaColor: Color = WhoopColors.Strain,
    sourceLabel: String? = null,
    accentColor: Color = WhoopColors.OnSurface,
    modifier: Modifier = Modifier,
    extraContent: @Composable (() -> Unit)? = null
) {
    EasySpotlightCard(
        spotlightAccent = accentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: label + source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.5.sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = WhoopColors.OnSurfaceSecondary
                )
                if (sourceLabel != null) {
                    Text(
                        text = sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = WhoopColors.OnSurfaceTertiary
                    )
                }
            }

            // Value row: big number with EasyUI NumberTicker + unit
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EasyNumberTicker(
                    value = value,
                    style = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = (-1).sp
                    ),
                    color = accentColor
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WhoopColors.OnSurfaceSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Delta badge
            if (delta != null) {
                Text(
                    text = delta,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = deltaColor
                )
            }

            // Extra content slot
            extraContent?.invoke()
        }
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun BiometricMetricCardPreview() {
    SaludPersonalTheme {
        BiometricMetricCard(
            label = "FRECUENCIA CARDÍACA",
            value = "72",
            unit = "ppm",
            sourceLabel = "Amazfit GTR 3",
            accentColor = WhoopColors.HeartRate
        )
    }
}

@Preview(backgroundColor = 0xFF0A0B0D, showBackground = true)
@Composable
private fun BiometricMetricCardWeightPreview() {
    SaludPersonalTheme {
        BiometricMetricCard(
            label = "PESO CORPORAL",
            value = "74.85",
            unit = "kg",
            delta = "7d: -0.40 kg",
            deltaColor = WhoopColors.RecoveryHigh,
            sourceLabel = "RENPHO Elis 1"
        )
    }
}
