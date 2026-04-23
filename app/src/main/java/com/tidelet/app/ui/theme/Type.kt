package com.tidelet.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Typography scale (v0.1 refresh — see docs/DESIGN_SYSTEM.md §3).
 *
 * We ship with system `FontFamily.SansSerif` as the immediate primary to keep
 * the APK small and always-available. When Inter Variable is added at
 * `res/font/inter_variable.ttf`, swap `TideletSans` to a `FontFamily(Font(...))`
 * reference — the rest of this file does not need to change.
 *
 * Similarly, `TideletSerif` currently maps to `FontFamily.Serif`. When
 * Instrument Serif lands at `res/font/instrument_serif_regular.ttf`, swap it
 * for a dedicated FontFamily. Serif is used *only* for emotional accent
 * moments: the streak hero numeral on Home and letter-to-self reveals.
 *
 * Modular 1.25 scale: 12, 14, 16, 20, 24, 32, 40, 56, 72 sp.
 * Four weights only: 400 Regular, 500 Medium, 600 SemiBold.
 */

val TideletSans: FontFamily = FontFamily.SansSerif
val TideletSerif: FontFamily = FontFamily.Serif

val TideletTypography = Typography(
    // Display — used sparingly, at most once per screen.
    displayLarge = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = (-0.02f).em,
    ),
    displayMedium = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.02f).em,
    ),
    displaySmall = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.02f).em,
    ),

    // Headline — screen titles, major section headings.
    headlineLarge = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01f).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),

    // Title — card titles, top-bar titles, list headers.
    titleLarge = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),

    // Body — default running text.
    bodyLarge = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),

    // Label — buttons, chips, tabs.
    labelLarge = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01f.em,
    ),
    labelMedium = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02f.em,
    ),
    labelSmall = TextStyle(
        fontFamily = TideletSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04f.em,
    ),
)

