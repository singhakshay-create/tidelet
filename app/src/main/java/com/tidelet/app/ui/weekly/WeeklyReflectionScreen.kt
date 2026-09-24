package com.tidelet.app.ui.weekly

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import java.time.format.DateTimeFormatter

/**
 * Sunday-evening reflection form.
 *
 * Three elements, top to bottom:
 *   - week range subtitle (so the user sees *which* week they're rating)
 *   - rating chips 1–5 with short labels (rough / hard / okay / good / great)
 *   - optional free-text "one sentence" field
 *
 * We autopopulate any existing row for this week's Monday, so writing again
 * is really editing. Save pops back to wherever we came from (Home or Stats).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReflectionScreen(
    onDone: () -> Unit,
    vm: WeeklyReflectionViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Once the VM confirms the row is persisted, leave the screen.
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weekly_title)) },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Humanised week range — e.g. "Apr 13 – Apr 19"
            val rangeFmt = DateTimeFormatter.ofPattern("MMM d")
            val rangeLabel = "${state.weekStart.format(rangeFmt)} – ${state.weekStart.plusDays(6).format(rangeFmt)}"
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.weekly_rating_prompt),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.weekly_rating_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            RatingChips(
                selected = state.rating,
                onSelect = vm::setRating,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.weekly_note_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.weekly_note_hint)) },
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = vm::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.weekly_save)) }
        }
    }
}

@Composable
private fun RatingChips(
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    val labels = listOf(
        1 to R.string.weekly_rating_1,
        2 to R.string.weekly_rating_2,
        3 to R.string.weekly_rating_3,
        4 to R.string.weekly_rating_4,
        5 to R.string.weekly_rating_5,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { (value, labelRes) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(stringResource(labelRes)) },
                colors = FilterChipDefaults.filterChipColors(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
