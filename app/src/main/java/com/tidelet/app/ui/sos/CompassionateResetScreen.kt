package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Compassionate reset screen.
 *
 * Shown when the user answers "I drank" at the end of Ride the Wave. Core
 * framing (see Reframe_Benchmarking.md, Tier 1):
 *
 *   - validate the user (slips are data, not failure)
 *   - no broken-streak animation, badge loss, or penalty UI
 *   - gently invite them into today's check-in so the moment is logged with
 *     context they can look back on later
 *
 * CBT-tier addition (Phase 3, "in-line functional analysis"): between the
 * validating copy and the CTAs we offer a collapsed "want to map what
 * happened?" card. It's optional — skip is the explicit path back to the
 * CTAs — and the panel quietly dismisses itself once the user engages with
 * it (save OR skip). The analysis is saved separately from the check-in
 * since the two serve different purposes: the check-in is "today at a
 * glance," the analysis is "what happened around this specific slip."
 */
@Composable
fun CompassionateResetScreen(
    onLogToday: () -> Unit,
    onLater: () -> Unit,
    vm: CompassionateResetViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val surface = TideletTheme.extended.sosSurface
    val onSurface = TideletTheme.extended.onSosSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.reset_title),
                style = MaterialTheme.typography.headlineMedium,
                color = onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.reset_body_1),
                style = MaterialTheme.typography.bodyLarge,
                color = onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.reset_body_2),
                style = MaterialTheme.typography.bodyLarge,
                color = onSurface,
                textAlign = TextAlign.Center,
            )

            // --- In-line functional analysis (optional) ---
            // The panel disappears once the user engages (save or skip),
            // so the screen collapses back to the original two-CTA state.
            val showPanel = !state.analysisSaved && !state.analysisDismissed
            if (showPanel) {
                Spacer(Modifier.height(24.dp))
                FunctionalAnalysisPanel(
                    titleRes = R.string.reset_map_title,
                    onSave = vm::saveAnalysis,
                    onSkip = vm::dismissAnalysis,
                )
            } else if (state.analysisSaved) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.reset_map_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onLogToday,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) { Text(stringResource(R.string.reset_log_today)) }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.reset_not_now)) }
        }
    }
}
