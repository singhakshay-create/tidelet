package com.tidelet.app.ui.cbt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R

/**
 * Drink Refusal Rehearsal.
 *
 * Shows the user a list of phrases they can say to turn down a drink —
 * mixing a built-in set of examples with any they've added themselves — and
 * lets them tap "practice" to have one picked at random to rehearse aloud.
 *
 * Intentionally simple. CBT skill rehearsal works because it's boring and
 * repeatable, not because of clever UX. The screen can live either as a
 * proactive tool (from Settings) or mid-craving (reachable from
 * Distractions); it behaves the same either way.
 */
@Composable
fun RefusalRehearsalScreen(
    onBack: () -> Unit,
    vm: RefusalRehearsalViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val userPhrases by vm.userPhrases.collectAsStateWithLifecycle()

    // Built-in phrases live in resources — localisable without touching the DB.
    // If you add/remove here, update the strings.xml array accordingly.
    val builtins = stringArrayResource(R.array.refusal_builtin_phrases).toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.refusal_back),
                    )
                }
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = stringResource(R.string.refusal_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.refusal_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // --- Practice area ---
            Spacer(Modifier.height(20.dp))
            PracticeCard(
                practiceTarget = state.practiceTarget,
                builtins = builtins,
                userPhrases = userPhrases,
                onPickAgain = { vm.practiceRandom(builtinCount = builtins.size) },
                onDismiss = vm::clearPracticeTarget,
                rehearsedCount = state.rehearsedCount,
            )

            // --- Built-in list ---
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.refusal_builtin_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            builtins.forEach { phrase ->
                PhraseRow(text = phrase, onDelete = null)
                Spacer(Modifier.height(6.dp))
            }

            // --- User list ---
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.refusal_user_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (userPhrases.isEmpty()) {
                Text(
                    text = stringResource(R.string.refusal_user_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                userPhrases.forEach { row ->
                    PhraseRow(text = row.text, onDelete = { vm.deletePhrase(row.id) })
                    Spacer(Modifier.height(6.dp))
                }
            }

            // --- Add phrase ---
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.newPhraseDraft,
                onValueChange = vm::setNewPhraseDraft,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.refusal_add_hint)) },
                minLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = vm::addPhrase,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.newPhraseDraft.trim().isNotEmpty(),
            ) {
                Text(stringResource(R.string.refusal_add_button))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * Card showing the currently-picked practice phrase. Collapses to a
 * "tap to pick one" state when nothing is selected.
 */
@Composable
private fun PracticeCard(
    practiceTarget: PracticeTarget?,
    builtins: List<String>,
    userPhrases: List<com.tidelet.app.data.db.RefusalPhrase>,
    onPickAgain: () -> Unit,
    onDismiss: () -> Unit,
    rehearsedCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val phrase: String? = when (practiceTarget) {
                null -> null
                is PracticeTarget.Builtin -> builtins.getOrNull(practiceTarget.index)
                is PracticeTarget.User -> userPhrases
                    .firstOrNull { it.id == practiceTarget.phraseId }?.text
            }

            if (phrase != null) {
                Text(
                    text = stringResource(R.string.refusal_practice_say),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\u201C$phrase\u201D",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.refusal_practice_done))
                    }
                    Spacer(Modifier.padding(6.dp))
                    Button(onClick = onPickAgain) {
                        Text(stringResource(R.string.refusal_practice_another))
                    }
                }
                if (rehearsedCount > 1) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.refusal_practice_session_count,
                            rehearsedCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.refusal_practice_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onPickAgain) {
                    Text(stringResource(R.string.refusal_practice_start))
                }
            }
        }
    }
}

@Composable
private fun PhraseRow(text: String, onDelete: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.refusal_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
