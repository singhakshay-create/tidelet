package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row every time the user opens the SOS flow and uses a tool.
 * Powers future "here are your triggers" insights — all on-device.
 */
@Entity(tableName = "craving_event")
data class CravingEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    /** Which SOS tool was used. See [SosToolKey]. */
    val tool: String,
    /** Outcome after the tool finished. See [CravingOutcome]. */
    val outcome: String,
)

/** Stable string keys for SOS tools — Room stores them as text for readability. */
object SosToolKey {
    const val RIDE_THE_WAVE = "ride_the_wave"
    const val BREATHE = "breathe"
    const val REASONS = "reasons"
    const val DISTRACTIONS = "distractions"
    const val JOURNAL = "journal"

    // CBT tier additions
    const val THOUGHT_CHECK = "thought_check"
    const val REFUSAL_PRACTICE = "refusal_practice"
}

object CravingOutcome {
    const val GOT_THROUGH = "got_through"
    const val STILL_STRUGGLING = "still_struggling"
    const val DRANK = "drank"
    const val UNKNOWN = "unknown"
}
