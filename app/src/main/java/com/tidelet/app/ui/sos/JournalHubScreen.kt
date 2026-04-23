package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Two-option hub reached when the user taps "Write it down" on the SOS grid.
 *
 * Freeform writing and the CBT Thought Check are structurally different (one
 * prompt vs. four, JournalEntry vs. ThoughtRecord), so rather than squash
 * them into a single screen we offer the choice here and route onward. The
 * hub itself is a cognitive pause — "what do I need right now?" — which is
 * in keeping with the "last rung" framing of the Journal tile on SOS.
 *
 * Stateless. The parent navigates to the two sub-screens via callbacks.
 */
@Composable
fun JournalHubScreen(
    onOpenFreeform: () -> Unit,
    onOpenThoughtCheck: () -> Unit,
    onBack: () -> Unit,
) {
    val surface = TideletTheme.extended.sosSurface
    val onSurface = TideletTheme.extended.onSosSurface

    Box(
        modifier = Modifier.fillMaxSize().background(surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.journal_hub_title),
                style = MaterialTheme.typography.headlineSmall,
                color = onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.journal_hub_body),
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            HubCard(
                icon = Icons.Rounded.Edit,
                titleRes = R.string.journal_hub_freeform_title,
                subtitleRes = R.string.journal_hub_freeform_sub,
                onClick = onOpenFreeform,
            )

            Spacer(Modifier.height(12.dp))

            HubCard(
                icon = Icons.Rounded.Psychology,
                titleRes = R.string.journal_hub_thought_check_title,
                subtitleRes = R.string.journal_hub_thought_check_sub,
                onClick = onOpenThoughtCheck,
            )

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.journal_hub_back)) }
        }
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    titleRes: Int,
    subtitleRes: Int,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TideletTheme.extended.onSosSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = TideletTheme.extended.sosAccent)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
