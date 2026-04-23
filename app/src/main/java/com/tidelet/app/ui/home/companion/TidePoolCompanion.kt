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
 * Option A — Tide pool companion (PHASE_2 §9B1 draft).
 *
 * Top-down view of a shallow tide pool. Stage 1 is an empty pool with a
 * single slow ripple; by stage 5 the pool holds a pebble, seaweed,
 * starfish, and a small barnacle cluster. Everything is Canvas-drawn —
 * zero asset files.
 *
 * Shared design constraints from the spec:
 *   - Palette pulls from deep teal / warm coral / ice only.
 *   - No creature face anywhere; the starfish is a plain 5-point shape.
 *   - `reduceMotion = true` snaps the ripple/seaweed animations to their
 *     resting state.
 */
@Composable
fun TidePoolCompanion(
    stage: Int,
    isAdvancing: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.companion_description, stage, 5)

    // One continuous phase 0..2π feeds both the surface ripple and the
    // seaweed sway so motion reads as a single ambient rhythm rather than
    // competing loops. Skipped entirely when reduce-motion is on.
    val phase = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "tide_phase")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI.toFloat()),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "tide_phase_value",
        )
        v
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .semantics { contentDescription = description },
    ) {
        drawTidePool(stage = stage, phase = phase)
    }
}

private fun DrawScope.drawTidePool(stage: Int, phase: Float) {
    val ice = Color(0xFFF2EBDE)
    val rockFar = Color(0xFFC5B8A1)
    val rockNear = Color(0xFFA5987E)
    val water = Color(0xFF1C5E7A)
    val waterHighlight = Color(0xFF8FB5C6)
    val coral = Color(0xFFD97757)

    // Flat ice background so the pool reads against an empty beach.
    drawRect(ice)

    // The rocky shelf takes the bottom third — two soft curves suggest
    // the rim and a further ridge. Kept muted on purpose.
    val rimY = size.height * 0.72f
    drawRect(color = rockFar, topLeft = androidx.compose.ui.geometry.Offset(0f, rimY), size = androidx.compose.ui.geometry.Size(size.width, size.height - rimY))

    // The pool itself — horizontal oval in the middle-to-lower band.
    val poolLeft = size.width * 0.15f
    val poolRight = size.width * 0.85f
    val poolTop = size.height * 0.30f
    val poolBottom = size.height * 0.78f
    drawOval(
        color = water,
        topLeft = androidx.compose.ui.geometry.Offset(poolLeft, poolTop),
        size = androidx.compose.ui.geometry.Size(poolRight - poolLeft, poolBottom - poolTop),
    )

    // Stage 1 — a single traveling highlight suggests the ripple. At
    // higher stages we add a reflection arc on the surface.
    val rippleX = poolLeft + (poolRight - poolLeft) *
        (0.5f + 0.35f * sin(phase).toFloat())
    val rippleY = poolTop + (poolBottom - poolTop) * 0.25f
    drawCircle(
        color = waterHighlight.copy(alpha = 0.55f),
        radius = size.height * 0.08f,
        center = androidx.compose.ui.geometry.Offset(rippleX, rippleY),
    )

    if (stage >= 5) {
        // Clearer surface — a subtle full-width reflection band.
        drawRect(
            color = waterHighlight.copy(alpha = 0.18f),
            topLeft = androidx.compose.ui.geometry.Offset(poolLeft, poolTop),
            size = androidx.compose.ui.geometry.Size(poolRight - poolLeft, (poolBottom - poolTop) * 0.18f),
        )
    }

    // Stage 2: a pebble at the north-west rim.
    if (stage >= 2) {
        val pebbleCenter = androidx.compose.ui.geometry.Offset(
            poolLeft + (poolRight - poolLeft) * 0.18f,
            poolBottom - (poolBottom - poolTop) * 0.15f,
        )
        drawOval(
            color = rockNear,
            topLeft = androidx.compose.ui.geometry.Offset(pebbleCenter.x - size.height * 0.05f, pebbleCenter.y - size.height * 0.03f),
            size = androidx.compose.ui.geometry.Size(size.height * 0.10f, size.height * 0.06f),
        )
    }

    // Stage 3: a frond of seaweed swaying with the phase.
    if (stage >= 3) {
        val baseX = poolLeft + (poolRight - poolLeft) * 0.35f
        val baseY = poolBottom - (poolBottom - poolTop) * 0.10f
        val sway = sin(phase * 1.2f).toFloat() * size.height * 0.04f
        val path = Path().apply {
            moveTo(baseX, baseY)
            quadraticBezierTo(
                baseX + sway, baseY - size.height * 0.20f,
                baseX + sway * 0.8f, baseY - size.height * 0.42f,
            )
        }
        drawPath(
            path = path,
            color = Color(0xFF2E5F4E),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height * 0.025f),
        )
    }

    // Stage 4: a small starfish on the pool floor. Five triangles radiating
    // from a centre point — kept abstract, no face, no expression.
    if (stage >= 4) {
        val cx = poolLeft + (poolRight - poolLeft) * 0.62f
        val cy = poolTop + (poolBottom - poolTop) * 0.60f
        val armLen = size.height * 0.10f
        val path = Path()
        repeat(5) { i ->
            val angle = (-Math.PI / 2 + i * 2 * Math.PI / 5).toFloat()
            val tipX = cx + kotlin.math.cos(angle) * armLen
            val tipY = cy + kotlin.math.sin(angle) * armLen
            if (i == 0) path.moveTo(tipX, tipY) else path.lineTo(tipX, tipY)
            // Inner notch between arms — creates the star silhouette.
            val inner = angle + (Math.PI / 5).toFloat()
            path.lineTo(
                cx + kotlin.math.cos(inner) * armLen * 0.45f,
                cy + kotlin.math.sin(inner) * armLen * 0.45f,
            )
        }
        path.close()
        drawPath(path = path, color = coral.copy(alpha = 0.85f))
    }

    // Stage 5: a cluster of barnacles on the north rock.
    if (stage >= 5) {
        val clusterCx = poolLeft + (poolRight - poolLeft) * 0.85f
        val clusterCy = poolTop + (poolBottom - poolTop) * 0.25f
        val r = size.height * 0.025f
        listOf(
            0f to 0f,
            0.08f to -0.01f,
            0.04f to 0.05f,
            -0.06f to 0.03f,
        ).forEach { (dx, dy) ->
            drawCircle(
                color = rockNear,
                radius = r,
                center = androidx.compose.ui.geometry.Offset(
                    clusterCx + dx * size.height,
                    clusterCy + dy * size.height,
                ),
            )
        }
    }
}
