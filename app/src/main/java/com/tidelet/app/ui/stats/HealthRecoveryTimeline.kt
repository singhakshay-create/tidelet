package com.tidelet.app.ui.stats

import androidx.annotation.StringRes
import com.tidelet.app.R

/**
 * Health-recovery timeline (PHASE_2 §9A1).
 *
 * Pins a fixed set of alcohol-recovery science milestones to the user's day
 * count so "day 14" carries meaning beyond a number. Copy is deliberately
 * hedged (typical / commonly / many people) — Tidelet is not a clinician.
 *
 * The data-layer impact is zero: milestones are constants, and the UI card
 * re-derives state on every recomposition via [selectHealthRecoveryState].
 */

data class HealthRecoveryMilestone(
    val days: Int,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

/**
 * Six canonical milestones, in ascending order. Chosen to match the "most
 * widely cited" recovery benchmarks in the AUD literature — each one has a
 * body copy that describes what *typically* happens around that point.
 */
val HEALTH_RECOVERY_MILESTONES: List<HealthRecoveryMilestone> = listOf(
    HealthRecoveryMilestone(
        days = 1,
        titleRes = R.string.health_recovery_24h_title,
        bodyRes = R.string.health_recovery_24h_body,
    ),
    HealthRecoveryMilestone(
        days = 3,
        titleRes = R.string.health_recovery_72h_title,
        bodyRes = R.string.health_recovery_72h_body,
    ),
    HealthRecoveryMilestone(
        days = 14,
        titleRes = R.string.health_recovery_2w_title,
        bodyRes = R.string.health_recovery_2w_body,
    ),
    HealthRecoveryMilestone(
        days = 30,
        titleRes = R.string.health_recovery_30d_title,
        bodyRes = R.string.health_recovery_30d_body,
    ),
    HealthRecoveryMilestone(
        days = 90,
        titleRes = R.string.health_recovery_90d_title,
        bodyRes = R.string.health_recovery_90d_body,
    ),
    HealthRecoveryMilestone(
        days = 365,
        titleRes = R.string.health_recovery_1yr_title,
        bodyRes = R.string.health_recovery_1yr_body,
    ),
)

/**
 * Snapshot of the user's position along the timeline.
 *
 * [reached] is the highest-[days] milestone that the user has passed or hit
 * (null when they haven't reached the first one yet — i.e. streak = 0).
 * [upcoming] is the remaining milestones ahead, with their days-to-go; empty
 * once the user has passed the final (1 year) milestone.
 */
data class HealthRecoveryState(
    val reached: HealthRecoveryMilestone?,
    val upcoming: List<UpcomingMilestone>,
)

data class UpcomingMilestone(
    val milestone: HealthRecoveryMilestone,
    val daysUntil: Long,
)

/**
 * Pure function — given the current streak in days, return which milestone
 * is currently highlighted and which ones are still ahead. Easy to unit-test.
 */
fun selectHealthRecoveryState(streakDays: Long): HealthRecoveryState {
    val reached = HEALTH_RECOVERY_MILESTONES.lastOrNull { it.days <= streakDays }
    val upcoming = HEALTH_RECOVERY_MILESTONES
        .filter { it.days > streakDays }
        .map { UpcomingMilestone(it, (it.days - streakDays).coerceAtLeast(0L)) }
    return HealthRecoveryState(reached = reached, upcoming = upcoming)
}
