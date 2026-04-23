package com.tidelet.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.data.db.EveningReview
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Read-only list of past evening mini-reviews (PHASE_2 §9A5).
 *
 * Each row inlines both fields ("Win" / "Challenge"). No separate detail
 * screen — the data is short enough to read at a glance, and the read-side
 * is meant for scanning, not editing.
 */
@Composable
fun EveningReviewsListScreen(
    vm: EveningReviewsListViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        Text(
            text = stringResource(R.string.evening_reviews_list_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(Spacing.s2))
        Text(
            text = stringResource(R.string.evening_reviews_list_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TideletTheme.extended.textMuted,
        )
        Spacer(Modifier.height(Spacing.s6))

        when {
            !state.loaded -> Unit
            state.isEmpty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.evening_reviews_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(Spacing.s2))
                    Text(
                        text = stringResource(R.string.evening_reviews_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideletTheme.extended.textMuted,
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("evening_reviews_list"),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                items(
                    items = state.entries,
                    key = { it.dateIso },
                ) { review ->
                    EveningReviewRow(review)
                }
            }
        }
    }
}

@Composable
private fun EveningReviewRow(review: EveningReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Text(
            text = formatDate(review.dateIso),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TideletTheme.extended.textSecondary,
        )
        if (review.winText.isNotBlank()) {
            Text(
                text = stringResource(R.string.evening_reviews_row_win, review.winText),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (review.challengeText.isNotBlank()) {
            Text(
                text = stringResource(R.string.evening_reviews_row_challenge, review.challengeText),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        HorizontalDivider(
            color = TideletTheme.extended.borderSubtle,
            modifier = Modifier.padding(top = Spacing.s2),
        )
    }
}

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
} catch (_: Exception) {
    iso
}
