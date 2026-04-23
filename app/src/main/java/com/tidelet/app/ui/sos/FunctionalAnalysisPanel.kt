package com.tidelet.app.ui.sos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tidelet.app.R
import com.tidelet.app.data.db.CheckInTrigger
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Reusable in-line "map what happened" panel.
 *
 * Rendered after a craving (Ride the Wave outcome) or a slip (Compassionate
 * Reset). Three small fields — a trigger chip, what-thought-came-first, and
 * what-followed — with a save button that hands the filled values back via
 * [onSave]. Absolutely skippable: the host screen should render the panel
 * with a visible "skip" path.
 *
 * State is local to the panel so the call-site doesn't need a ViewModel just
 * for these three fields. The panel reports only on Save — if the user
 * navigates away, nothing is persisted.
 *
 * The panel is intentionally designed to look like a card inset into the
 * existing screen, not a new screen. This matches the "in-line, not
 * deferred" request: the user doesn't leave the moment.
 */
@Composable
fun FunctionalAnalysisPanel(
    /** Optional title override — defaults to the generic "map what happened". */
    titleRes: Int = R.string.func_analysis_title,
    /** Called when the user submits. All three fields may be null/blank. */
    onSave: (antecedent: String?, thought: String?, followingAction: String?) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var antecedent by remember { mutableStateOf<String?>(null) }
    var thought by remember { mutableStateOf("") }
    var followingAction by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.func_analysis_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            // Trigger chip row — reuses the existing CheckIn trigger taxonomy
            // so future Stats queries can join across surfaces on the same key.
            Text(
                text = stringResource(R.string.func_analysis_antecedent_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CheckInTrigger.ALL.forEach { tag ->
                    FilterChip(
                        selected = antecedent == tag,
                        onClick = { antecedent = if (antecedent == tag) null else tag },
                        label = { Text(triggerLabel(tag)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TideletTheme.extended.sosAccent.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.func_analysis_thought_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = thought,
                onValueChange = { thought = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text(stringResource(R.string.func_analysis_thought_hint)) },
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.func_analysis_following_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = followingAction,
                onValueChange = { followingAction = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text(stringResource(R.string.func_analysis_following_hint)) },
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.func_analysis_skip))
                }
                Spacer(Modifier.padding(4.dp))
                TextButton(
                    onClick = {
                        onSave(
                            antecedent,
                            thought.trim().takeIf { it.isNotEmpty() },
                            followingAction.trim().takeIf { it.isNotEmpty() },
                        )
                    },
                    // Save is always enabled — user can submit with just a
                    // trigger chip if that's all they have. Only fully-empty
                    // submissions will be discarded at the repository.
                ) {
                    Text(stringResource(R.string.func_analysis_save))
                }
            }
        }
    }
}

/** Local convenience for trigger labels — mirrors the CheckIn form's chip strings. */
@Composable
private fun triggerLabel(key: String): String = when (key) {
    CheckInTrigger.STRESS -> stringResource(R.string.checkin_trigger_stress)
    CheckInTrigger.SOCIAL -> stringResource(R.string.checkin_trigger_social)
    CheckInTrigger.BOREDOM -> stringResource(R.string.checkin_trigger_boredom)
    CheckInTrigger.CELEBRATION -> stringResource(R.string.checkin_trigger_celebration)
    CheckInTrigger.HALT -> stringResource(R.string.checkin_trigger_halt)
    CheckInTrigger.HABIT -> stringResource(R.string.checkin_trigger_habit)
    CheckInTrigger.OTHER -> stringResource(R.string.checkin_trigger_other)
    else -> key
}

/** Unused — kept for a future "readonly preview" variant. */
@Suppress("unused")
private val UNSUSED: Color = Color.Transparent
