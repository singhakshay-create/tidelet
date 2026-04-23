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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme
import java.time.LocalDate
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

            TodayCard(
                row = state.today,
                onCheckIn = { onOpenCheckIn(today) },
            )

            Spacer(Modifier.height(16.dp))

            if (state.history.isEmpty() && state.today == null) {
                EmptyLog(modifier = Modifier.fillMaxSize())
            } else if (state.history.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(state.history, key = { it.date.toString() }) { row ->
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.log_today_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))

            if (row == null) {
                Button(
                    onClick = onCheckIn,
                    modifier = Modifier.fillMaxWidth(),
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.date.prettyLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
    val bg: Color
    val fg: Color
    val label: String
    if (!row.didDrink) {
        bg = TideletTheme.extended.success.copy(alpha = 0.16f)
        fg = TideletTheme.extended.success
        label = stringResource(R.string.log_entry_drink_free)
    } else {
        bg = MaterialTheme.colorScheme.errorContainer
        fg = MaterialTheme.colorScheme.onErrorContainer
        label = row.drinkCount?.let { stringResource(R.string.log_entry_drank, it) }
            ?: stringResource(R.string.log_entry_drank_unknown)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                .size(8.dp)
                .background(tint, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(moodLabelRes),
            style = MaterialTheme.typography.labelMedium,
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

private fun LocalDate.prettyLabel(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
