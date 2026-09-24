package com.tidelet.app.ui.cbt

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tidelet.app.R
import com.tidelet.app.ui.theme.Shapes
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme

/**
 * Browsable reference of cognitive distortions with drinking-flavoured
 * examples, reframe techniques, and challenging questions. Purely
 * psychoeducational — no user input, no persistence.
 *
 * Reached from:
 *   - Thought Check ("is there a pattern here?" link)
 *   - Settings > Cognitive distortions (reference)
 *
 * No ViewModel. Content comes from [COGNITIVE_DISTORTIONS] directly. At the
 * bottom is a soft callout distinguishing habit-driven drinking (not a
 * thought) from thought-driven drinking (tracked here).
 */
@Composable
fun DistortionsLibraryScreen(
    onBack: () -> Unit,
    onOpenCheckIn: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.distortions_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.distortions_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )

        Spacer(Modifier.height(Spacing.s6))

        // Reading material, not a dashboard: no cards, just well-spaced text.
        COGNITIVE_DISTORTIONS.forEach { distortion ->
            DistortionEntry(distortion)
            Spacer(Modifier.height(Spacing.s8))
        }

        // "Not a thought?" callout — soft border, no fill, sitting apart from
        // the reading list so it reads as contextual guidance, not as another
        // entry in the taxonomy.
        HabitCallout(onOpenCheckIn = onOpenCheckIn)

        Spacer(Modifier.height(Spacing.s6))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.distortions_back)) }
        Spacer(Modifier.height(Spacing.s4))
    }
}

@Composable
private fun DistortionEntry(distortion: Distortion) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        // Name
        Text(
            text = stringResource(distortion.labelRes),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        // Definition
        Text(
            text = stringResource(distortion.definitionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = TideletTheme.extended.textSecondary,
        )
        // Example in italic \u2014 the string resources already include their own quotes.
        Text(
            text = stringResource(distortion.exampleRes),
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            color = TideletTheme.extended.textMuted,
        )
        Spacer(Modifier.height(Spacing.s1))
        // Technique — bold label prefix
        Text(
            text = buildString {
                append("Try: ")
                append(stringResource(distortion.techniqueRes))
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // Challenging question — italic
        Text(
            text = stringResource(distortion.challengeRes),
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            color = TideletTheme.extended.textMuted,
        )
    }
}

@Composable
private fun HabitCallout(onOpenCheckIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TideletTheme.extended.borderSubtle, Shapes.md)
            .padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(
            text = stringResource(R.string.distortion_habit_callout_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.distortion_habit_callout_body),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textSecondary,
        )
        TextButton(
            onClick = onOpenCheckIn,
            modifier = Modifier.padding(top = Spacing.s1),
        ) {
            Text(stringResource(R.string.distortion_habit_callout_cta))
        }
    }
}
