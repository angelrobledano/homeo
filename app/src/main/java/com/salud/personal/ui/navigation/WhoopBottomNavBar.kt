package com.salud.personal.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.salud.personal.ui.theme.WhoopColors

/**
 * Barra de navegación inferior con 4 áreas principales.
 * Estilo oscuro WHOOP sin bordes visibles.
 */
@Composable
fun WhoopBottomNavBar(
    currentDestination: TopLevelDestination,
    onNavigate: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = WhoopColors.Surface,
        contentColor = WhoopColors.OnSurface,
        modifier = modifier
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination == destination
            val localizedLabel = androidx.compose.ui.res.stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = localizedLabel
                    )
                },
                label = {
                    Text(
                        text = localizedLabel,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WhoopColors.Strain,
                    selectedTextColor = WhoopColors.Strain,
                    unselectedIconColor = WhoopColors.OnSurfaceTertiary,
                    unselectedTextColor = WhoopColors.OnSurfaceTertiary,
                    indicatorColor = WhoopColors.Strain.copy(alpha = 0.12f)
                )
            )
        }
    }
}
