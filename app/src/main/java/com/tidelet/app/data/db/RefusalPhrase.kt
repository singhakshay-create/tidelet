package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-added drink-refusal phrase.
 *
 * CBT-for-alcohol programmes rehearse refusal skills in advance — the
 * evidence for "practice saying no before you need to" is strong enough that
 * it deserves a tiny editable list in the app. Built-in suggestion phrases
 * live in strings.xml (`refusal_builtin_*`) and are NOT stored here — only
 * user-authored phrases are rows.
 *
 * This keeps export/import smaller (only the user's own data round-trips)
 * and lets us evolve the built-in copy via app updates without touching the DB.
 */
@Entity(tableName = "refusal_phrase")
data class RefusalPhrase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val createdEpochMillis: Long = System.currentTimeMillis(),
)
