package com.tidelet.app.ui.home.companion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure tests for the stage-progression math (PHASE_2 §9B1).
 *
 * Covers the three spec acceptance criteria:
 *   - "never decreases when streak resets" — high-water mark holds.
 *   - "high-water mark persists across cold starts" — exercised at the
 *     prefs layer (see UserPreferencesTest), smoke-checked here via the
 *     function's read of the input.
 *   - "reduce-motion path skips animation" — verified at the composable
 *     level by visual review; the stage math is identical.
 */
class CompanionStageTest {

    @Test
    fun `day zero with no check-ins starts at stage 1`() {
        val stage = computeCompanionStage(
            CompanionInputs(
                streakDays = 0L,
                checkInsLast14d = 0,
                highestStageEverReached = 1,
            )
        )
        assertThat(stage).isEqualTo(1)
    }

    @Test
    fun `streak driver — stage bumps every 14 days`() {
        // Spec formula: stage = 1 + streakDays / 14.
        // 0–13 → 1, 14–27 → 2, 28–41 → 3, 42–55 → 4, 56+ → 5.
        listOf(
            0L to 1, 13L to 1,
            14L to 2, 27L to 2,
            28L to 3,
            42L to 4,
            56L to 5, 400L to 5,
        ).forEach { (days, expected) ->
            val stage = computeCompanionStage(
                CompanionInputs(streakDays = days, checkInsLast14d = 0, highestStageEverReached = 1)
            )
            assertThat(stage).isEqualTo(expected)
        }
    }

    @Test
    fun `check-in driver — stage advances every 3 check-ins in 14d`() {
        // Spec formula: stage = 1 + checkInsLast14d / 3.
        // 12+ / 14d = stage 5, even if streak is 0 (post-reset).
        val stage = computeCompanionStage(
            CompanionInputs(streakDays = 0L, checkInsLast14d = 12, highestStageEverReached = 1)
        )
        assertThat(stage).isEqualTo(5)
    }

    @Test
    fun `the higher of the two drivers wins`() {
        // streak alone → 1; check-ins alone → 3; combined → 3.
        val stage = computeCompanionStage(
            CompanionInputs(streakDays = 0L, checkInsLast14d = 6, highestStageEverReached = 1)
        )
        assertThat(stage).isEqualTo(3)
    }

    @Test
    fun `never decreases when streak resets — high-water mark holds`() {
        // User hit stage 4 once, then reset to day 0 with no recent check-ins.
        // Raw drivers say stage 1; rendered stage must stay at 4.
        val stage = computeCompanionStage(
            CompanionInputs(
                streakDays = 0L,
                checkInsLast14d = 0,
                highestStageEverReached = 4,
            )
        )
        assertThat(stage).isEqualTo(4)
    }

    @Test
    fun `raw stage ignores the high-water mark`() {
        // Useful for "should we raise the mark?" decisions: compare
        // raw > stored. Raw does NOT see the mark, so a reset after a
        // previous stage 4 high reads as raw=1.
        assertThat(computeRawCompanionStage(streakDays = 0L, checkInsLast14d = 0))
            .isEqualTo(1)
    }

    @Test
    fun `stage is clamped to 1 through 5`() {
        // Huge input mustn't blow past the top of the scale.
        val stage = computeCompanionStage(
            CompanionInputs(
                streakDays = 10_000L,
                checkInsLast14d = 1_000,
                highestStageEverReached = 99,
            )
        )
        assertThat(stage).isEqualTo(5)
    }
}
