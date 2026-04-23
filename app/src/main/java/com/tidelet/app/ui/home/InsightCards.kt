package com.tidelet.app.ui.home

import androidx.annotation.StringRes
import com.tidelet.app.R

/**
 * Insight card inventory + selector (PHASE_2 §9B2).
 *
 * The deck is small and intentional — every card is short, calm, and
 * earns its slot. Two categories ship in v1:
 *   - QUOTE: a brief reflection from recovery literature or a related
 *     thinker. Authorless attribution; we keep tone neutral.
 *   - SCIENCE: one factual sentence about what changes in the body or
 *     brain after extended abstinence. Always hedged ("typically",
 *     "many people").
 *
 * USER_PATTERN cards (e.g. "your last three cravings were in the 6–8pm
 * window") are deferred — they require a separate analysis pipeline.
 * Selector falls back to the other categories whenever pattern data
 * isn't available, which is always in v1.
 */

enum class InsightCategory { QUOTE, SCIENCE, USER_PATTERN }

data class InsightCard(
    val key: String,
    val category: InsightCategory,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

/**
 * v1 deck. Keys are stable strings used by [InsightCardShownDao] to track
 * what's been surfaced — DO NOT renumber existing cards. Add new ones at
 * the end of each section.
 *
 * Target mix per spec: ~50% quotes, ~30% science, ~20% user-pattern.
 * The mix here is deliberately quote-heavy because we have no
 * USER_PATTERN cards yet; SCIENCE cards balance the deck without
 * requiring pattern data.
 */
val INSIGHT_CARDS: List<InsightCard> = listOf(
    // --- Quotes (~50%) ---
    InsightCard("q01_carl_jung", InsightCategory.QUOTE,
        R.string.insight_q01_title, R.string.insight_q01_body),
    InsightCard("q02_marcus_aurelius", InsightCategory.QUOTE,
        R.string.insight_q02_title, R.string.insight_q02_body),
    InsightCard("q03_aa_one_day", InsightCategory.QUOTE,
        R.string.insight_q03_title, R.string.insight_q03_body),
    InsightCard("q04_seneca", InsightCategory.QUOTE,
        R.string.insight_q04_title, R.string.insight_q04_body),
    InsightCard("q05_smart_recovery", InsightCategory.QUOTE,
        R.string.insight_q05_title, R.string.insight_q05_body),
    InsightCard("q06_proverb", InsightCategory.QUOTE,
        R.string.insight_q06_title, R.string.insight_q06_body),

    // --- Science (~30%) ---
    InsightCard("s01_sleep_rebound", InsightCategory.SCIENCE,
        R.string.insight_s01_title, R.string.insight_s01_body),
    InsightCard("s02_liver_recovery", InsightCategory.SCIENCE,
        R.string.insight_s02_title, R.string.insight_s02_body),
    InsightCard("s03_dopamine_baseline", InsightCategory.SCIENCE,
        R.string.insight_s03_title, R.string.insight_s03_body),
    InsightCard("s04_hippocampus", InsightCategory.SCIENCE,
        R.string.insight_s04_title, R.string.insight_s04_body),
)

/**
 * Pure selector — given the current state, returns either an [InsightCard]
 * to show or null when one shouldn't surface yet.
 *
 * Rules (PHASE_2 §9B2):
 *   1. Spacing — at least 2 days since the last shown card.
 *   2. Probability — only fire on ~30% of eligible Home opens.
 *   3. Recency — exclude cards last shown within the past 60 days.
 *   4. Variety — never the same card twice in a 60-day window.
 *
 * The function is deterministic given its inputs (incl. the random source),
 * which makes it cheap to unit-test the gating logic with a seeded RNG.
 */
fun selectInsightCard(
    nowMillis: Long,
    lastShownAtMillis: Long?,
    recentlyShownKeys: Set<String>,
    inventory: List<InsightCard> = INSIGHT_CARDS,
    random: () -> Double = { Math.random() },
    minSpacingMillis: Long = 2L * 24 * 60 * 60 * 1000,
    showProbability: Double = 0.30,
): InsightCard? {
    // Spacing rule: too soon since the last surfaced card.
    if (lastShownAtMillis != null && nowMillis - lastShownAtMillis < minSpacingMillis) {
        return null
    }
    // Probability rule: most opens skip the slot — keeps it feeling occasional.
    if (random() >= showProbability) return null

    // Recency rule: filter out anything shown within the recency window.
    val eligible = inventory.filterNot { it.key in recentlyShownKeys }
    if (eligible.isEmpty()) return null

    // Pick uniformly at random from the eligible deck. Could weight by
    // category later for a precise mix; for v1 a flat draw is fine.
    val idx = (random() * eligible.size).toInt().coerceIn(0, eligible.size - 1)
    return eligible[idx]
}
