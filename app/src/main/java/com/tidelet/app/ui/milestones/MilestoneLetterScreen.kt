package com.tidelet.app.ui.milestones

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme
import com.tidelet.app.util.milestoneLabel

/**
 * Write a short note to your future self at a milestone. On save, the reveal
 * will surface on Home when the user hits the next milestone (see
 * [com.tidelet.app.ui.home.HomeViewModel]).
 */
@Composable
fun MilestoneLetterScreen(
    onDone: () -> Unit,
    vm: MilestoneLetterViewModel = viewModel(factory = MilestoneLetterViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.s6, vertical = Spacing.s8),
    ) {
        Text(
            text = stringResource(R.string.milestone_letter_title, milestoneLabel(state.milestoneDays)),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.milestone_letter_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TideletTheme.extended.textSecondary,
        )

        Spacer(Modifier.height(Spacing.s6))

        OutlinedTextField(
            value = state.text,
            onValueChange = vm::setText,
            placeholder = {
                Text(
                    text = stringResource(R.string.milestone_letter_placeholder),
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
            maxLines = 20,
        )

        Spacer(Modifier.height(Spacing.s8))

        Button(
            onClick = vm::save,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.milestone_letter_save)) }

        Spacer(Modifier.height(Spacing.s2))

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.milestone_letter_cancel)) }

        Spacer(Modifier.height(Spacing.s6))
    }
}
