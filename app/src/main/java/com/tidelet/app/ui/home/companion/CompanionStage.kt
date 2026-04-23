package com.tidelet.app.ui.home.companion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compassionate visual companion (PHASE_2 §9B1).
 *
 * This file ships **three drafts** — TidePool, Lantern, Geometric — behind a
 * compile-time flag ([ACTIVE_COMPANION_OPTION]). Reviewers flip the flag,
 * take screenshots at stages 1/3/5 for each option, and pick one. After the
 * side-by-side review, the two losing options and the flag itself are
 * deleted; the winning Composable is wired in directly.
 *
 * Shared contract every option implements. `stage` is 1..5; `isAdvancing`
 * is true for the one emission immediately after the stage bumps past its
 * historical high-water mark, which options can use to drive a subtle
 * one-shot accent.
 */
@Composable
fun CompanionVisual(
    stage: Int,
    isAdvancing: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val clampedStage = stage.coerceIn(1, 5)
    when (ACTIVE_COMPANION_OPTION) {
        CompanionOption.TIDE_POOL -> TidePoolCompanion(
            stage = clampedStage,
            isAdvancing = isAdvancing,
            reduceMotion = reduceMotion,
            modifier = modifier,
        )
        CompanionOption.LANTERN -> LanternCompanion(
            stage = clampedStage,
            isAdvancing = isAdvancing,
            reduceMotion = reduceMotion,
            modifier = modifier,
        )
        CompanionOption.GEOMETRIC -> GeometricCompanion(
            stage = clampedStage,
            isAdvancing = isAdvancing,
            reduceMotion = reduceMotion,
            modifier = modifier,
        )
    }
}

/** Which of the three draft options the review build renders. */
enum class CompanionOption { TIDE_POOL, LANTERN, GEOMETRIC }

/**
 * Flip this for the side-by-side review. Once a decision is reached, delete
 * the two losing option files + this constant and inline the winning
 * Composable call in HomeScreen.
 *
 * Spec default if no clear preference emerges: [CompanionOption.LANTERN] —
 * lowest design surface area, least risk.
 */
val ACTIVE_COMPANION_OPTION: CompanionOption = CompanionOption.LANTERN

// ------------------------------------------------------------------
// Pure stage progression
// ------------------------------------------------------------------

/**
 * Inputs that drive the companion's stage. Both knobs move it forward;
 * whichever is more advanced wins, so the visual can keep climbing even
 * after a streak reset if the user is still showing up for check-ins.
 */
data class CompanionInputs(
    val streakDays: Long,
    val checkInsLast14d: Int,
    val highestStageEverReached: Int,
)

/**
 * The stage the companion should render at, given [inputs].
 *
 * Spec formula (§9B1): `stage = (1..5).coerceIn(1 + max(streakDays / 14,
 * checkInsLast14d / 3))`. Stage 1 from day 0; stage 5 at 56 dry days or
 * 12 check-ins in last 14 days, whichever comes first.
 *
 * The return value respects the high-water mark — a reset can never drop
 * the rendered stage below what was previously achieved.
 */
fun computeCompanionStage(inputs: CompanionInputs): Int {
    val streakStage = 1 + (inputs.streakDays / 14).toInt()
    val checkInStage = 1 + (inputs.checkInsLast14d / 3)
    val raw = maxOf(streakStage, checkInStage).coerceIn(1, 5)
    return maxOf(raw, inputs.highestStageEverReached.coerceIn(1, 5))
}

/**
 * "Raw" stage — ignores the high-water mark. Useful for deciding whether
 * the stored mark needs to be raised (`raw > stored` → persist), and for
 * computing [isAdvancing] flashes.
 */
fun computeRawCompanionStage(streakDays: Long, checkInsLast14d: Int): Int {
    val streakStage = 1 + (streakDays / 14).toInt()
    val checkInStage = 1 + (checkInsLast14d / 3)
    return maxOf(streakStage, checkInStage).coerceIn(1, 5)
}
