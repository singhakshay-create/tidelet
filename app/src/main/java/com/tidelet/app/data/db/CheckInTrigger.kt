package com.tidelet.app.data.db

/**
 * Constant keys for the optional "what was happening?" tag on a [CheckIn]
 * where `didDrink = true`.
 *
 * Stored as a String so the DB stays forward-compatible (new triggers can be
 * added without a schema migration). Use [ALL] when enumerating in the UI.
 *
 * The taxonomy is deliberately small. More choices = more decision work at the
 * exact moment a user is already reflecting on a hard day.
 *
 *  - STRESS       — work, money, argument, overwhelmed
 *  - SOCIAL       — someone offered / everyone else was drinking
 *  - BOREDOM      — nothing to do, reaching for something
 *  - CELEBRATION  — a good thing happened and drinking felt reflexive
 *  - HALT         — hungry / angry / lonely / tired (the classic AA check)
 *  - HABIT        — "it was just that time of day"
 *  - OTHER        — doesn't fit the above
 */
object CheckInTrigger {
    const val STRESS = "STRESS"
    const val SOCIAL = "SOCIAL"
    const val BOREDOM = "BOREDOM"
    const val CELEBRATION = "CELEBRATION"
    const val HALT = "HALT"
    const val HABIT = "HABIT"
    const val OTHER = "OTHER"

    val ALL: List<String> = listOf(STRESS, SOCIAL, BOREDOM, CELEBRATION, HALT, HABIT, OTHER)
}
