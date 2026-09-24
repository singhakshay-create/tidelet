package com.tidelet.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Brand Specification: Tidelet v0.1 (Refresh)
 * 
 * This theme enforces the "Quiet over Loud" and "Warm over Cold" principles.
 */

private val LightColors = lightColorScheme(
    primary              = SagePrimary,
    onPrimary            = SageOnPrimary,
    primaryContainer     = SageContainer,
    onPrimaryContainer   = SageOnContainer,

    background           = WarmIvory,
    onBackground         = Ink,

    surface              = PureWhite,
    onSurface            = Ink,
    surfaceVariant       = Color(0xFFF5F2EC), // surface-sunken
    onSurfaceVariant     = TextSecondary,

    error                = ErrorRed,
    onError              = PureWhite,

    outline              = BorderSubtle,
    outlineVariant       = BorderEmphasis,
)

private val DarkColors = darkColorScheme(
    primary              = PaleSage,
    onPrimary            = WarmBlack,
    primaryContainer     = DarkSageContainer,
    onPrimaryContainer   = DarkSageOnContainer,

    background           = WarmBlack,
    onBackground         = DarkInk,

    surface              = DarkSurface,
    onSurface            = DarkInk,
    surfaceVariant       = DarkSurfaceRaised,
    onSurfaceVariant     = DarkTextSecondary,

    error                = DarkError,
    onError              = WarmBlack,

    outline              = DarkBorderSubtle,
)

/**
 * Structural tokens for Tidelet.
 */
data class TideletExtendedColors(
    val utility: Color,
    val accent: Color,
    val surfaceSecondary: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderEmphasis: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val sosSurface: Color,
    val onSosSurface: Color,
    val sosAccent: Color,
    val sosAccentContainer: Color,
)

private val LightExtended = TideletExtendedColors(
    utility            = TextMuted,
    accent             = SagePrimary,
    surfaceSecondary   = Color(0xFFF5F2EC),
    border             = BorderSubtle,
    borderSubtle       = BorderSubtle,
    borderEmphasis     = BorderEmphasis,
    textSecondary      = TextSecondary,
    textMuted          = TextMuted,
    sosSurface         = WarmSand,
    onSosSurface       = OnTerracotta,
    sosAccent          = Terracotta,
    sosAccentContainer = TerracottaContainer,
)

private val DarkExtended = TideletExtendedColors(
    utility            = DarkTextMuted,
    accent             = PaleSage,
    surfaceSecondary   = DarkSurfaceRaised,
    border             = DarkBorderSubtle,
    borderSubtle       = DarkBorderSubtle,
    borderEmphasis     = PaleSage,
    textSecondary      = DarkTextSecondary,
    textMuted          = DarkTextMuted,
    sosSurface         = DarkSosSurface,
    onSosSurface       = DarkOnSosSurface,
    sosAccent          = DarkTerracotta,
    sosAccentContainer = Color(0xFF3D2621), // Estimated
)

val LocalTideletExtendedColors = staticCompositionLocalOf { LightExtended }

object TideletTheme {
    val extended: TideletExtendedColors
        @Composable get() = LocalTideletExtendedColors.current
}

@Composable
fun TideletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtended else LightExtended

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalTideletExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TideletTypography,
            content = content,
        )
    }
}
