package com.tidelet.app.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.data.db.CheckInTrigger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The Daily Check-in form.
 *
 * Captures four pieces of information for a given date:
 *   1. Did you drink? (Yes / No)
 *   2. If yes — how many standard drinks? (stepper)
 *   3. How did the day feel? (1–5 mood chips, optional)
 *   4. Anything to remember? (optional free text)
 *
 * This screen is deliberately *outside* the SOS flow. It uses the normal
 * Material palette — not the warm coral SOS surface — so users feel calm
 * logging the day, not in an emergency state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    onDone: () -> Unit,
    vm: CheckInViewModel = viewModel(factory = CheckInViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Once the repo says the save completed, hop back to where we came from.
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkin_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.checkin_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.checkin_for_date, state.date.prettyLabel()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            DidDrinkSection(
                didDrink = state.didDrink,
                onSet = vm::setDidDrink,
            )

            if (state.didDrink == true) {
                Spacer(Modifier.height(20.dp))
                DrinkCountSection(
                    count = state.drinkCount,
                    onDec = vm::decrementDrinkCount,
                    onInc = vm::incrementDrinkCount,
                )
                Spacer(Modifier.height(24.dp))
                TriggerSection(
                    selected = state.trigger,
                    onToggle = vm::toggleTrigger,
                )
            }

            Spacer(Modifier.height(24.dp))
            MoodSection(mood = state.mood, onSet = vm::setMood)

            Spacer(Modifier.height(24.dp))
            NoteSection(note = state.note, onChange = vm::setNote)

            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.checkin_cancel)) }
                Button(
                    onClick = vm::save,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f).testTag("checkin_save"),
                ) { Text(stringResource(R.string.checkin_save)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Sections ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DidDrinkSection(
    didDrink: Boolean?,
    onSet: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.checkin_drink_question),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterChip(
            selected = didDrink == false,
            onClick = { onSet(false) },
            label = { Text(stringResource(R.string.checkin_drank_no)) },
            modifier = Modifier.weight(1f).testTag("checkin_did_drink_no"),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        FilterChip(
            selected = didDrink == true,
            onClick = { onSet(true) },
            label = { Text(stringResource(R.string.checkin_drank_yes)) },
            modifier = Modifier.weight(1f).testTag("checkin_did_drink_yes"),
        )
    }
}

@Composable
private fun DrinkCountSection(
    count: Int,
    onDec: () -> Unit,
    onInc: () -> Unit,
) {
    Text(
        text = stringResource(R.string.checkin_how_many),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(
            onClick = onDec,
            enabled = count > 1,
            modifier = Modifier.testTag("checkin_drink_decrement"),
        ) {
            Icon(
                Icons.Rounded.Remove,
                contentDescription = stringResource(R.string.checkin_decrement),
            )
        }
        Spacer(Modifier.width(24.dp))
        Box(
            modifier = Modifier.size(width = 64.dp, height = 40.dp).testTag("checkin_drink_count"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(Modifier.width(24.dp))
        OutlinedButton(
            onClick = onInc,
            modifier = Modifier.testTag("checkin_drink_increment"),
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.checkin_increment),
            )
        }
    }
}

/**
 * Optional "what was happening?" chip row. Shown only when the user indicated
 * they drank. Tapping the already-selected chip clears the trigger.
 *
 * We use FlowRow so the 7 chips wrap naturally on narrow screens without
 * clipping or forcing the user to horizontal-scroll.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TriggerSection(
    selected: String?,
    onToggle: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.checkin_trigger_question),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.checkin_trigger_help),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth().testTag("checkin_trigger_chips"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CheckInTrigger.ALL.forEach { key ->
            FilterChip(
                selected = selected == key,
                onClick = { onToggle(key) },
                label = { Text(stringResource(triggerLabelRes(key))) },
            )
        }
    }
}

private fun triggerLabelRes(key: String): Int = when (key) {
    CheckInTrigger.STRESS -> R.string.checkin_trigger_stress
    CheckInTrigger.SOCIAL -> R.string.checkin_trigger_social
    CheckInTrigger.BOREDOM -> R.string.checkin_trigger_boredom
    CheckInTrigger.CELEBRATION -> R.string.checkin_trigger_celebration
    CheckInTrigger.HALT -> R.string.checkin_trigger_halt
    CheckInTrigger.HABIT -> R.string.checkin_trigger_habit
    else -> R.string.checkin_trigger_other
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodSection(mood: Int?, onSet: (Int) -> Unit) {
    Text(
        text = stringResource(R.string.checkin_mood_question),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(12.dp))
    val moodLabels = listOf(
        R.string.checkin_mood_1,
        R.string.checkin_mood_2,
        R.string.checkin_mood_3,
        R.string.checkin_mood_4,
        R.string.checkin_mood_5,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        moodLabels.forEachIndexed { i, labelRes ->
            val value = i + 1
            FilterChip(
                selected = mood == value,
                onClick = { onSet(value) },
                label = { Text(stringResource(labelRes)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NoteSection(note: String, onChange: (String) -> Unit) {
    Text(
        text = stringResource(R.string.checkin_note_label),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = note,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.checkin_note_hint)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 8,
    )
}

// ---- Helpers ----

private fun LocalDate.prettyLabel(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
