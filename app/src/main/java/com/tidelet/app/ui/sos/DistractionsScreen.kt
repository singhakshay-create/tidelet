package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme
import kotlin.random.Random

/**
 * "Do something else" — a curated list of 2-to-15 minute alternatives to
 * drinking. Built-ins come from `@array/builtin_distractions`. In Phase 2 the
 * user will also be able to add their own.
 *
 * Interaction:
 *   - tap "Pick one for me" to highlight a random item and scroll it into view
 *   - tap "I did it" to log the tool as a got-through event, "Nothing worked"
 *     as a still-struggling event
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistractionsScreen(
    onDone: () -> Unit,
    vm: DistractionsViewModel = viewModel(),
) {
    val warmSurface = TideletTheme.extended.sosSurface
    val onWarm = TideletTheme.extended.onSosSurface
    val accent = TideletTheme.extended.sosAccent

    val items = stringArrayResource(R.array.builtin_distractions).toList()

    // -1 = nothing picked yet (user hasn't tapped the shuffle button).
    var pickedIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    // Scroll the picked card into view whenever pickedIndex changes.
    LaunchedEffect(pickedIndex) {
        if (pickedIndex >= 0) listState.animateScrollToItem(pickedIndex)
    }

    Scaffold(
        containerColor = warmSurface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.distractions_title), color = onWarm) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            Text(
                text = stringResource(R.string.distractions_body),
                style = MaterialTheme.typography.bodyLarge,
                color = onWarm,
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { pickedIndex = Random.nextInt(items.size) },
                modifier = Modifier.fillMaxWidth().testTag("distractions_shuffle"),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null)
                Text("  " + stringResource(R.string.distractions_pick_one))
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(items.size) { index ->
                    DistractionCard(
                        text = items[index],
                        highlighted = index == pickedIndex,
                        accent = accent,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        vm.logNothingWorked()
                        onDone()
                    },
                    modifier = Modifier.weight(1f).testTag("distractions_nothing_worked"),
                ) { Text(stringResource(R.string.distractions_skipped)) }

                Button(
                    onClick = {
                        vm.logDidIt()
                        onDone()
                    },
                    modifier = Modifier.weight(1f).testTag("distractions_did_it"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text(stringResource(R.string.distractions_did_it)) }
            }
        }
    }
}

@Composable
private fun DistractionCard(text: String, highlighted: Boolean, accent: Color) {
    val container = if (highlighted) {
        accent.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (highlighted) it.border(2.dp, accent, RoundedCornerShape(14.dp)) else it },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
            )
        }
    }
}
