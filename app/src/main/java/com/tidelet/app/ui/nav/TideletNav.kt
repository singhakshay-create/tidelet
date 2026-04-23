package com.tidelet.app.ui.nav

/**
 * All route strings for Compose Navigation, in one place.
 *
 * We use plain string constants rather than type-safe nav (Kotlin Serialization-based)
 * to keep the surface small and readable for a learner. Easy to upgrade later.
 */
object Routes {
    // Top-level (outside the bottom-nav)
    const val ONBOARDING = "onboarding"

    // Bottom-nav destinations
    const val HOME = "home"
    const val LOG = "log"
    const val JOURNAL = "journal"
    const val STATS = "stats"
    const val SETTINGS = "settings"

    // Log flow
    /** Pattern used when registering the route. `{date}` is an ISO date ("YYYY-MM-DD"). */
    const val LOG_CHECKIN = "log/checkin/{date}"

    /** Concrete path for navigating — fills the `{date}` slot. */
    fun logCheckIn(isoDate: String): String = "log/checkin/$isoDate"

    /**
     * Sunday-evening weekly reflection form. One row per ISO week — writes
     * upsert the single slot keyed by the week's Monday.
     */
    const val WEEKLY_REFLECTION = "weekly/reflection"

    // SOS flow (modal — takes over the screen, no bottom bar)
    const val SOS = "sos"
    const val SOS_WAVE = "sos/wave"
    const val SOS_BREATHE = "sos/breathe"
    const val SOS_REASONS = "sos/reasons"
    const val SOS_DISTRACTIONS = "sos/distractions"

    /**
     * The Journal hub — a chooser between "write freely" and "thought check."
     * Kept at the same route as before (SOS grid still points here) to
     * minimise churn; the old single-prompt screen now lives at
     * [SOS_JOURNAL_FREE].
     */
    const val SOS_JOURNAL = "sos/journal"

    /** The freeform journal editor — the original JournalScreen. */
    const val SOS_JOURNAL_FREE = "sos/journal/free"

    /**
     * Thought check — the CBT micro thought-record. Four short prompts plus
     * an optional distortion tag. Saves to ThoughtRecord.
     */
    const val SOS_THOUGHT_CHECK = "sos/thought-check"

    /**
     * Browsable list of cognitive distortions with drinking-flavoured examples.
     * Reachable from the Thought Check screen and from Settings.
     */
    const val CBT_DISTORTIONS = "cbt/distortions"

    /**
     * The user-authored relapse-prevention plan. View/edit the three fields
     * (high-risk situations, early warning signs, coping plan). Reached from
     * Settings and from the Reasons screen.
     */
    const val RELAPSE_PLAN = "cbt/relapse-plan"

    /**
     * Drink-refusal rehearsal — practice pre-written + user-added refusal
     * phrases. Reached from Distractions and from Settings.
     */
    const val REFUSAL_REHEARSAL = "cbt/refusal"

    /**
     * Compassionate reset — shown after the user answers "I drank" at the
     * end of Ride the Wave. Validates the slip, then offers a route to
     * today's check-in so the moment doesn't disappear unlogged.
     */
    const val SOS_RESET = "sos/reset"

    /**
     * Milestone letter-to-self. `{days}` is the milestone day-count (1, 7, 30,
     * …). Reached from a prompt card on Home when the user reaches a
     * canonical milestone and hasn't written a letter for it yet.
     */
    const val MILESTONE_LETTER = "milestones/letter/{days}"

    fun milestoneLetter(days: Int): String = "milestones/letter/$days"

    // ---- Read-side Journal flow (PHASE_2 §5) ----

    /**
     * List of past thought-check entries, grouped by distortion tag. Reached
     * from the Journal bottom-nav tab.
     */
    const val JOURNAL_THOUGHT_CHECKS = "journal/thoughts"

    /**
     * Read-only detail for one ThoughtRecord. `{id}` is the row id.
     */
    const val JOURNAL_THOUGHT_CHECK_DETAIL = "journal/thoughts/{id}"

    fun journalThoughtCheckDetail(id: Long): String = "journal/thoughts/$id"

    /** List of past free-text journal entries, reverse-chronological with search. */
    const val JOURNAL_ENTRIES = "journal/entries"

    /** Read-only detail for one JournalEntry. `{id}` is the row id. */
    const val JOURNAL_ENTRY_DETAIL = "journal/entries/{id}"

    fun journalEntryDetail(id: Long): String = "journal/entries/$id"

    /**
     * Optional evening mini-review (PHASE_2 §9A5). Two free-text fields,
     * upserted by today's date. Reached from a Home button when opted-in
     * via Settings.
     */
    const val EVENING_REVIEW = "checkin/evening-review"

    /** Read-only list of past evening reviews. Reached from the Journal hub. */
    const val JOURNAL_EVENING_REVIEWS = "journal/evening-reviews"
}
