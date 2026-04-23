package com.tidelet.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.util.milestoneLabel
import java.text.NumberFormat
import java.util.Locale

/**
 * Stats / insights tab.
 *
 * Four small cards, all computed on-device:
 *   1. Days since start (hero metric — same number Home leads with)
 *   2. Drink-free days this month + total
 *   3. Money saved (estimated from the user's typical drinks/day × $/drink)
 *   4. Next milestone (with a progress bar)
 *
 * No graphs, no charts, no trend lines. The benchmarking doc against Reframe
 * calls out "too much" as the #1 UX complaint in this category — restraint is
 * the competitive position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vm: StatsViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_stats)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            DaysSinceCard(days = state.streak?.days ?: 0L)

            Spacer(Modifier.height(12.dp))

            // Health-recovery timeline (PHASE_2 §9A1) sits above the money
            // card — the science of "what's happening in my body" lands as
            // more motivating than dollars for users deeper into a streak.
            HealthRecoveryCard(streakDays = state.streak?.days ?: 0L)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DrinkFreeCard(
                    thisMonth = state.drinkFreeDaysThisMonth,
                    total = state.drinkFreeDaysTotal,
                    modifier = Modifier.weight(1f),
                )
                MoneySavedCard(
                    cents = state.moneySavedCents,
                    typicalPerDay = state.typicalDrinksPerDay,
                    priceCents = state.priceCentsPerDrink,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Hours reclaimed (PHASE_2 §9A2). Sits on its own row rather than
            // squeezed into the money/drink-free row — the "≈ X hours"
            // framing deserves room to breathe.
            HoursReclaimedCard(
                hours = state.hoursReclaimed,
                typicalPerDay = state.typicalDrinksPerDay,
                hoursPerDrink = state.hoursPerDrink,
            )

            Spacer(Modifier.height(12.dp))

            NextMilestoneCard(
                nextDays = state.nextMilestone?.days,
                daysRemaining = state.nextMilestone?.daysRemaining,
                currentDays = state.streak?.days ?: 0L,
            )

            Spacer(Modifier.height(12.dp))

            // Gentle footer explaining the estimate. Not a disclaimer —
            // honesty about where the number comes from builds trust.
            Text(
                text = stringResource(R.string.stats_footer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Cards ----

@Composable
private fun DaysSinceCard(days: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.stats_days_since_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = days.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    if (days == 1L) R.string.home_days_singular else R.string.home_days_plural,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun DrinkFreeCard(
    thisMonth: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    SmallStatCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.stats_drink_free_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = thisMonth.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.stats_this_month),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stats_total_fmt, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoneySavedCard(
    cents: Long,
    typicalPerDay: Int,
    priceCents: Int,
    modifier: Modifier = Modifier,
) {
    SmallStatCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.stats_money_saved_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatCentsAsDollars(cents),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.stats_money_saved_assumption,
                typicalPerDay,
                formatCentsAsDollars(priceCents.toLong()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NextMilestoneCard(
    nextDays: Int?,
    daysRemaining: Int?,
    currentDays: Long,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.stats_next_milestone_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (nextDays == null || daysRemaining == null) {
                // No next milestone means the user has passed the final one (1 year).
                Text(
                    text = stringResource(R.string.stats_all_milestones_cleared),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.stats_next_milestone_headline,
                        milestoneLabel(nextDays),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.stats_next_milestone_sub,
                        daysRemaining,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                // Progress = current / nextDays, clamped. currentDays can exceed
                // nextDays briefly between tick and recomputation, so coerce.
                val progress = (currentDays.toFloat() / nextDays.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Hours-reclaimed card (PHASE_2 §9A2). Full-width card sitting below the
 * money/drink-free row. Number prefixed with "≈" to make the estimate
 * explicit; subcopy explains the assumption the same way the money card
 * does for its typical-price input.
 */
@Composable
private fun HoursReclaimedCard(hours: Long, typicalPerDay: Int, hoursPerDrink: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.stats_hours_reclaimed_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.stats_hours_reclaimed_value, hours),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.stats_hours_reclaimed_assumption,
                    typicalPerDay,
                    hoursPerDrink,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Health-recovery timeline card (PHASE_2 §9A1). Highlights the most-recent
 * reached milestone in a filled Text style, and lists upcoming milestones
 * dim with days-to-go. Hedged-language footer reminds the user these are
 * typical patterns, not guarantees.
 */
@Composable
private fun HealthRecoveryCard(streakDays: Long) {
    val snapshot = selectHealthRecoveryState(streakDays)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.health_recovery_card_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (snapshot.reached == null) {
                // Day 0 — the user hasn't hit the first milestone yet. Keep
                // the card quiet; no "you haven't done anything" framing.
                Text(
                    text = stringResource(R.string.health_recovery_not_reached),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                // The reached row is rendered at full strength — title bold,
                // body in body-medium, onSurface colour.
                Text(
                    text = stringResource(snapshot.reached.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(snapshot.reached.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Upcoming rows render dim — bodyMedium/onSurfaceVariant — with
            // the days-to-go suffix so the user can eyeball "what's next".
            snapshot.upcoming.forEach { upcoming ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(upcoming.milestone.titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.health_recovery_days_until,
                        upcoming.daysUntil,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.health_recovery_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shared shell for the two half-width stat cards. */
@Composable
private fun SmallStatCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 20.dp)),
        ) {
            content()
        }
    }
}

/**
 * Locale-aware currency formatting. Produces "$142" in en-US, "₹250" in en-IN,
 * "€8" in de-DE, etc. No decimals — v1 deals in whole-unit drink prices.
 *
 * Renamed from the old USD-only helper; call sites should keep working via the
 * old name, so we keep it as an alias below.
 */
private fun formatCentsInLocale(cents: Long): String {
    val whole = cents / 100
    val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return fmt.format(whole)
}

/** Backwards-compat alias — old call sites keep working. */
private fun formatCentsAsDollars(cents: Long): String = formatCentsInLocale(cents)
