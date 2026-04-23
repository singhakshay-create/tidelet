package com.tidelet.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.ui.home.companion.CompanionVisual
import com.tidelet.app.ui.theme.Shapes
import com.tidelet.app.ui.theme.Spacing
import com.tidelet.app.ui.theme.TideletTheme
import com.tidelet.app.util.milestoneLabel

@Composable
fun HomeScreen(
    onOpenSos: () -> Unit,
    onOpenWeeklyReflection: () -> Unit = {},
    onWriteLetter: (milestoneDays: Int) -> Unit = {},
    onOpenThoughtCheck: (id: Long) -> Unit = {},
    onOpenJournalEntry: (id: Long) -> Unit = {},
    onOpenEveningReview: () -> Unit = {},
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.s6, vertical = Spacing.s6),
    ) {
        // ---- Compassionate visual companion (PHASE_2 §9B1) ----
        // Opt-in, default off. Appears above the streak hero so it frames the
        // number as "held" by the ambient motif rather than competing with it.
        if (state.companionEnabled) {
            val reduceMotion = rememberReduceMotion()
            CompanionVisual(
                stage = state.companionStage,
                isAdvancing = state.companionIsAdvancing,
                reduceMotion = reduceMotion,
                modifier = Modifier.testTag("home_companion"),
            )
        }

        // ---- Day count — hero at the top, big and quiet ----

        Spacer(Modifier.height(Spacing.s12))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val days = state.streak.days
                Text(
                    text = days.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("home_streak_days"),
                )
                Spacer(Modifier.height(Spacing.s2))
                Text(
                    text = if (days == 1L) stringResourceOrEmpty(R.string.home_days_singular)
                           else stringResourceOrEmpty(R.string.home_days_plural),
                    style = MaterialTheme.typography.titleMedium,
                    color = TideletTheme.extended.textMuted,
                )
                if (state.streak.isWithinFirstDay) {
                    Spacer(Modifier.height(Spacing.s3))
                    Text(
                        text = "${state.streak.hours}h ${state.streak.minutes}m",
                        style = MaterialTheme.typography.titleMedium,
                        color = TideletTheme.extended.textMuted,
                    )
                }
                // Lifetime-dry-days subtitle (PHASE_2 §9A4). Only surfaces once
                // the lifetime total is strictly greater than the current
                // streak — i.e. the user has reset at least once — so a
                // never-reset user doesn't see a redundant "you've had N days
                // (same as your streak)" line.
                if (state.lifetimeDryDays > state.streak.days) {
                    Spacer(Modifier.height(Spacing.s3))
                    Text(
                        text = stringRes(
                            R.string.home_lifetime_total_subtitle,
                            state.lifetimeDryDays,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TideletTheme.extended.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("home_lifetime_total"),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.s8))

        // ---- Next milestone — a single line, no progress bar ----

        state.nextMilestone?.let { nm ->
            Column(modifier = Modifier.fillMaxWidth().testTag("home_next_milestone")) {
                Text(
                    text = "Next milestone — ${milestoneLabel(nm.days)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(Spacing.s1))
                Text(
                    text = "${nm.daysRemaining} to go",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TideletTheme.extended.textMuted,
                )
            }
        }

        // Sunday-evening weekly reflection card. Only rendered if today is
        // Sunday and the user hasn't already written one for this week —
        // HomeViewModel computes that and hands us a single boolean.
        if (state.showWeeklyReflectionCard) {
            Spacer(Modifier.height(24.dp))
            WeeklyReflectionCard(onOpen = onOpenWeeklyReflection)
        }

        // Evening mini-review entry button (PHASE_2 §9A5). Visible only when
        // the user opted in via Settings AND hasn't logged today.
        if (state.showEveningReviewButton) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenEveningReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_evening_review_button"),
            ) {
                Text(stringResourceOrEmpty(com.tidelet.app.R.string.home_evening_review_cta))
            }
        }

        // Milestone letter prompt — user reached a canonical milestone and
        // hasn't written a letter yet this session.
        state.pendingLetterPrompt?.let { days ->
            Spacer(Modifier.height(24.dp))
            MilestoneLetterPromptCard(
                milestoneDays = days,
                onWrite = { onWriteLetter(days) },
                onDismiss = { vm.dismissLetterPrompt(days) },
            )
        }

        // Milestone letter reveal — a previously-written letter surfaces on
        // Home when the user reaches the next milestone.
        state.letterToReveal?.let { letter ->
            Spacer(Modifier.height(24.dp))
            MilestoneLetterRevealCard(
                letter = letter,
                onDismiss = { vm.markLetterRevealed(letter.milestoneDays) },
            )
        }

        // "On this day" — an oldest matching thought/journal from a past month.
        // Dismissible and session-local; it reappears tomorrow with the next
        // anniversary if one exists.
        state.onThisDay?.let { item ->
            Spacer(Modifier.height(24.dp))
            OnThisDayCard(
                item = item,
                onOpen = {
                    when (item) {
                        is OnThisDayItem.Thought -> onOpenThoughtCheck(item.record.id)
                        is OnThisDayItem.Journal -> onOpenJournalEntry(item.entry.id)
                    }
                },
                onDismiss = { vm.dismissOnThisDay() },
            )
        }

        // Insight card (PHASE_2 §9B2) — variable-reward slot. Dismissible;
        // selector decides at most every 2 days, ~30% of eligible opens.
        state.insightCard?.let { card ->
            Spacer(Modifier.height(24.dp))
            InsightCardView(card = card, onDismiss = { vm.dismissInsightCard() })
        }

        // Fill the space above the SOS button
        Spacer(Modifier.weight(1f))

        // ---- SOS button — outlined pill with a small terracotta dot. Quieter
        //      than a solid coral slab; reads as "friend on call" not "DANGER".
        OutlinedButton(
            onClick = onOpenSos,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("home_sos_button"),
            shape = Shapes.full,
            border = BorderStroke(1.5.dp, TideletTheme.extended.sosAccent),
            contentPadding = PaddingValues(horizontal = Spacing.s6),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TideletTheme.extended.sosAccent),
            )
            Spacer(Modifier.width(Spacing.s2))
            Text(
                text = stringResourceOrEmpty(R.string.home_sos_button),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Spacing.s3))
    }
}

