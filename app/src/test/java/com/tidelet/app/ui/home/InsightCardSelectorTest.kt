package com.tidelet.app.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-JVM tests for the spaced + probabilistic insight-card selector
 * (PHASE_2 §9B2). All randomness is supplied via the [random] lambda so
 * the rules are deterministically observable.
 */
class InsightCardSelectorTest {

    private val now = 100L * 24 * 60 * 60 * 1000  // arbitrary epoch ms anchor
    private val sample = listOf(
        InsightCard("a", InsightCategory.QUOTE, 1, 2),
        InsightCard("b", InsightCategory.SCIENCE, 3, 4),
        InsightCard("c", InsightCategory.QUOTE, 5, 6),
    )

    @Test
    fun `returns null when within the spacing window`() {
        val recent = now - (24L * 60 * 60 * 1000)  // 1 day ago
        val pick = selectInsightCard(
            nowMillis = now,
            lastShownAtMillis = recent,
            recentlyShownKeys = emptySet(),
            inventory = sample,
            random = { 0.0 },  // probability check would otherwise pass
        )
        assertThat(pick).isNull()
    }

    @Test
    fun `returns null when probability gate trips`() {
        val pick = selectInsightCard(
            nowMillis = now,
            lastShownAtMillis = null,  // no prior — spacing rule passes
            recentlyShownKeys = emptySet(),
            inventory = sample,
            random = { 0.99 },  // > showProbability of 0.30
        )
        assertThat(pick).isNull()
    }

    @Test
    fun `picks an eligible card past spacing and probability`() {
        val pick = selectInsightCard(
            nowMillis = now,
            lastShownAtMillis = null,
            recentlyShownKeys = emptySet(),
            inventory = sample,
            random = { 0.05 },  // first call: probability passes; second: index 0 of 3
        )
        assertThat(pick).isNotNull()
        assertThat(pick!!.key).isEqualTo("a")
    }

    @Test
    fun `excludes recently shown cards`() {
        val pick = selectInsightCard(
            nowMillis = now,
            lastShownAtMillis = null,
            recentlyShownKeys = setOf("a", "c"),
            inventory = sample,
            random = { 0.05 },  // index 0 → "b" after filtering
        )
        assertThat(pick!!.key).isEqualTo("b")
    }

    @Test
    fun `returns null when every card is recently shown`() {
        val pick = selectInsightCard(
            nowMillis = now,
            lastShownAtMillis = null,
            recentlyShownKeys = setOf("a", "b", "c"),
            inventory = sample,
            random = { 0.05 },
        )
        assertThat(pick).isNull()
    }

    @Test
    fun `inventory is non-empty and key-stable`() {
        // Guards against accidental key edits — the keys are persisted, so
        // changing them would orphan rows in the InsightCardShown table.
        val keys = INSIGHT_CARDS.map { it.key }
        assertThat(keys).contains("q01_carl_jung")
        assertThat(keys).contains("s01_sleep_rebound")
        // No duplicates — duplicate keys would break the recency filter.
        assertThat(keys).containsNoDuplicates()
    }
}
