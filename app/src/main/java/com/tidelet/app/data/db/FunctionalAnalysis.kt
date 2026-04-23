package com.tidelet.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A short ABC-style "map what happened" record, written in-line after a
 * craving (Ride the Wave outcome) or a slip (Compassionate Reset).
 *
 * The design intent (see CBT tier plan) is that the user fills this in
 * *right then, if they want to* — not as deferred homework. The whole form
 * is four or fewer taps plus an optional sentence, and every field is
 * nullable so a user who hits "skip" doesn't leave a ghost row.
 *
 * Tied loosely to context via [cravingEventId] — nullable because some of
 * these records are entered from a slip context (Compassionate Reset) where
 * the craving event was logged earlier in the flow and we don't need a hard
 * foreign-key constraint for v1.
 */
@Entity(
    tableName = "functional_analysis",
    indices = [Index(value = ["dateIso"]), Index(value = ["cravingEventId"])],
)
data class FunctionalAnalysis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** LocalDate.toString() — ISO "YYYY-MM-DD". */
    val dateIso: String,
    /**
     * The CravingEvent this analysis belongs to, if we know it. Nullable on
     * purpose (see class doc). No FK constraint — we want analyses to survive
     * if the parent event is ever deleted.
     */
    val cravingEventId: Long? = null,
    /** Trigger key from [CheckInTrigger], or null. */
    val antecedent: String? = null,
    /** The thought that came up at the moment of the craving/slip. */
    val thoughtAtMoment: String? = null,
    /** What happened next — what helped, what didn't, what the user did. */
    val followingAction: String? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
