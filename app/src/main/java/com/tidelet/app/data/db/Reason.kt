package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A personal reason the user wrote down for quitting or cutting back.
 *
 * Surfaces on the SOS "My reasons" screen during a craving — the user's own
 * words, from a calmer moment, talking back to the current urge.
 */
@Entity(tableName = "reason")
data class Reason(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val createdEpochMillis: Long = System.currentTimeMillis(),
)
