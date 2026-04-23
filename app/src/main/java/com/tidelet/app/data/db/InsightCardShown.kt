package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records that a particular insight card was surfaced on Home (PHASE_2 §9B2).
 *
 * Used to enforce the "don't repeat the same card within 60 days" rule
 * and to space cards out across the deck (a card last shown 5 days ago is
 * unavailable; one shown 70 days ago is back in rotation).
 *
 * One row per card key — re-showing a card upserts the timestamp.
 */
@Entity(tableName = "insight_card_shown")
data class InsightCardShown(
    @PrimaryKey val cardKey: String,
    val shownAtEpochMillis: Long,
)
