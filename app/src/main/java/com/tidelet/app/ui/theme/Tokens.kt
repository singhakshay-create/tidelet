package com.tidelet.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Non-colour design tokens — spacing, shape, motion. See docs/DESIGN_SYSTEM.md.
 *
 * Use these instead of hardcoded `.dp` values and `RoundedCornerShape(n.dp)`
 * literals scattered through composables. When we need to change every card
 * radius or every page-gutter, it should be one file.
 */

// ---- Spacing — strict 8-point grid ----

object Spacing {
    val s1 = 4.dp   // tight inline
    val s2 = 8.dp   // chip gaps, icon-to-label
    val s3 = 12.dp  // dense list rows
    val s4 = 16.dp  // default gap between related blocks
    val s6 = 24.dp  // horizontal page padding (everywhere)
    val s8 = 32.dp  // between major sections
    val s12 = 48.dp // hero top breathing
    val s16 = 64.dp // extreme whitespace (onboarding, landing)
}

/** Minimum touch target per WCAG 2.5.5. */
val MinTouchTarget = 48.dp

// ---- Shape — one scale, four values ----

object Shapes {
    val sm: Shape = RoundedCornerShape(8.dp)    // inputs, chips, small buttons
    val md: Shape = RoundedCornerShape(16.dp)   // cards, tiles, standard buttons
    val lg: Shape = RoundedCornerShape(24.dp)   // bottom sheets, dialogs
    val topLg: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
    val full: Shape = RoundedCornerShape(1000.dp) // pills
}

// ---- Motion ----

object Motion {
    const val Micro = 120
    const val Standard = 240
    const val Page = 400
    const val Emotion = 600

    val Standard_Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val Emphatic_Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val Exit_Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}
