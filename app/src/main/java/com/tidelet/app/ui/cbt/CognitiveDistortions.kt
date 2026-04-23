package com.tidelet.app.ui.cbt

import androidx.annotation.StringRes
import com.tidelet.app.R

/**
 * Catalogue of common cognitive distortions, tailored for alcohol use.
 *
 * Ten entries — large enough to cover the high-frequency patterns seen in
 * CBT-for-alcohol case formulations (Kadden et al., Marlatt's relapse-prevention
 * model) without overwhelming a user who is mid-craving. Each entry has a
 * one-line definition, a drinking-flavoured example, a short reframe technique,
 * and one challenging question the user can ask themselves.
 *
 * [key] is the stable identifier written to [ThoughtRecord.distortionTag].
 * It must never change without a data migration; labels/definitions/examples/
 * techniques/questions are i18n-friendly via string resources.
 *
 * "Habit" is deliberately NOT a distortion — see the bottom of
 * DistortionsLibraryScreen for the soft callout that distinguishes
 * habit-driven drinking (tracked as a CheckInTrigger) from thought-driven
 * drinking (tracked here).
 */
data class Distortion(
    /** Stable identifier — saved to the DB. Never change. */
    val key: String,
    @StringRes val labelRes: Int,
    @StringRes val definitionRes: Int,
    @StringRes val exampleRes: Int,
    @StringRes val techniqueRes: Int,
    @StringRes val challengeRes: Int,
)

val COGNITIVE_DISTORTIONS: List<Distortion> = listOf(
    Distortion(
        key = "all_or_nothing",
        labelRes = R.string.distortion_all_or_nothing,
        definitionRes = R.string.distortion_all_or_nothing_def,
        exampleRes = R.string.distortion_all_or_nothing_ex,
        techniqueRes = R.string.distortion_all_or_nothing_technique,
        challengeRes = R.string.distortion_all_or_nothing_question,
    ),
    Distortion(
        key = "permission_giving",
        labelRes = R.string.distortion_permission_giving,
        definitionRes = R.string.distortion_permission_giving_def,
        exampleRes = R.string.distortion_permission_giving_ex,
        techniqueRes = R.string.distortion_permission_giving_technique,
        challengeRes = R.string.distortion_permission_giving_question,
    ),
    Distortion(
        key = "emotional_reasoning",
        labelRes = R.string.distortion_emotional_reasoning,
        definitionRes = R.string.distortion_emotional_reasoning_def,
        exampleRes = R.string.distortion_emotional_reasoning_ex,
        techniqueRes = R.string.distortion_emotional_reasoning_technique,
        challengeRes = R.string.distortion_emotional_reasoning_question,
    ),
    Distortion(
        key = "fortune_telling",
        labelRes = R.string.distortion_fortune_telling,
        definitionRes = R.string.distortion_fortune_telling_def,
        exampleRes = R.string.distortion_fortune_telling_ex,
        techniqueRes = R.string.distortion_fortune_telling_technique,
        challengeRes = R.string.distortion_fortune_telling_question,
    ),
    Distortion(
        key = "minimizing",
        labelRes = R.string.distortion_minimizing,
        definitionRes = R.string.distortion_minimizing_def,
        exampleRes = R.string.distortion_minimizing_ex,
        techniqueRes = R.string.distortion_minimizing_technique,
        challengeRes = R.string.distortion_minimizing_question,
    ),
    Distortion(
        key = "magnifying",
        labelRes = R.string.distortion_magnifying,
        definitionRes = R.string.distortion_magnifying_def,
        exampleRes = R.string.distortion_magnifying_ex,
        techniqueRes = R.string.distortion_magnifying_technique,
        challengeRes = R.string.distortion_magnifying_question,
    ),
    Distortion(
        key = "should_ing",
        labelRes = R.string.distortion_should_ing,
        definitionRes = R.string.distortion_should_ing_def,
        exampleRes = R.string.distortion_should_ing_ex,
        techniqueRes = R.string.distortion_should_ing_technique,
        challengeRes = R.string.distortion_should_ing_question,
    ),
    Distortion(
        key = "mental_filter",
        labelRes = R.string.distortion_mental_filter,
        definitionRes = R.string.distortion_mental_filter_def,
        exampleRes = R.string.distortion_mental_filter_ex,
        techniqueRes = R.string.distortion_mental_filter_technique,
        challengeRes = R.string.distortion_mental_filter_question,
    ),
    Distortion(
        key = "personalization",
        labelRes = R.string.distortion_personalization,
        definitionRes = R.string.distortion_personalization_def,
        exampleRes = R.string.distortion_personalization_ex,
        techniqueRes = R.string.distortion_personalization_technique,
        challengeRes = R.string.distortion_personalization_question,
    ),
    Distortion(
        key = "labeling",
        labelRes = R.string.distortion_labeling,
        definitionRes = R.string.distortion_labeling_def,
        exampleRes = R.string.distortion_labeling_ex,
        techniqueRes = R.string.distortion_labeling_technique,
        challengeRes = R.string.distortion_labeling_question,
    ),
)

/** Lookup by key — O(n) but n=10 and only called at UI time. */
fun distortionByKey(key: String?): Distortion? =
    key?.let { k -> COGNITIVE_DISTORTIONS.firstOrNull { it.key == k } }