/**
 * Tiny wrapper so callers don't each need to import `stringResource`.
 * Keeps the screen files focused on layout.
 */
@Composable
private fun stringResourceOrEmpty(resId: Int): String =
    androidx.compose.ui.res.stringResource(id = resId)

/**
 * Reads Android's "remove animations" accessibility setting. When the user
 * has system animations disabled (`ANIMATOR_DURATION_SCALE = 0`), the
 * companion's draw functions collapse tweened motion to resting values
 * instead of throttling cycles. Cheap — one settings lookup, remembered.
 */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

@Composable
private fun stringRes(resId: Int, vararg args: Any): String =
    androidx.compose.ui.res.stringResource(id = resId, *args)

/**
 * PHASE_2 §9B2 — variable-reward insight card. Quiet styling (surfaceVariant
 * fill, no leading icon, single dismiss action). Body is the substance;
 * title is just a category-style header. Designed to feel like a thoughtful
 * note rather than an achievement.
 */
@Composable
private fun InsightCardView(card: InsightCard, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_insight_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResourceOrEmpty(card.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResourceOrEmpty(card.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text(stringResourceOrEmpty(com.tidelet.app.R.string.home_insight_dismiss))
            }
        }
    }
}

@Composable
private fun MilestoneLetterPromptCard(
    milestoneDays: Int,
    onWrite: () -> Unit,
    onDismiss: () -> Unit,
) {
    val label = com.tidelet.app.util.milestoneLabel(milestoneDays)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringRes(com.tidelet.app.R.string.home_letter_prompt_title, label),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResourceOrEmpty(com.tidelet.app.R.string.home_letter_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = onWrite) {
                    Text(stringResourceOrEmpty(com.tidelet.app.R.string.home_letter_prompt_cta))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResourceOrEmpty(com.tidelet.app.R.string.home_letter_prompt_dismiss))
                }
            }
        }
    }
}

@Composable
private fun MilestoneLetterRevealCard(
    letter: com.tidelet.app.data.db.MilestoneLetter,
    onDismiss: () -> Unit,
) {
    val label = com.tidelet.app.util.milestoneLabel(letter.milestoneDays)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringRes(com.tidelet.app.R.string.home_letter_reveal_title, label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = letter.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text(stringResourceOrEmpty(com.tidelet.app.R.string.home_letter_reveal_dismiss))
            }
        }
    }
}

/**
 * "On this day" anniversary card — surfaces an oldest past ThoughtRecord or
 * JournalEntry whose date matches today's day-of-month and is ≥ 1 calendar
 * month in the past. Tapping the card navigates into the detail screen;
 * dismiss hides it for the rest of the session.
 */
@Composable
private fun OnThisDayCard(
    item: OnThisDayItem,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (item) {
        is OnThisDayItem.Thought ->
            stringResourceOrEmpty(R.string.home_on_this_day_title_thought)
        is OnThisDayItem.Journal ->
            stringResourceOrEmpty(R.string.home_on_this_day_title_journal)
    }
    val monthsAgo = stringRes(R.string.home_on_this_day_months_ago, item.monthsAgo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_on_this_day_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$title · $monthsAgo",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.preview.take(140),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = onOpen) {
                    Text(stringResourceOrEmpty(R.string.home_on_this_day_open))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResourceOrEmpty(R.string.home_on_this_day_dismiss))
                }
            }
        }
    }
}

/**
 * The low-pressure Sunday prompt. Small card with two lines of copy and one
 * primary action — matches the "don't harangue the user" tone we set with
 * the rest of Home. The card itself gates on the VM (isSunday AND no row
 * yet), so it simply disappears once the user has reflected.
 */
@Composable
private fun WeeklyReflectionCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_weekly_reflection_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResourceOrEmpty(R.string.home_weekly_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResourceOrEmpty(R.string.home_weekly_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpen) {
                Text(stringResourceOrEmpty(R.string.home_weekly_cta))
            }
        }
    }
}
