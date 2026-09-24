package com.tidelet.app.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Optional evening mini-review (PHASE_2 §9A5). Two short free-text fields
 * — one win, one challenge — saved as a single row keyed on today's date.
 *
 * Visual style mirrors the Thought-Check write screen: a vertical Column,
 * generous spacing, no card decoration. The form is meant to feel like a
 * journal page, not a survey.
 */
@Composable
fun EveningReviewScreen(
    onDone: () -> Unit,
    vm: EveningReviewViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // When the VM flips `saved` true after a successful upsert, hand control
    // back to whoever pushed us onto the back stack (typically Home).
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Text(
            text = stringResource(R.string.evening_review_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.evening_review_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )

        Spacer(Modifier.height(Spacing.s2))

        OutlinedTextField(
            value = state.winText,
            onValueChange = vm::setWin,
            label = { Text(stringResource(R.string.evening_review_win_label)) },
            placeholder = { Text(stringResource(R.string.evening_review_win_placeholder)) },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evening_review_win"),
        )

        OutlinedTextField(
            value = state.challengeText,
            onValueChange = vm::setChallenge,
            label = { Text(stringResource(R.string.evening_review_challenge_label)) },
            placeholder = { Text(stringResource(R.string.evening_review_challenge_placeholder)) },
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evening_review_challenge"),
        )

        Spacer(Modifier.height(Spacing.s2))

        Button(
            onClick = vm::save,
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evening_review_save"),
        ) {
            Text(stringResource(R.string.evening_review_save))
        }

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.evening_review_skip))
        }
    }
}
