package com.salud.personal.ui.easyui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salud.personal.ui.navigation.TopLevelDestination
import com.salud.personal.ui.theme.WhoopColors

/**
 * EasyUI Glass Navigation Bar para Jetpack Compose:
 * Barra estilo muelle flotante con aspecto traslúcido glassmorphism,
 * bordes biselados con gradiente y transiciones suaves de muelle.
 */
@Composable
fun EasyGlassNavBar(
    currentDestination: TopLevelDestination,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val navShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(navShape)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            WhoopColors.GlassHighlight,
                            WhoopColors.GlassBorderMid,
                            WhoopColors.GlassBorderDark
                        )
                    )
                ),
                navShape
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WhoopColors.GlassSurfaceTop,
                        WhoopColors.GlassSurfaceBottom
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = currentDestination == destination

                val activeColor by animateColorAsState(
                    targetValue = if (isSelected) WhoopColors.Strain else WhoopColors.OnSurfaceTertiary,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "navColor"
                )

                val pillBackground = if (isSelected) {
                    WhoopColors.Strain.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .tactilePress(scaleDown = 0.90f, onClick = { onNavigate(destination) })
                        .clip(RoundedCornerShape(18.dp))
                        .background(pillBackground)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val localizedLabel = androidx.compose.ui.res.stringResource(destination.labelRes)
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = localizedLabel,
                            tint = activeColor
                        )
                        Text(
                            text = localizedLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = activeColor
                        )
                    }
                }
            }
        }
    }
}
