package com.salud.personal.ui.navigation

import androidx.annotation.StringRes
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
import com.salud.personal.R

/**
 * Destinos de la navegación principal de 4 pestañas con soporte i18n bilingüe.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOY(
        route = "hoy",
        label = "Hoy",
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.Today,
        unselectedIcon = Icons.Outlined.Today
    ),
    ENTRENAMIENTO(
        route = "entrenamiento",
        label = "Entreno",
        labelRes = R.string.nav_workout,
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    ),
    PROGRESO(
        route = "progreso",
        label = "Progreso",
        labelRes = R.string.nav_progress,
        selectedIcon = Icons.AutoMirrored.Filled.ShowChart,
        unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart
    ),
    COACH(
        route = "coach",
        label = "Coach",
        labelRes = R.string.nav_coach,
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    );

    companion object {
        fun fromRoute(route: String?): TopLevelDestination? =
            entries.find { it.route == route }
    }
}
