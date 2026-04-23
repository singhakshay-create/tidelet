package com.tidelet.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tidelet's colour palette (v0.1 refresh — see docs/DESIGN_SYSTEM.md).
 *
 * Two semantic families share a warm-neutral foundation:
 *   - **Calm** — deep sage primary on warm ivory. The default everywhere.
 *   - **Warm** — terracotta accent on warm sand. SOS flow only.
 *
 * Rule: never reference these Color values directly from UI code. Read them
 * off `MaterialTheme.colorScheme` (for primary/surface/etc.) or
 * `TideletTheme.extended` (for sos-specific tokens) so a single change in
 * Theme.kt re-skins the whole app.
 *
 * Contrast ratios are documented in DESIGN_SYSTEM.md §2; every body-text
 * pairing lands ≥ 4.5:1 (WCAG AA).
 */

// ---- Light mode ----

// Calm (default)
val SageDeep   = Color(0xFF2E3D35)   // primary
val SageSoft   = Color(0xFFE6EBE5)   // primary-container
val SageInk    = Color(0xFF1A221D)   // on-primary-container

// Neutrals (warm)
val WarmIvory    = Color(0xFFFBF9F5) // background
val SurfaceWhite = Color(0xFFFFFFFF) // surface
val SurfaceSunk  = Color(0xFFF5F2EC) // surface-sunken
val BorderSubtle = Color(0xFFE8E4DB) // 1dp separators, card outlines
val BorderEmph   = Color(0xFFD4CEC1) // focused input borders
val Ink          = Color(0xFF1A1C1A) // on-surface, primary body text
val TextSecond   = Color(0xFF404340) // secondary body
val TextMuted    = Color(0xFF6B6F6B) // tertiary / captions
val OnPrimary    = Color(0xFFF5F3EE) // text on sage buttons

// Warm (SOS only)
val Terracotta   = Color(0xFFC26B5A) // sos accent
val WarmSand     = Color(0xFFFAE8E2) // sos surface
val TerraInk     = Color(0xFF7A342B) // on-sos-surface
val TerraSoft    = Color(0xFFF0D2C7) // sos accent container

// Status
val SageSuccess  = Color(0xFF5A8560)
val DeepError    = Color(0xFFA83E34)

// ---- Dark mode ----

// Calm
val PaleSage        = Color(0xFFA3BEB0) // primary (dark)
val DarkPrimaryCtnr = Color(0xFF263028)
val OnDarkPrimaryCt = Color(0xFFD5E2D8)

// Neutrals
val WarmBlack       = Color(0xFF0F1110) // background (dark)
val DarkSurface     = Color(0xFF181A19)
val DarkSurfaceRais = Color(0xFF22241F)
val DarkBorder      = Color(0xFF2A2D2A)
val DarkInk         = Color(0xFFECECEA) // never pure white
val DarkTextSecond  = Color(0xFFB8BAB6)
val DarkTextMuted   = Color(0xFF8C908B)

// Warm (SOS)
val TerracottaDark  = Color(0xFFD89482)
val WarmSandDark    = Color(0xFF2A1A16)
val OnWarmSandDark  = Color(0xFFF0D2C7)

// Status
val SageSuccessDark = Color(0xFF8CBA92)
val ErrorDark       = Color(0xFFE59083)
