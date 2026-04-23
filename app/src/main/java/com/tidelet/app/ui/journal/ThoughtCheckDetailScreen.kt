package com.tidelet.app.ui.journal

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.ui.cbt.distortionByKey
import com.tidelet.app.ui.theme.Shapes
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Read-only detail view of a ThoughtRecord. Four sections mirror the four
 * prompts the user saw when writing: situation, thought, challenge, friend
 * reframe. A small distortion chip sits up top if tagged.
 */
@Composable
fun ThoughtCheckDetailScreen(
    onBack: () -> Unit,
    vm: ThoughtCheckDetailViewModel = viewModel(factory = ThoughtCheckDetailViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        when {
            !state.loaded -> Unit
            state.notFound -> NotFoundBody(onBack = onBack)
            else -> DetailBody(record = state.record!!, onBack = onBack)
        }
    }
}

@Composable
private fun DetailBody(record: ThoughtRecord, onBack: () -> Unit) {
    // Header: date + distortion chip
    Text(
        text = formatDate(record.dateIso),
        style = MaterialTheme.typography.bodySmall,
        color = TideletTheme.extended.textMuted,
    )
    Spacer(Modifier.height(Spacing.s2))

    val distortion = record.distortionTag?.let { distortionByKey(it) }
    if (distortion != null) {
        DistortionChip(labelRes = distortion.labelRes)
        Spacer(Modifier.height(Spacing.s6))
    } else {
        Spacer(Modifier.height(Spacing.s4))
    }

    PromptSection(
        prompt = stringResource(R.string.thought_check_detail_situation),
        body = record.situation,
    )
    Spacer(Modifier.height(Spacing.s6))
    PromptSection(
        prompt = stringResource(R.string.thought_check_detail_thought),
        body = record.thought,
    )
    Spacer(Modifier.height(Spacing.s6))
    PromptSection(
        prompt = stringResource(R.string.thought_check_detail_challenge),
        body = record.challenge,
    )
    Spacer(Modifier.height(Spacing.s6))
    PromptSection(
        prompt = stringResource(R.string.thought_check_detail_friend_reframe),
        body = record.friendReframe,
    )

    Spacer(Modifier.height(Spacing.s8))

    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_back))
    }
    Spacer(Modifier.height(Spacing.s4))
}

@Composable
private fun PromptSection(prompt: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TideletTheme.extended.textSecondary,
        )
        if (body.isBlank()) {
            Text(
                text = stringResource(R.string.detail_empty_field),
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = TideletTheme.extended.textMuted,
            )
        } else {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DistortionChip(labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = TideletTheme.extended.textSecondary,
        modifier = Modifier
            .border(1.dp, TideletTheme.extended.borderSubtle, Shapes.full)
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    )
}

@Composable
private fun NotFoundBody(onBack: () -> Unit) {
    Text(
        text = stringResource(R.string.detail_not_found),
        style = MaterialTheme.typography.bodyLarge,
        color = TideletTheme.extended.textMuted,
    )
    Spacer(Modifier.height(Spacing.s6))
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.common_back))
    }
}

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
} catch (_: Exception) {
    iso
}
