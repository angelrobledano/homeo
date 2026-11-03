package com.salud.personal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de la navegación principal de 4 pestañas.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOY(
        route = "hoy",
        label = "Hoy",
        selectedIcon = Icons.Filled.Today,
        unselectedIcon = Icons.Outlined.Today
    ),
    ENTRENAMIENTO(
        route = "entrenamiento",
        label = "Entreno",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ),
    PROGRESO(
        route = "progreso",
        label = "Progreso",
        selectedIcon = Icons.AutoMirrored.Filled.ShowChart,
        unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart
    ),
    COACH(
        route = "coach",
        label = "Coach",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    );

    companion object {
        fun fromRoute(route: String?): TopLevelDestination? =
            entries.find { it.route == route }
    }
}
