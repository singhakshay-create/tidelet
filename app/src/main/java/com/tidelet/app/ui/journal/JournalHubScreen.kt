package com.tidelet.app.ui.journal

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.Shapes
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Landing screen for the Journal bottom-nav tab.
 *
 * Two entry points — past Thought Checks, past free-writes — plus small,
 * muted entry points back into the write flows so a user who opens Journal
 * intending to *write* isn't dead-ended. Counts come from
 * [JournalHubViewModel].
 *
 * Visual language matches Settings rows: borderless, rows separated by subtle
 * dividers, not boxed-card chrome.
 */
@Composable
fun JournalHubScreen(
    onOpenThoughtChecks: () -> Unit,
    onOpenJournalEntries: () -> Unit,
    onOpenEveningReviews: () -> Unit,
    onWriteThoughtCheck: () -> Unit,
    onWriteFreely: () -> Unit,
    vm: JournalHubViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.journal_tab_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.journal_tab_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )

        Spacer(Modifier.height(Spacing.s8))

        // Read-side entry points — the primary reason the tab exists.
        SectionLabel(stringResource(R.string.journal_tab_section_review))
        Spacer(Modifier.height(Spacing.s2))

        HubRow(
            icon = Icons.Outlined.Psychology,
            title = stringResource(R.string.journal_tab_thought_checks_title),
            subtitle = if (state.loaded) {
                pluralCount(
                    count = state.thoughtChecksCount,
                    singularRes = R.string.journal_tab_thought_checks_count_singular,
                    pluralRes = R.string.journal_tab_thought_checks_count_plural,
                )
            } else {
                stringResource(R.string.journal_tab_loading)
            },
            onClick = onOpenThoughtChecks,
        )
        HubRow(
            icon = Icons.Outlined.AutoStories,
            title = stringResource(R.string.journal_tab_entries_title),
            subtitle = if (state.loaded) {
                pluralCount(
                    count = state.journalEntriesCount,
                    singularRes = R.string.journal_tab_entries_count_singular,
                    pluralRes = R.string.journal_tab_entries_count_plural,
                )
            } else {
                stringResource(R.string.journal_tab_loading)
            },
            onClick = onOpenJournalEntries,
        )
        // PHASE_2 §9A5 — third feed in the read-side hub.
        HubRow(
            icon = Icons.Outlined.Insights,
            title = stringResource(R.string.journal_tab_evening_reviews_title),
            subtitle = if (state.loaded) {
                pluralCount(
                    count = state.eveningReviewsCount,
                    singularRes = R.string.journal_tab_evening_reviews_count_singular,
                    pluralRes = R.string.journal_tab_evening_reviews_count_plural,
                )
            } else {
                stringResource(R.string.journal_tab_loading)
            },
            onClick = onOpenEveningReviews,
        )

        Spacer(Modifier.height(Spacing.s8))

        // Write-side entry points — muted, for users who came here to write.
        SectionLabel(stringResource(R.string.journal_tab_section_write))
        Spacer(Modifier.height(Spacing.s2))

        HubRow(
            icon = Icons.Outlined.Insights,
            title = stringResource(R.string.journal_tab_write_thought_check),
            subtitle = stringResource(R.string.journal_tab_write_thought_check_sub),
            onClick = onWriteThoughtCheck,
        )
        HubRow(
            icon = Icons.Outlined.AutoStories,
            title = stringResource(R.string.journal_tab_write_freely),
            subtitle = stringResource(R.string.journal_tab_write_freely_sub),
            onClick = onWriteFreely,
        )

        Spacer(Modifier.height(Spacing.s8))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TideletTheme.extended.textMuted,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun HubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TideletTheme.extended.borderSubtle, Shapes.md)
            .clickable { onClick() }
            .padding(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.s1))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TideletTheme.extended.textMuted,
            )
        }
    }
    Spacer(Modifier.height(Spacing.s3))
}

/**
 * Resolve a singular/plural string resource with a numeric argument.
 * We avoid Android's `quantityString` (and the complexity of plural rules)
 * because Tidelet only ever needs "0/1 / many" for these short labels.
 */
@Composable
private fun pluralCount(count: Int, singularRes: Int, pluralRes: Int): String =
    if (count == 1) stringResource(singularRes, count) else stringResource(pluralRes, count)
