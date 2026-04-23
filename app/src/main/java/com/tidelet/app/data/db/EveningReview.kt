package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A one-question-pair evening reflection (PHASE_2 §9A5). The user writes one
 * "win" and one "challenge" for the day; both are free text, both are
 * optional beyond a minimum non-blank check, and there is exactly one row
 * per day — the primary key is the ISO date.
 *
 * Opt-in only (controlled by `UserProfile.eveningReviewEnabled`); Home shows
 * the entry button only when the user has opted in and hasn't yet logged one
 * for today.
 *
 * Keeping `dateIso` as the PK (rather than an auto-id + index) mirrors the
 * [CheckIn] pattern and lets us upsert without extra existence checks.
 */
@Entity(tableName = "evening_review")
data class EveningReview(
    /** ISO "YYYY-MM-DD" in local time. Also the primary key. */
    @PrimaryKey val dateIso: String,
    val winText: String,
    val challengeText: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
