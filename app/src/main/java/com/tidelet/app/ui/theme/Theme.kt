package com.tidelet.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tidelet's Material 3 theme wrapper (v0.1 refresh — see docs/DESIGN_SYSTEM.md).
 *
 * Usage: wrap your composable tree in `TideletTheme { ... }` at the top of
 * MainActivity. Everything inside reads these colours and typography.
 *
 * `TideletExtendedColors` holds app-specific tokens that don't fit Material 3's
 * built-in slots — notably the warm SOS palette and subtle borders.
 */

private val LightColors = lightColorScheme(
    primary              = SageDeep,
    onPrimary            = OnPrimary,
    primaryContainer     = SageSoft,
    onPrimaryContainer   = SageInk,

    secondary            = SageDeep,
    onSecondary          = OnPrimary,

    background           = WarmIvory,
    onBackground         = Ink,

    surface              = SurfaceWhite,
    onSurface            = Ink,
    surfaceVariant       = SurfaceSunk,
    onSurfaceVariant     = TextMuted,

    error                = DeepError,
    onError              = OnPrimary,
    errorContainer       = WarmSand,
    onErrorContainer     = TerraInk,

    outline              = BorderEmph,
    outlineVariant       = BorderSubtle,
)

private val DarkColors = darkColorScheme(
    primary              = PaleSage,
    onPrimary            = WarmBlack,
    primaryContainer     = DarkPrimaryCtnr,
    onPrimaryContainer   = OnDarkPrimaryCt,

    secondary            = PaleSage,
    onSecondary          = WarmBlack,

    background           = WarmBlack,
    onBackground         = DarkInk,

    surface              = DarkSurface,
    onSurface            = DarkInk,
    surfaceVariant       = DarkSurfaceRais,
    onSurfaceVariant     = DarkTextMuted,

    error                = ErrorDark,
    onError              = WarmBlack,
    errorContainer       = WarmSandDark,
    onErrorContainer     = OnWarmSandDark,

    outline              = DarkBorder,
    outlineVariant       = DarkBorder,
)

/**
 * App-specific colour tokens that don't map cleanly onto Material 3 slots.
 * Refreshed palette — terracotta replaces coral; text-secondary and
 * border-subtle promoted to first-class tokens.
 */
data class TideletExtendedColors(
    val sosAccent: Color,
    val sosSurface: Color,
    val onSosSurface: Color,
    val sosAccentContainer: Color,
    val success: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val borderEmphasis: Color,
)

private val LightExtended = TideletExtendedColors(
    sosAccent          = Terracotta,
    sosSurface         = WarmSand,
    onSosSurface       = TerraInk,
    sosAccentContainer = TerraSoft,
    success            = SageSuccess,
    textSecondary      = TextSecond,
    textMuted          = TextMuted,
    borderSubtle       = BorderSubtle,
    borderEmphasis     = BorderEmph,
)

private val DarkExtended = TideletExtendedColors(
    sosAccent          = TerracottaDark,
    sosSurface         = WarmSandDark,
    onSosSurface       = OnWarmSandDark,
    sosAccentContainer = WarmSandDark,
    success            = SageSuccessDark,
    textSecondary      = DarkTextSecond,
    textMuted          = DarkTextMuted,
    borderSubtle       = DarkBorder,
    borderEmphasis     = DarkBorder,
)

val LocalTideletExtendedColors = staticCompositionLocalOf { LightExtended }

object TideletTheme {
    val extended: TideletExtendedColors
        @Composable get() = LocalTideletExtendedColors.current
}

@Composable
fun TideletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // On Android 12+ we can opt into the OS dynamic palette. Kept off in v1
    // so the SOS terracotta is always the chosen colour, not wallpaper-derived.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val extended = if (darkTheme) DarkExtended else LightExtended

    // System status bar picks up our background colour instead of the default.
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
