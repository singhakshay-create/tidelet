package com.tidelet.app.ui.sos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Box breathing: In (4s) → Hold (4s) → Out (4s) → Hold (4s), repeated.
 *
 * The visual is a single expanding/contracting circle with the current phase label
 * inside it. The circle grows during "In", stays large during "Hold", shrinks during
 * "Out", stays small during the second "Hold" — so the eye learns the rhythm.
 *
 * One cycle = all four phases. We show a running cycle count; tapping Stop logs a
 * CravingEvent (if any cycle was completed) and pops back.
 */
private enum class BreathPhase(val labelRes: Int, val durationMillis: Int) {
    In(R.string.breathe_in, 4_000),
    Hold1(R.string.breathe_hold, 4_000),
    Out(R.string.breathe_out, 4_000),
    Hold2(R.string.breathe_hold, 4_000),
}

@Composable
fun BreatheScreen(
    onDone: () -> Unit,
    intensity: Int? = null,
    vm: BreatheViewModel = viewModel(),
) {
    val warmSurface = TideletTheme.extended.sosSurface
    val onWarm = TideletTheme.extended.onSosSurface
    val accent = TideletTheme.extended.sosAccent

    // Phase state drives both the label and the target circle size.
    var phase by remember { mutableStateOf(BreathPhase.In) }
    var cycles by remember { mutableIntStateOf(0) }

    // Run the phase loop. A single LaunchedEffect keeps the cadence honest — each
    // delay matches the animation duration so the text flip and the size change stay
    // in sync. Cancellation happens automatically when the composable leaves the tree.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(phase.durationMillis.toLong())
            phase = when (phase) {
                BreathPhase.In -> BreathPhase.Hold1
                BreathPhase.Hold1 -> BreathPhase.Out
                BreathPhase.Out -> BreathPhase.Hold2
                BreathPhase.Hold2 -> {
                    cycles += 1
                    BreathPhase.In
                }
            }
        }
    }

    // Scale is driven by an Animatable (not animateFloatAsState) so we can start at
    // 0.4 (empty-lung size) on first composition. animateFloatAsState snaps to its
    // first target without animating, which caused the circle to sit static at full
    // size during the first In + Hold1 phases. With an Animatable we get a real
    // animation on the very first "In" breath.
    val scaleAnim = remember { Animatable(0.4f) }
    LaunchedEffect(phase) {
        val target = if (phase == BreathPhase.In || phase == BreathPhase.Hold1) 1f else 0.4f
        val duration = when (phase) {
            BreathPhase.In, BreathPhase.Out -> phase.durationMillis
            BreathPhase.Hold1, BreathPhase.Hold2 -> 0
        }
        scaleAnim.animateTo(target, tween(duration, easing = LinearEasing))
    }
    val scale = scaleAnim.value

    // If the user navigates away without pressing Stop (e.g. system back), we still
    // want to log a completion if they did at least one cycle. DisposableEffect runs
    // on composable exit.
    DisposableEffect(Unit) {
        onDispose {
            if (cycles > 0) vm.logCompleted(intensity)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(warmSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.breathe_title),
                style = MaterialTheme.typography.headlineSmall,
                color = onWarm,
            )

            // Animated circle with the phase label inside.
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size((260 * scale).dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(phase.labelRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("breathe_phase_label"),
                    )
                }
            }

            Text(
                text = stringResource(R.string.breathe_cycles, cycles),
                style = MaterialTheme.typography.titleMedium,
                color = onWarm,
                modifier = Modifier.testTag("breathe_cycle_count"),
            )

            OutlinedButton(
                onClick = {
                    // onDispose will log if cycles > 0; just navigate away.
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.breathe_stop))
            }
        }
    }
}
