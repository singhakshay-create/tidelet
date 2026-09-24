package com.tidelet.app.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import com.tidelet.app.ui.theme.ErrorRed
import com.tidelet.app.ui.theme.SuccessGreen
import com.tidelet.app.ui.theme.TideletTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Daily check-in history.
 *
 * Top: a card for today. If the user hasn't checked in yet, a "Check in for today"
 * button is shown. If they have, the card summarises the entry and offers an
 * "Edit" affordance — both lead to the same form, parameterised by date.
 *
 * Below: a reverse-chronological LazyColumn of past check-ins, each tappable to
 * edit. This lets people backfill a day they forgot or change a call they made
 * in haste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onOpenCheckIn: (LocalDate) -> Unit,
    vm: LogViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val heatmapData by vm.heatmapData.collectAsStateWithLifecycle()
    val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.log_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            if (heatmapData.isNotEmpty()) {
                CravingHeatmapCard(
                    data = heatmapData,
                    selectedDay = selectedDay,
                    onSelectDay = vm::selectDay,
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
            }

            val currentSelection = selectedDay
            if (currentSelection != null) {
                val summary = heatmapData[currentSelection]
                if (summary != null) {
                    SelectedDayDetail(
                        day = currentSelection,
                        summary = summary,
                        onClear = { vm.selectDay(null) },
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                }
            }

            TodayCard(
                row = state.today,
                onCheckIn = { onOpenCheckIn(today) },
            )

            Spacer(Modifier.height(16.dp))

            val displayHistory = if (currentSelection != null) {
                state.history.filter { it.date == currentSelection }
            } else {
                state.history
            }

            if (displayHistory.isEmpty() && state.today == null && currentSelection == null) {
                EmptyLog(modifier = Modifier.fillMaxSize())
            } else if (displayHistory.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(displayHistory, key = { it.date.toString() }) { row ->
                        HistoryRow(row = row, onClick = { onOpenCheckIn(row.date) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(
    row: LogRow?,
    onCheckIn: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.log_today_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            if (row == null) {
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                ) { Text(stringResource(R.string.log_check_in_today)) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(row = row)
                    Spacer(Modifier.width(12.dp))
                    row.mood?.let { MoodIndicator(mood = it) }
                }
                if (!row.note.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = row.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(12.dp))
                EditToday(onClick = onCheckIn)
            }
        }
    }
}

@Composable
private fun EditToday(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.log_edit_today),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun HistoryRow(row: LogRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.date.prettyLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                row.mood?.let { MoodIndicator(mood = it) }
            }
            Spacer(Modifier.height(6.dp))
            StatusPill(row = row)
            if (!row.note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = row.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

/** Little chip-ish label showing drink-free vs. drank-N for a row. */
@Composable
private fun StatusPill(row: LogRow) {
    val fg: Color
    val label: String
    if (!row.didDrink) {
        fg = SuccessGreen
        label = stringResource(R.string.log_entry_drink_free)
    } else {
        fg = ErrorRed
        label = row.drinkCount?.let { stringResource(R.string.log_entry_drank, it) }
            ?: stringResource(R.string.log_entry_drank_unknown)
    }
    Surface(
        color = Color.Transparent,
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, fg.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Mood dot + numeric label. 1 = rough, 5 = great. */
@Composable
private fun MoodIndicator(mood: Int) {
    val safeMood = mood.coerceIn(1, 5)
    val moodLabelRes = when (safeMood) {
        1 -> R.string.checkin_mood_1
        2 -> R.string.checkin_mood_2
        3 -> R.string.checkin_mood_3
        4 -> R.string.checkin_mood_4
        else -> R.string.checkin_mood_5
    }
    val tint = MaterialTheme.colorScheme.secondary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(tint, RectangleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(moodLabelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyLog(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.log_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.log_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SelectedDayDetail(
    day: LocalDate,
    summary: DayCravingSummary,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "[REF: DATA_POINT_ANALYSIS]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                Text(
                    text = "CLOSE →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable(onClick = onClear),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = day.prettyLabel().uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.heatmap_day_count, summary.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            summary.events.forEach { event ->
                Spacer(Modifier.height(8.dp))
                CravingEventRow(event)
            }
        }
    }
}

@Composable
private fun CravingEventRow(event: CravingEvent) {
    val toolLabel = when (event.tool) {
        SosToolKey.RIDE_THE_WAVE -> stringResource(R.string.sos_ride_wave)
        SosToolKey.BREATHE -> stringResource(R.string.sos_breathe)
        SosToolKey.REASONS -> stringResource(R.string.sos_reasons)
        SosToolKey.DISTRACTIONS -> stringResource(R.string.sos_distractions)
        SosToolKey.JOURNAL -> stringResource(R.string.sos_journal)
        SosToolKey.THOUGHT_CHECK -> stringResource(R.string.heatmap_tool_thought_check)
        SosToolKey.REFUSAL_PRACTICE -> stringResource(R.string.heatmap_tool_refusal)
        else -> event.tool
    }
    val outcomeLabel = when (event.outcome) {
        CravingOutcome.GOT_THROUGH -> stringResource(R.string.heatmap_outcome_got_through)
        CravingOutcome.STILL_STRUGGLING -> stringResource(R.string.heatmap_outcome_struggling)
        CravingOutcome.DRANK -> stringResource(R.string.heatmap_outcome_drank)
        else -> event.outcome
    }
    val time = Instant.ofEpochMilli(event.timestampEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "$toolLabel · $outcomeLabel",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Row {
            event.intensity?.let {
                Text(
                    text = stringResource(R.string.heatmap_intensity_label, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LocalDate.prettyLabel(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
