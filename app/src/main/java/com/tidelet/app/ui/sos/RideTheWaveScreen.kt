package com.tidelet.app.ui.sos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme
import kotlin.math.sin

/**
 * The urge-surfing screen.
 *
 * What this screen does, top to bottom:
 *   - a slow sinusoidal wave animates across the middle
 *   - a 15-minute countdown ticks down
 *   - a chip row lets the user pick an ambient soundscape (Silent / Ocean / River / Rain)
 *   - a rotating line of text softens every ~30 seconds
 *   - "End early" closes the timer and jumps to the outcome question
 *   - once the timer ends (or the user ends early), we ask "how did that go?"
 *     and log a CravingEvent based on their answer
 */
@Composable
fun RideTheWaveScreen(
    onDone: () -> Unit,
    onDrankFlow: () -> Unit = onDone,
    intensity: Int? = null,
    vm: RideTheWaveViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val warmSurface = TideletTheme.extended.sosSurface
    val onWarm = TideletTheme.extended.onSosSurface

    Box(
        modifier = Modifier.fillMaxSize().background(warmSurface),
    ) {
        when (state.phase) {
            WavePhase.Running -> WaveRunning(
                remainingSeconds = state.remainingSeconds,
                totalSeconds = state.totalSeconds,
                selectedSoundscape = state.soundscape,
                onSelectSoundscape = vm::selectSoundscape,
                onEndEarly = vm::endEarly,
                accent = TideletTheme.extended.sosAccent,
                onBackground = onWarm,
            )
            WavePhase.Done -> {
                if (state.showAnalysisPanel) {
                    // Non-DRANK outcome just landed. Offer the in-line
                    // analysis before leaving the wave flow. The VM has
                    // already logged the craving event — this panel
                    // optionally adds a FunctionalAnalysis linked to it.
                    WaveAnalysisStep(
                        onSave = { a, t, f ->
                            vm.saveAnalysis(a, t, f)
                            onDone()
                        },
                        onSkip = {
                            vm.dismissAnalysis()
                            onDone()
                        },
                    )
                } else {
                    WaveDone(
                        onGotThrough = {
                            vm.logGotThrough(intensity)
                        },
                        onStillStruggling = {
                            vm.logStillStruggling(intensity)
                        },
                        onDrank = {
                            vm.logDrank(intensity)
                            onDrankFlow()
                        },
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WaveAnalysisStep(
    onSave: (antecedent: String?, thought: String?, followingAction: String?) -> Unit,
    onSkip: () -> Unit,
) {
    val onWarm = TideletTheme.extended.onSosSurface
    val scrollState = androidx.compose.foundation.rememberScrollState()
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .androidxCompatVerticalScroll(scrollState)
            .imePadding()
            .padding(24.dp)
            .testTag("wave_analysis_panel"),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.wave_analysis_intro_title),
            style = MaterialTheme.typography.titleLarge,
            color = onWarm,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wave_analysis_intro_body),
            style = MaterialTheme.typography.bodyMedium,
            color = onWarm,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        FunctionalAnalysisPanel(
            titleRes = R.string.wave_analysis_title,
            onSave = onSave,
            onSkip = onSkip,
        )
    }
}

/**
 * Local alias so we can reuse [androidx.compose.foundation.verticalScroll] without
 * muddling existing imports at the top of the file. Imported inline to keep the
 * analysis-step addition self-contained.
 */
private fun Modifier.androidxCompatVerticalScroll(
    scrollState: androidx.compose.foundation.ScrollState
): Modifier =
    this.then(
        Modifier.verticalScroll(
            scrollState,
        ),
    )

// ---- Running ----

@Composable
private fun WaveRunning(
    remainingSeconds: Int,
    totalSeconds: Int,
    selectedSoundscape: Soundscape,
    onSelectSoundscape: (Soundscape) -> Unit,
    onEndEarly: () -> Unit,
    accent: Color,
    onBackground: Color,
) {
    // Rotate the calming phrase every 30s.
    val lines = listOf(
        R.string.wave_line_1,
        R.string.wave_line_2,
        R.string.wave_line_3,
        R.string.wave_line_4,
        R.string.wave_line_5,
    )
    var lineIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            lineIndex = (lineIndex + 1) % lines.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // Countdown
        Text(
            text = formatClock(remainingSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = onBackground,
            modifier = Modifier.testTag("wave_timer_text"),
        )

        Text(
            text = stringResource(R.string.wave_title),
            style = MaterialTheme.typography.titleLarge,
            color = onBackground,
        )

        // Wave
        WaveCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            color = accent,
        )

        // Soundscape picker — horizontally scrollable in case the list grows.
        SoundscapeRow(
            selected = selectedSoundscape,
            onSelect = onSelectSoundscape,
            accent = accent,
        )

        Text(
            text = stringResource(lines[lineIndex]),
            style = MaterialTheme.typography.titleMedium,
            color = onBackground,
            textAlign = TextAlign.Center,
        )

        OutlinedButton(
            onClick = onEndEarly,
            modifier = Modifier.fillMaxWidth().testTag("wave_end_early"),
        ) { Text(stringResource(R.string.wave_end_early)) }
    }
}

@Composable
private fun SoundscapeRow(
    selected: Soundscape,
    onSelect: (Soundscape) -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Soundscape.entries.forEach { s ->
            FilterChip(
                selected = s == selected,
                onClick = { onSelect(s) },
                label = { Text(stringResource(s.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.25f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun WaveCanvas(modifier: Modifier, color: Color) {
    // One shared infinite transition drives the horizontal offset of the wave.
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-phase",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val amplitude = h / 4f
        // Draw 3 nested waves with decreasing opacity for a layered, calming feel.
        repeat(3) { i ->
            val alpha = 1f - i * 0.3f
            val localPhase = phase + i * 0.6f
            val path = Path()
            val step = 4f
            var x = 0f
            while (x <= w) {
                val y = centerY + amplitude * sin((x / w) * 4f * Math.PI.toFloat() + localPhase)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += step
            }
            drawPath(
                path = path,
                color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                style = Stroke(width = 6f),
            )
        }

        // Subtle moving highlight dot to give the eye something to follow
        val dotX = w / 2f + (w / 4f) * sin(phase)
        val dotY = centerY + amplitude * sin((dotX / w) * 4f * Math.PI.toFloat() + phase)
        drawCircle(color = color, radius = 10f, center = Offset(dotX, dotY))
    }
}

// ---- Done / outcome ----

@Composable
private fun WaveDone(
    onGotThrough: () -> Unit,
    onStillStruggling: () -> Unit,
    onDrank: () -> Unit,
) {
    val onWarm = TideletTheme.extended.onSosSurface

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.wave_done),
            style = MaterialTheme.typography.headlineMedium,
            color = onWarm,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.wave_done_body),
            style = MaterialTheme.typography.bodyLarge,
            color = onWarm,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.wave_how_did_it_go),
            style = MaterialTheme.typography.titleMedium,
            color = onWarm,
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onGotThrough,
            modifier = Modifier.fillMaxWidth().testTag("wave_got_through"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) { Text(stringResource(R.string.wave_got_through)) }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onStillStruggling,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.wave_still_struggling)) }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onDrank,
            modifier = Modifier.fillMaxWidth().testTag("wave_drank"),
        ) { Text(stringResource(R.string.wave_i_drank)) }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
