package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.cbt.COGNITIVE_DISTORTIONS
import com.tidelet.app.ui.theme.TideletTheme

/**
 * The Thought Check screen — a four-prompt micro CBT thought-record.
 *
 * Prompts, in order:
 *   1. What's happening right now? (situation)
 *   2. What's the thought? (automatic thought)
 *   3. Is that 100% true? (challenge)
 *   4. What would you tell a friend? (compassion reframe)
 *
 * Plus an optional distortion-tag chip row so the entry can later be
 * aggregated on the Stats tab ("your most common pattern: permission-giving
 * thoughts"). The "pattern" chip row is presented after prompt 2 because
 * naming the distortion is easiest once the thought is in front of you.
 *
 * A "See common patterns" link launches the reference library without
 * losing the in-flight state (VM survives navigation).
 */
@Composable
fun ThoughtCheckScreen(
    onDone: () -> Unit,
    onOpenDistortions: () -> Unit,
    intensity: Int? = null,
    vm: ThoughtCheckViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val surface = TideletTheme.extended.sosSurface
    val onSurface = TideletTheme.extended.onSosSurface

    Box(
        modifier = Modifier.fillMaxSize().background(surface),
    ) {
        if (state.saved) {
            ThoughtCheckSaved(onClose = onDone, onSurface = onSurface)
        } else {
            ThoughtCheckEditor(
                state = state,
                onSituation = vm::setSituation,
                onThought = vm::setThought,
                onChallenge = vm::setChallenge,
                onFriendReframe = vm::setFriendReframe,
                onToggleDistortion = vm::toggleDistortion,
                onOpenDistortions = onOpenDistortions,
                onSave = { vm.save(intensity) },
                onSkip = {
                    vm.skip(intensity)
                    onDone()
                },
                onSurface = onSurface,
            )
        }
    }
}

@Composable
private fun ThoughtCheckEditor(
    state: ThoughtCheckUiState,
    onSituation: (String) -> Unit,
    onThought: (String) -> Unit,
    onChallenge: (String) -> Unit,
    onFriendReframe: (String) -> Unit,
    onToggleDistortion: (String) -> Unit,
    onOpenDistortions: () -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    onSurface: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.thought_check_title),
            style = MaterialTheme.typography.headlineSmall,
            color = onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.thought_check_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        // ---- 1. Situation ----
        PromptLabel(R.string.thought_check_q1, onSurface)
        OutlinedTextField(
            value = state.situation,
            onValueChange = onSituation,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text(stringResource(R.string.thought_check_q1_hint)) },
        )

        Spacer(Modifier.height(16.dp))

        // ---- 2. Thought ----
        PromptLabel(R.string.thought_check_q2, onSurface)
        OutlinedTextField(
            value = state.thought,
            onValueChange = onThought,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text(stringResource(R.string.thought_check_q2_hint)) },
        )

        Spacer(Modifier.height(12.dp))

        // ---- Distortion chip row (placed here so naming the pattern is
        //      easy once the thought is on the page) ----
        Text(
            text = stringResource(R.string.thought_check_pattern_prompt),
            style = MaterialTheme.typography.labelLarge,
            color = onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            COGNITIVE_DISTORTIONS.forEach { d ->
                FilterChip(
                    selected = state.distortionKey == d.key,
                    onClick = { onToggleDistortion(d.key) },
                    label = { Text(stringResource(d.labelRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TideletTheme.extended.sosAccent.copy(alpha = 0.25f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onOpenDistortions) {
            Text(stringResource(R.string.thought_check_see_patterns))
        }

        Spacer(Modifier.height(12.dp))

        // ---- 3. Challenge ----
        PromptLabel(R.string.thought_check_q3, onSurface)
        OutlinedTextField(
            value = state.challenge,
            onValueChange = onChallenge,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text(stringResource(R.string.thought_check_q3_hint)) },
        )

        Spacer(Modifier.height(16.dp))

        // ---- 4. Friend reframe ----
        PromptLabel(R.string.thought_check_q4, onSurface)
        OutlinedTextField(
            value = state.friendReframe,
            onValueChange = onFriendReframe,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            placeholder = { Text(stringResource(R.string.thought_check_q4_hint)) },
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = state.thought.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) { Text(stringResource(R.string.thought_check_save)) }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.thought_check_skip))
        }
    }
}

@Composable
private fun PromptLabel(resId: Int, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ThoughtCheckSaved(
    onClose: () -> Unit,
    onSurface: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.thought_check_saved_title),
            style = MaterialTheme.typography.headlineMedium,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.thought_check_saved_body),
            style = MaterialTheme.typography.bodyLarge,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.thought_check_back)) }
    }
}
