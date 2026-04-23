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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.ui.cbt.distortionByKey
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Which lens the user is reading the list through. Kept local (not in the
 * ViewModel) because it's pure UI preference — `rememberSaveable` carries it
 * across config changes but lets it reset naturally on a cold start.
 */
private enum class ThoughtChecksViewMode { BY_PATTERN, SEQUENTIAL }

/**
 * Read-only list of past thought-check entries.
 *
 * Two view modes:
 *   - **By pattern** (default) — rows grouped under each cognitive-distortion
 *     tag, with a running count, so repeat patterns jump out.
 *   - **Chronological** — a single flat newest-first stream. Better for
 *     scanning recent work without pattern noise.
 *
 * Visual style: reading material, not a dashboard. No cards, just well-spaced
 * headers and rows separated by subtle dividers. Matches DistortionsLibrary.
 */
@Composable
fun ThoughtChecksListScreen(
    onOpenDetail: (Long) -> Unit,
    vm: ThoughtChecksListViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var viewMode by rememberSaveable { mutableStateOf(ThoughtChecksViewMode.BY_PATTERN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.thought_checks_list_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.thought_checks_list_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )

        Spacer(Modifier.height(Spacing.s6))

        // Chips only surface once there's something to show — hiding them
        // during loading + empty keeps those states visually quiet.
        if (state.loaded && !state.isEmpty) {
            ViewModeChips(selected = viewMode, onChange = { viewMode = it })
            Spacer(Modifier.height(Spacing.s4))
        }

        when {
            !state.loaded -> Unit
            state.isEmpty -> EmptyThoughtChecks(Modifier.fillMaxSize())
            else -> when (viewMode) {
                ThoughtChecksViewMode.BY_PATTERN -> ThoughtChecksListBody(
                    groups = state.groups,
                    total = state.total,
                    onOpenDetail = onOpenDetail,
                )
                ThoughtChecksViewMode.SEQUENTIAL -> ThoughtChecksSequentialBody(
                    entries = state.allEntries,
                    total = state.total,
                    onOpenDetail = onOpenDetail,
                )
            }
        }
    }
}

/**
 * Two-chip selector for the list's view mode. Mirrors the FilterChip pattern
 * used in [com.tidelet.app.ui.log.CheckInScreen] so the interaction feels
 * consistent across the app.
 */
@Composable
private fun ViewModeChips(
    selected: ThoughtChecksViewMode,
    onChange: (ThoughtChecksViewMode) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        FilterChip(
            selected = selected == ThoughtChecksViewMode.BY_PATTERN,
            onClick = { onChange(ThoughtChecksViewMode.BY_PATTERN) },
            label = { Text(stringResource(R.string.thought_checks_view_mode_by_pattern)) },
            modifier = Modifier
                .weight(1f)
                .testTag("thought_checks_view_by_pattern"),
            colors = chipColors,
        )
        FilterChip(
            selected = selected == ThoughtChecksViewMode.SEQUENTIAL,
            onClick = { onChange(ThoughtChecksViewMode.SEQUENTIAL) },
            label = { Text(stringResource(R.string.thought_checks_view_mode_chronological)) },
            modifier = Modifier
                .weight(1f)
                .testTag("thought_checks_view_chronological"),
            colors = chipColors,
        )
    }
}

@Composable
private fun ThoughtChecksListBody(
    groups: List<ThoughtChecksGroup>,
    total: Int,
    onOpenDetail: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("thought_checks_list"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        // "All ({total})" header at the top so the user always sees the count.
        item(key = "__all_header") {
            AllHeader(total = total)
            Spacer(Modifier.height(Spacing.s4))
        }

        groups.forEach { group ->
            item(key = "header-${group.tagKey ?: "__untagged"}") {
                GroupHeader(group = group)
            }
            items(
                items = group.entries,
                key = { "record-${it.id}" },
            ) { record ->
                ThoughtCheckRow(record = record, onClick = { onOpenDetail(record.id) })
            }
            item(key = "spacer-${group.tagKey ?: "__untagged"}") {
                Spacer(Modifier.height(Spacing.s6))
            }
        }
    }
}

/**
 * Flat chronological body — one stream of rows, newest first, no pattern
 * grouping. Keeps the same `AllHeader` at the top and reuses [ThoughtCheckRow]
 * unchanged so rows look identical to the grouped view.
 */
@Composable
private fun ThoughtChecksSequentialBody(
    entries: List<ThoughtRecord>,
    total: Int,
    onOpenDetail: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("thought_checks_list"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        item(key = "__all_header") {
            AllHeader(total = total)
            Spacer(Modifier.height(Spacing.s4))
        }
        items(
            items = entries,
            key = { "record-${it.id}" },
        ) { record ->
            ThoughtCheckRow(record = record, onClick = { onOpenDetail(record.id) })
        }
    }
}

@Composable
private fun AllHeader(total: Int) {
    Text(
        text = stringResource(R.string.thought_checks_all_count, total),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = TideletTheme.extended.textSecondary,
    )
}

@Composable
private fun GroupHeader(group: ThoughtChecksGroup) {
    val label = group.tagKey
        ?.let { distortionByKey(it) }
        ?.labelRes
        ?.let { stringResource(it) }
        ?: stringResource(R.string.thought_checks_untagged)

    Column {
        Text(
            text = stringResource(R.string.thought_checks_group_label, label, group.count),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(Spacing.s2))
        HorizontalDivider(color = TideletTheme.extended.borderSubtle)
        Spacer(Modifier.height(Spacing.s2))
    }
}

@Composable
private fun ThoughtCheckRow(record: ThoughtRecord, onClick: () -> Unit) {
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
                text = record.thought.ifBlank { record.situation }.take(120),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = true),
            )
            Spacer(Modifier.height(Spacing.s2))
            Text(
                text = formatDate(record.dateIso),
                style = MaterialTheme.typography.bodySmall,
                color = TideletTheme.extended.textMuted,
                modifier = Modifier.padding(start = Spacing.s4),
            )
        }
    }
}

@Composable
private fun EmptyThoughtChecks(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.thought_checks_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.s2))
            Text(
                text = stringResource(R.string.thought_checks_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TideletTheme.extended.textMuted,
            )
        }
    }
}

/** ISO "YYYY-MM-DD" → localised medium date (e.g. "15 Jan 2025"). */
private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
} catch (_: Exception) {
    iso
}
