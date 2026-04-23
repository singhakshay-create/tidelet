package com.tidelet.app.ui.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the pure selector that drives the PHASE_2 §9A1 card.
 *
 * Only covers the function — UI-level concerns (compose render, padding,
 * colours) sit outside this file and are exercised manually on device.
 */
class HealthRecoveryTimelineTest {

    @Test
    fun `day zero — no milestone reached, first upcoming at 1 day`() {
        val state = selectHealthRecoveryState(streakDays = 0L)

        assertThat(state.reached).isNull()
        assertThat(state.upcoming).hasSize(6)
        val first = state.upcoming.first()
        assertThat(first.milestone.days).isEqualTo(1)
        assertThat(first.daysUntil).isEqualTo(1L)
    }

    @Test
    fun `selects latest reached milestone`() {
        // Streak = 20 → 24h, 72h, 2w all reached; 2 weeks is the most recent.
        val state = selectHealthRecoveryState(streakDays = 20L)

        assertThat(state.reached).isNotNull()
        assertThat(state.reached!!.days).isEqualTo(14)
        // Upcoming list is strictly future — 30d, 90d, 1yr.
        assertThat(state.upcoming.map { it.milestone.days }).containsExactly(30, 90, 365).inOrder()
    }

    @Test
    fun `computes daysUntil for each upcoming milestone`() {
        val state = selectHealthRecoveryState(streakDays = 20L)

        val byDay = state.upcoming.associate { it.milestone.days to it.daysUntil }
        assertThat(byDay[30]).isEqualTo(10L)
        assertThat(byDay[90]).isEqualTo(70L)
        assertThat(byDay[365]).isEqualTo(345L)
    }

    @Test
    fun `highlights the exact-match milestone on its day`() {
        // Streak exactly 30 → 30d is reached, upcoming starts at 90d.
        val state = selectHealthRecoveryState(streakDays = 30L)

        assertThat(state.reached!!.days).isEqualTo(30)
        assertThat(state.upcoming.first().milestone.days).isEqualTo(90)
        assertThat(state.upcoming.first().daysUntil).isEqualTo(60L)
    }

    @Test
    fun `streak past the final milestone has empty upcoming`() {
        val state = selectHealthRecoveryState(streakDays = 400L)

        assertThat(state.reached!!.days).isEqualTo(365)
        assertThat(state.upcoming).isEmpty()
    }

    @Test
    fun `all six canonical milestones are present in order`() {
        val days = HEALTH_RECOVERY_MILESTONES.map { it.days }
        assertThat(days).containsExactly(1, 3, 14, 30, 90, 365).inOrder()
    }
}
