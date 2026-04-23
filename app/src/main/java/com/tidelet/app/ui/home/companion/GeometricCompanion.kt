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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tidelet.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Option C — Geometric pattern companion (PHASE_2 §9B1 draft).
 *
 * A concentric dot pattern that grows outward with the stage. Day 0 is a
 * single centre dot; stage 5 is four rings (6 + 12 + 18 + 24 dots)
 * around a coral centre, with a faint connecting web and a very slow
 * full rotation (one revolution per 60 seconds).
 *
 * Pure abstraction by design — closer to data-viz than to a creature.
 * If that distance is wanted (see spec's decision criteria), this is
 * the option.
 */
@Composable
fun GeometricCompanion(
    stage: Int,
    isAdvancing: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.companion_description, stage, 5)

    // One slow rotation, only active at stage 5 and only when reduce-
    // motion is off. One revolution per 60 seconds keeps the motion
    // ambient rather than attention-grabbing.
    val rotation = if (reduceMotion || stage < 5) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "geom_rotation")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI.toFloat()),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 60_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "geom_rotation_value",
        )
        v
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = description },
    ) {
        drawPattern(stage = stage, rotation = rotation)
    }
}

/**
 * Ring definitions — count of dots per ring + radius as a fraction of
 * available height. Ring i (0-indexed) lives at `ringRadii[i]`.
 */
private val ringDots = listOf(6, 12, 18, 24)
private val ringRadii = listOf(0.18f, 0.30f, 0.40f, 0.48f)

private fun DrawScope.drawPattern(stage: Int, rotation: Float) {
    drawRect(Color(0xFFF2EBDE))  // ice background — matches other options

    val cx = size.width / 2f
    val cy = size.height / 2f
    val ringBase = size.height  // radii are fractions of the canvas height

    val teal = Color(0xFF1C5E7A)
    val coral = Color(0xFFD97757)
    // Centre dot colour: stage ≥ 3 swaps to coral per spec.
    val centreColor = if (stage >= 3) coral else teal

    // Number of rings to show = stage - 1 (stage 1 = centre only).
    val ringsToShow = (stage - 1).coerceIn(0, ringDots.size)

    // Stage 5: a faint connecting web — lines from centre to each outer
    // ring dot, very low alpha so it reads as texture, not structure.
    if (stage >= 5 && ringsToShow >= 1) {
        val outer = ringsToShow - 1
        val outerRadius = ringRadii[outer] * ringBase
        val count = ringDots[outer]
        repeat(count) { i ->
            val angle = rotation + (i * 2 * Math.PI / count).toFloat()
            drawLine(
                color = teal.copy(alpha = 0.10f),
                start = Offset(cx, cy),
                end = Offset(
                    cx + cos(angle) * outerRadius,
                    cy + sin(angle) * outerRadius,
                ),
                strokeWidth = 0.6f,
            )
        }
    }

    // Rings — outer to inner, so any overlap has the earlier (larger)
    // ring sit underneath.
    repeat(ringsToShow) { ringIndex ->
        val count = ringDots[ringIndex]
        val radius = ringRadii[ringIndex] * ringBase
        // Saturation grows slightly with ring index — subtle gradient from
        // inner rings (full teal) to outer rings (a hint of coral).
        val ringColor = lerpColor(teal, coral, (ringIndex / 4f) * 0.45f)
        repeat(count) { i ->
            val angle = rotation + (i * 2 * Math.PI / count).toFloat()
            drawCircle(
                color = ringColor,
                radius = dotRadius(ringIndex),
                center = Offset(
                    cx + cos(angle) * radius,
                    cy + sin(angle) * radius,
                ),
            )
        }
    }

    // Centre dot — drawn last so it sits above any web lines.
    drawCircle(
        color = centreColor,
        radius = dotRadius(-1),
        center = Offset(cx, cy),
    )
}

/** Inner rings get a slightly larger dot than outer ones — a gentle weight gradient. */
private fun DrawScope.dotRadius(ringIndex: Int): Float =
    when (ringIndex) {
        -1 -> size.height * 0.045f  // centre
        0 -> size.height * 0.030f
        1 -> size.height * 0.026f
        2 -> size.height * 0.023f
        else -> size.height * 0.020f
    }

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
