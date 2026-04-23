package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per day the user logs their drinking status.
 *
 * `date` is stored as a LocalDate in ISO format ("YYYY-MM-DD"). Strings are a bit larger than
 * epoch-days ints, but they're readable in DB inspectors — useful while learning.
 *
 * `mood` is a 1–5 integer (1 = rough day, 5 = great day) or null if the user skipped it.
 *
 * `trigger` is only meaningful when `didDrink = true` — it stores one of the
 * keys in [CheckInTrigger] (stress / social / boredom / celebration / HALT /
 * habit / other). Null means "the user didn't tag it" — always optional.
 */
@Entity(tableName = "check_in")
data class CheckIn(
    @PrimaryKey
    val date: String,
    val didDrink: Boolean,
    val drinkCount: Int? = null,
    val mood: Int? = null,
    val trigger: String? = null,
    val note: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
