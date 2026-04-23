package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A free-text journal entry, written inside the Journal SOS tool.
 *
 * Unlike [CheckIn], a user can write several entries on the same day, so the
 * primary key is an auto-generated id — not the date. `dateIso` is indexed so
 * we can cheaply pull "today's entries" on the Log tab later.
 *
 * The prompt that was shown is *not* stored: there is only ever one in v1
 * ("What's really going on right now?"), and if prompt variety is added later
 * we can introduce a `promptKey` column without breaking existing rows.
 */
@Entity(
    tableName = "journal_entry",
    indices = [Index(value = ["dateIso"])],
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** LocalDate.toString() — ISO "YYYY-MM-DD" in the user's local time. */
    val dateIso: String,
    val text: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
