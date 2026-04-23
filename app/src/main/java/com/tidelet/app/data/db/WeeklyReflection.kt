package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single Sunday-evening reflection for the week that's ending.
 *
 * We key by the ISO date of the week's Monday (e.g. a reflection written any
 * day between Mon 2026-04-13 and Sun 2026-04-19 carries `weekStartIso =
 * "2026-04-13"`). Upsert semantics: writing again overwrites — the user gets
 * one slot per week, edit-in-place.
 *
 * `rating` is 1–5 (1 = a rough week, 5 = a really good one). `note` is
 * optional free text.
 */
@Entity(tableName = "weekly_reflection")
data class WeeklyReflection(
    @PrimaryKey
    val weekStartIso: String,
    val rating: Int,
    val note: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
