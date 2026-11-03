package com.salud.personal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores premium deportiva inspirada en WHOOP.
 * Fondo profundo negro, acentos funcionales de alto contraste.
 */
object WhoopColors {
    // Backgrounds
    val Background = Color(0xFF0A0B0D)
    val Surface = Color(0xFF131519)
    val SurfaceElevated = Color(0xFF1C1F26)
    val SurfaceBright = Color(0xFF252830)

    // On-surface (WCAG AA Compliant contrast against Background #0A0B0D)
    val OnSurface = Color(0xFFF0F0F2)
    val OnSurfaceSecondary = Color(0xFF9EA2AC)
    val OnSurfaceTertiary = Color(0xFF8E929B)

    // Strain / Esfuerzo (cyan deportivo)
    val Strain = Color(0xFF00E5FF)
    val StrainDim = Color(0xFF00798A)
    val StrainTrack = Color(0xFF1A2A2E)

    // Recovery / Recuperación (tricolor)
    val RecoveryHigh = Color(0xFF00E676)     // Verde esmeralda ≥67%
    val RecoveryMedium = Color(0xFFFFB300)   // Ámbar 34-66%
    val RecoveryLow = Color(0xFFFF5252)      // Rojo <34%

    // Sleep / Sueño
    val Sleep = Color(0xFF7C4DFF)
    val SleepDim = Color(0xFF4A2E99)
    val SleepTrack = Color(0xFF1A1528)

    // Sleep Stages
    val SleepDeep = Color(0xFF4338CA)
    val SleepRem = Color(0xFFA855F7)
    val SleepLight = Color(0xFF38BDF8)
    val SleepAwake = Color(0xFFF59E0B)

    // Cardiovascular Zones
    val Zone1Recovery = Color(0xFF38BDF8)
    val Zone2Aerobic = Color(0xFF10B981)
    val Zone3Tempo = Color(0xFFF59E0B)
    val Zone4Threshold = Color(0xFFF97316)
    val Zone5Peak = Color(0xFFEF4444)

    // Functional
    val Error = Color(0xFFFF5252)
    val ErrorContainer = Color(0xFF3D1515)
    val Warning = Color(0xFFFFB300)
    val Success = Color(0xFF00E676)
    val Outline = Color(0xFF2A2D35)
    val OutlineVariant = Color(0xFF1E2128)

    // Weight / Peso
    val Weight = Color(0xFF42A5F5)
    val WeightDim = Color(0xFF245B8A)

    // Heart Rate
    val HeartRate = Color(0xFFEF5350)
    val HeartRateDim = Color(0xFF8B2E2D)

    // Glass & Ambient Effects
    val GlassHighlight = Color(0x44FFFFFF)
    val GlassBorderMid = Color(0x1A27272A)
    val GlassBorderDark = Color(0x0D000000)
    val GlassSurfaceTop = Color(0xF0181A20)
    val GlassSurfaceBottom = Color(0xF80E0F12)
    val SpotlightBorderTop = Color(0xFF3F3F46)
    val SpotlightBorderMid = Color(0xFF27272A)
    val SpotlightBorderBottom = Color(0xFF1E1F23)
    val SpotlightBorderDark = Color(0xFF18181B)
    val SpotlightSurfaceTop = Color(0xFF18191D)

    /**
     * Devuelve el color de recuperación según el porcentaje (0-100).
     */
    fun recoveryColor(percentage: Int): Color = when {
        percentage >= 67 -> RecoveryHigh
        percentage >= 34 -> RecoveryMedium
        else -> RecoveryLow
    }
}
