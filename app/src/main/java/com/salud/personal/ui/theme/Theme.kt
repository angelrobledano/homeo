package com.salud.personal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WhoopDarkColorScheme = darkColorScheme(
    primary = WhoopColors.Strain,
    onPrimary = WhoopColors.Background,
    primaryContainer = WhoopColors.StrainDim,
    onPrimaryContainer = WhoopColors.OnSurface,
    secondary = WhoopColors.Sleep,
    onSecondary = WhoopColors.Background,
    secondaryContainer = WhoopColors.SleepDim,
    onSecondaryContainer = WhoopColors.OnSurface,
    tertiary = WhoopColors.RecoveryHigh,
    onTertiary = WhoopColors.Background,
    tertiaryContainer = WhoopColors.RecoveryHigh.copy(alpha = 0.2f),
    onTertiaryContainer = WhoopColors.RecoveryHigh,
    error = WhoopColors.Error,
    onError = WhoopColors.OnSurface,
    errorContainer = WhoopColors.ErrorContainer,
    onErrorContainer = WhoopColors.Error,
    background = WhoopColors.Background,
    onBackground = WhoopColors.OnSurface,
    surface = WhoopColors.Surface,
    onSurface = WhoopColors.OnSurface,
    surfaceVariant = WhoopColors.SurfaceElevated,
    onSurfaceVariant = WhoopColors.OnSurfaceSecondary,
    outline = WhoopColors.Outline,
    outlineVariant = WhoopColors.OutlineVariant,
    inverseSurface = WhoopColors.OnSurface,
    inverseOnSurface = WhoopColors.Background,
    inversePrimary = WhoopColors.StrainDim,
    surfaceTint = WhoopColors.Strain,
)

@Composable
fun SaludPersonalTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = WhoopDarkColorScheme,
        typography = WhoopTypography,
        content = content
    )
}
