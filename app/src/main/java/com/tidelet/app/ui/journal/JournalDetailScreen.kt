package com.tidelet.app.ui.journal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Read-only detail for one journal entry. Full-text, no truncation.
 */
@Composable
fun JournalDetailScreen(
    onBack: () -> Unit,
    vm: JournalDetailViewModel = viewModel(factory = JournalDetailViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        when {
            !state.loaded -> Unit
            state.notFound -> NotFoundBody(onBack = onBack)
            else -> DetailBody(entry = state.entry!!, onBack = onBack)
        }
    }
}

@Composable
private fun DetailBody(entry: JournalEntry, onBack: () -> Unit) {
    Text(
        text = formatDate(entry.dateIso),
        style = MaterialTheme.typography.bodySmall,
        color = TideletTheme.extended.textMuted,
    )
    Spacer(Modifier.height(Spacing.s6))

    Text(
        text = entry.text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.height(Spacing.s8))

    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_back))
    }
    Spacer(Modifier.height(Spacing.s4))
}

@Composable
private fun NotFoundBody(onBack: () -> Unit) {
    Text(
        text = stringResource(R.string.detail_not_found),
        style = MaterialTheme.typography.bodyLarge,
        color = TideletTheme.extended.textMuted,
    )
    Spacer(Modifier.height(Spacing.s6))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_back))
    }
}

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
} catch (_: Exception) {
    iso
}
