package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme

/**
 * The user's personal "why I'm doing this" list — read in the hard moments,
 * written in the calm ones.
 *
 * Arrival flow:
 *   - empty list → gentle prompt; the composer at the bottom is the next step
 *   - non-empty → each reason as its own soft card with a delete button
 *
 * On dispose we log a REASONS/GOT_THROUGH CravingEvent — but only if the user
 * actually had reasons to read during the visit, so Stats doesn't double-count
 * empty sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasonsScreen(
    onBack: () -> Unit,
    vm: ReasonsViewModel = viewModel(),
) {
    val reasons by vm.reasons.collectAsStateWithLifecycle()
    val warmSurface = TideletTheme.extended.sosSurface
    val onWarm = TideletTheme.extended.onSosSurface

    // Did the user see any reasons at any point during this visit? Controls
    // whether we log the event on dispose.
    var sawReasons by remember { mutableStateOf(false) }
    if (reasons.isNotEmpty()) sawReasons = true

    DisposableEffect(Unit) {
        onDispose { vm.logViewed(hadReasons = sawReasons) }
    }

    var draft by remember { mutableStateOf("") }

    Scaffold(
        containerColor = warmSurface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reasons_title), color = onWarm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = onWarm,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = warmSurface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(warmSurface)
                .padding(horizontal = 20.dp),
        ) {
            if (reasons.isEmpty()) {
                EmptyReasons(onWarm = onWarm, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(reasons, key = { it.id }) { reason ->
                        ReasonCard(
                            text = reason.text,
                            onDelete = { vm.deleteReason(reason.id) },
                            reasonId = reason.id,
                        )
                    }
                }
            }

            // Composer — always visible so a user can add a reason in the moment
            // they're already thinking of it.
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(stringResource(R.string.reasons_add_hint)) },
                    modifier = Modifier.weight(1f).testTag("reasons_input"),
                    singleLine = false,
                    maxLines = 3,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val t = draft.trim()
                        if (t.isNotEmpty()) {
                            vm.addReason(t)
                            draft = ""
                        }
                    },
                    enabled = draft.trim().isNotEmpty(),
                    modifier = Modifier.testTag("reasons_submit"),
                ) { Text(stringResource(R.string.reasons_add_button)) }
            }
        }
    }
}

@Composable
private fun EmptyReasons(onWarm: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp).testTag("reasons_empty_cta"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.reasons_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = onWarm,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.reasons_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = onWarm,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReasonCard(text: String, onDelete: () -> Unit, reasonId: Long = 0L) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("reasons_chip_$reasonId"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete, modifier = Modifier.testTag("reasons_delete_$reasonId")) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.reasons_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
