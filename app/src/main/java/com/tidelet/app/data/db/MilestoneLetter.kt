package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A short letter the user writes to their future self when a milestone lands.
 *
 * Mechanics (see PHASE_2_SPEC §7):
 *   - At each canonical milestone (see [com.tidelet.app.util.MILESTONES_DAYS])
 *     Home shows a one-time prompt: "write a note to your future self?"
 *   - When the user hits the *next* milestone, the previous letter surfaces
 *     on Home as a quiet reveal card and is then marked [revealedAtEpochMillis].
 *
 * `milestoneDays` is the primary key — one letter per milestone. If the user
 * dismisses the prompt they can still write later; if they skip they don't
 * block future prompts.
 */
@Entity(tableName = "milestone_letter")
data class MilestoneLetter(
    /** The milestone-day value this letter commemorates (1, 7, 30, 90, 180, 365…). */
    @PrimaryKey
    val milestoneDays: Int,
    val text: String,
    val writtenAtEpochMillis: Long = System.currentTimeMillis(),
    /**
     * When the reveal card on Home actually showed this letter back to the
     * user. Null until revealed. Once non-null, the reveal card for this
     * letter will not surface again.
     */
    val revealedAtEpochMillis: Long? = null,
)
