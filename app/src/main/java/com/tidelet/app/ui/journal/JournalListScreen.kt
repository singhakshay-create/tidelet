package com.tidelet.app.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
 * Read-only list of free-text journal entries. Reverse-chronological with a
 * case-insensitive substring search box at the top.
 */
@Composable
fun JournalListScreen(
    onOpenDetail: (Long) -> Unit,
    vm: JournalListViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.journal_list_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.journal_list_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )

        Spacer(Modifier.height(Spacing.s6))

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("journal_search"),
            placeholder = { Text(stringResource(R.string.journal_search_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.isSearching) {
                    IconButton(onClick = vm::clearQuery) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.journal_search_clear),
                        )
                    }
                }
            },
            singleLine = true,
        )

        Spacer(Modifier.height(Spacing.s4))

        when {
            !state.loaded -> Unit
            state.isEmpty -> EmptyJournal(Modifier.fillMaxSize())
            state.hasNoMatches -> NoMatches(
                Modifier.fillMaxSize(),
                query = state.query,
            )
            else -> JournalListBody(entries = state.filtered, onOpenDetail = onOpenDetail)
        }
    }
}

@Composable
private fun JournalListBody(entries: List<JournalEntry>, onOpenDetail: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("journal_list"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        items(
            items = entries,
            key = { "entry-${it.id}" },
        ) { entry ->
            JournalRow(entry = entry, onClick = { onOpenDetail(entry.id) })
            HorizontalDivider(color = TideletTheme.extended.borderSubtle)
        }
    }
}

@Composable
private fun JournalRow(entry: JournalEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.s3),
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = firstLine(entry.text),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = true),
            )
            Text(
                text = formatDate(entry.dateIso),
                style = MaterialTheme.typography.bodySmall,
                color = TideletTheme.extended.textMuted,
                modifier = Modifier.padding(start = Spacing.s4),
            )
        }
    }
}

@Composable
private fun EmptyJournal(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.journal_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.s2))
            Text(
                text = stringResource(R.string.journal_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TideletTheme.extended.textMuted,
            )
        }
    }
}

@Composable
private fun NoMatches(modifier: Modifier = Modifier, query: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.journal_search_no_matches, query),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )
    }
}

/** Trim to the first non-empty line; entries often start with a leading newline. */
private fun firstLine(text: String): String =
    text.lineSequence().map { it.trim() }.firstOrNull { it.isNotBlank() } ?: ""

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
} catch (_: Exception) {
    iso
}
