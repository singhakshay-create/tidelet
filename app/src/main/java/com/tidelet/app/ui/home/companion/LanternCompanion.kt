package com.tidelet.app.ui.home.companion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tidelet.app.R
import kotlin.math.sin

/**
 * Option B — Lantern companion (PHASE_2 §9B1 draft).
 *
 * A single flame over a centred glow. As stages advance the glow radius
 * grows, the flame warms from cool teal to warm coral, and stage 5 adds
 * a slow 4 s breath-pulse on the outer glow.
 *
 * Deliberate minimalism: no holder, no base, no "object" around the
 * flame. The flame + glow is the entire motif. Resist the urge to add
 * chrome here on review — the abstraction is the whole point.
 */
@Composable
fun LanternCompanion(
    stage: Int,
    isAdvancing: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.companion_description, stage, 5)

    // Two independent motions: a low-amplitude flicker for the flame
    // shape and, at stage 5 only, a slow glow pulse. Both collapse to
    // their resting values when reduce-motion is on.
    val flicker: Float
    val pulse: Float
    if (reduceMotion) {
        flicker = 0f
        pulse = 0f
    } else {
        val transition = rememberInfiniteTransition(label = "lantern_companion")
        val flickerAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI.toFloat()),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "lantern_flicker",
        )
        val pulseAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI.toFloat()),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "lantern_pulse",
        )
        flicker = flickerAnim
        pulse = pulseAnim
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = description },
    ) {
        drawLantern(
            stage = stage,
            flickerPhase = flicker,
            pulsePhase = pulse,
        )
    }
}

/**
 * Stage-indexed "warmth" blend. Stage 1 is cool teal; stage 5 is full
 * warm coral. Intermediate stages cross-fade between the two brand
 * colours so the warmth grows with abstinence rather than appearing all
 * at once.
 */
private fun flameColor(stage: Int): Color {
    val cool = Color(0xFF1C5E7A)   // deep teal
    val warm = Color(0xFFD97757)   // warm coral
    val t = ((stage - 1) / 4f).coerceIn(0f, 1f)
    return lerpColor(cool, warm, t)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

private fun DrawScope.drawLantern(
    stage: Int,
    flickerPhase: Float,
    pulsePhase: Float,
) {
    val ice = Color(0xFFF2EBDE)
    drawRect(ice)

    val cx = size.width / 2f
    val cy = size.height / 2f + size.height * 0.05f  // slightly below centre so the glow has headroom

    // ---- Glow (drawn first so the flame sits on top) ----
    // Base radius grows with stage: stage 1 ~0.35× height, stage 5 ~0.62×.
    val baseRadius = size.height * (0.30f + 0.08f * stage)
    // Stage 5 only: breathe ±6% on a 4s cycle.
    val breathing = if (stage >= 5) 1f + 0.06f * sin(pulsePhase).toFloat() else 1f
    val radius = baseRadius * breathing

    val glow = flameColor(stage)
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            glow.copy(alpha = 0.45f),
            glow.copy(alpha = 0.18f),
            glow.copy(alpha = 0f),
        ),
        center = Offset(cx, cy),
        radius = radius,
    )
    drawCircle(brush = glowBrush, radius = radius, center = Offset(cx, cy))

    // Stage 4 adds a soft outer ring — a faint second gradient at ~1.2×.
    if (stage >= 4) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    glow.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = radius * 1.25f,
            ),
            radius = radius * 1.25f,
            center = Offset(cx, cy),
        )
    }

    // ---- Flame ----
    // Shaped like a teardrop: wider base, narrow top, slightly waggled
    // by flickerPhase. Shrinks a hair when reduce-motion is on because
    // flickerPhase collapses to zero.
    val flameWidth = size.height * 0.11f
    val flameHeight = size.height * (0.22f + 0.04f * stage)
    val wobble = sin(flickerPhase).toFloat() * size.height * 0.012f
    val baseY = cy + size.height * 0.02f
    val tipY = baseY - flameHeight
    val flamePath = Path().apply {
        moveTo(cx, baseY)
        // Left side — base → curve up to the tip.
        quadraticBezierTo(
            cx - flameWidth - wobble, baseY - flameHeight * 0.55f,
            cx + wobble * 0.4f, tipY,
        )
        // Right side — tip → back down to the base.
        quadraticBezierTo(
            cx + flameWidth - wobble, baseY - flameHeight * 0.55f,
            cx, baseY,
        )
        close()
    }
    drawPath(path = flamePath, color = flameColor(stage).copy(alpha = 0.95f))

    // A brighter inner wick — small vertical blade that gives the flame
    // some depth without pushing into "cartoon" territory.
    val wickPath = Path().apply {
        moveTo(cx, baseY - flameHeight * 0.10f)
        quadraticBezierTo(
            cx - flameWidth * 0.35f, baseY - flameHeight * 0.45f,
            cx, tipY + flameHeight * 0.20f,
        )
        quadraticBezierTo(
            cx + flameWidth * 0.35f, baseY - flameHeight * 0.45f,
            cx, baseY - flameHeight * 0.10f,
        )
        close()
    }
    drawPath(
        path = wickPath,
        color = Color(0xFFFFF0DA).copy(alpha = 0.85f),
    )
}
