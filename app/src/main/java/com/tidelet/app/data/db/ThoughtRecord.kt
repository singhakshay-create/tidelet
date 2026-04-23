package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A micro thought-record written inside the SOS "Thought check" tool.
 *
 * This is the cognitive-restructuring workhorse of the CBT tier. The user is
 * walked through four short prompts (situation, thought, challenge, friend
 * reframe) and everything they type is saved here. We keep the raw text AND a
 * coarse distortion tag so the Stats tab can later surface "your most common
 * pattern" without re-parsing user prose.
 *
 * One row per check. Multiple checks per day allowed (a craving can come back),
 * so the primary key is an auto-generated id — not the date. `dateIso` is
 * indexed for cheap "today's records" pulls.
 *
 * `distortionTag` is the `key` field from [com.tidelet.app.ui.cbt.Distortion],
 * or null if the user didn't tag one. We never enforce a specific taxonomy in
 * the DB — future-us can grow the tag space without a migration.
 */
@Entity(
    tableName = "thought_record",
    indices = [Index(value = ["dateIso"])],
)
data class ThoughtRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** LocalDate.toString() — ISO "YYYY-MM-DD" in the user's local time. */
    val dateIso: String,
    /** What's happening right now? (situation + feeling, free text) */
    val situation: String,
    /** What's the thought? ("I need this," "I can't do this sober") */
    val thought: String,
    /** Is that 100% true? — the short challenge */
    val challenge: String,
    /** What would you tell a friend in this moment? — the compassion reframe */
    val friendReframe: String,
    /** Distortion key (e.g. "all_or_nothing") — null if the user didn't tag one. */
    val distortionTag: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
