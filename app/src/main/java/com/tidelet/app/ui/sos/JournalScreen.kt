package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme

/**
 * The Journal SOS tool.
 *
 * This is the "last rung" of the SOS toolkit — Reframe-benchmarking notes
 * call it out as the surface we reach for when Ride the Wave and Breathe
 * haven't quieted things down. One prompt, a large writing area, two
 * buttons (save / skip). Same warm palette as the rest of the SOS flow so
 * the visual continuity is preserved.
 *
 * After saving, we pivot to a short confirmation view — a quiet "it's
 * written down" moment — before the user taps back to the SOS menu. We
 * don't auto-dismiss, because if someone just unloaded something hard,
 * letting them sit with the confirmation for a beat feels right.
 */
@Composable
fun JournalScreen(
    onDone: () -> Unit,
    onViewEntries: () -> Unit = {},
    intensity: Int? = null,
    vm: JournalViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val surface = TideletTheme.extended.sosSurface
    val onSurface = TideletTheme.extended.onSosSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        if (state.saved) {
            JournalSaved(
                onClose = onDone,
                onViewEntries = onViewEntries,
                onSurface = onSurface,
            )
        } else {
            JournalEditor(
                text = state.text,
                onTextChange = vm::setText,
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
private fun JournalEditor(
    text: String,
    onTextChange: (String) -> Unit,
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.journal_title),
            style = MaterialTheme.typography.headlineSmall,
            color = onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.journal_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
            placeholder = { Text(stringResource(R.string.journal_placeholder)) },
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSave,
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) { Text(stringResource(R.string.journal_save)) }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.journal_skip)) }
    }
}

@Composable
private fun JournalSaved(
    onClose: () -> Unit,
    onViewEntries: () -> Unit,
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
            text = stringResource(R.string.journal_saved_title),
            style = MaterialTheme.typography.headlineMedium,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.journal_saved_body),
            style = MaterialTheme.typography.bodyLarge,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onViewEntries) {
            Text(stringResource(R.string.journal_saved_view_entries))
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.journal_back)) }
    }
}
