package com.tidelet.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CheckIn
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.EveningReview
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.MilestoneLetter
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.ui.home.companion.CompanionInputs
import com.tidelet.app.ui.home.companion.computeCompanionStage
import com.tidelet.app.ui.home.companion.computeRawCompanionStage
import com.tidelet.app.util.MILESTONES_DAYS
import com.tidelet.app.util.NextMilestone
import com.tidelet.app.util.StreakDuration
import com.tidelet.app.util.mondayOf
import com.tidelet.app.util.nextMilestoneFor
import com.tidelet.app.util.streakFromStartDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

data class HomeUiState(
    val streak: StreakDuration,
    val startDate: LocalDate?,
    val nextMilestone: NextMilestone?,
    /**
     * Total dry days across the user's current streak AND every previous
     * streak they've since reset away from. Equals [streak.days] for a
     * never-reset user; strictly greater once they've reset at least once.
     * See PHASE_2 §9A4 — the subtitle under the streak count.
     */
    val lifetimeDryDays: Long = 0L,
    /**
     * Whether to surface the weekly-reflection prompt card on Home.
     *
     * True only when: today is Sunday (the chosen low-pressure cadence — see
     * Reframe_Benchmarking.md) AND the user hasn't already written a
     * reflection for the current week.
     */
    val showWeeklyReflectionCard: Boolean = false,
    /**
     * When the user has reached a canonical milestone and hasn't yet written a
     * letter for it, this holds the milestone day-count (e.g. 7, 30, 90). Home
     * renders a one-time prompt card.
     */
    val pendingLetterPrompt: Int? = null,
    /**
     * A previously-written letter whose milestone is older than the user's
     * current streak and which hasn't been revealed yet. Home renders a quiet
     * reveal card; the VM marks it revealed when the user dismisses.
     */
    val letterToReveal: MilestoneLetter? = null,
    /**
     * Oldest thought-record or journal entry whose date matches today's
     * day-of-month and is at least a calendar month in the past. Null if
     * nothing eligible exists or the user dismissed the card this session.
     * See [OnThisDayItem] + [computeOnThisDay].
     */
    val onThisDay: OnThisDayItem? = null,
    /**
     * Whether to surface the optional evening-review entry button on Home
     * (PHASE_2 §9A5). True iff the user opted in via Settings AND no
     * evening review has been logged for today's local date.
     */
    val showEveningReviewButton: Boolean = false,
    /**
     * The insight card to surface this Home open, if any (PHASE_2 §9B2).
     * Picked once per VM init via [selectInsightCard]; persists for the
     * lifetime of the VM so re-recompositions don't churn the slot.
     */
    val insightCard: InsightCard? = null,
    /**
     * Whether the user has opted into the visual companion (PHASE_2 §9B1).
     * When false, HomeScreen skips the companion slot entirely.
     */
    val companionEnabled: Boolean = false,
    /**
     * Rendered stage for the companion, 1..5. Respects the high-water
     * mark from the profile, so a streak reset can never drop the stage.
     */
    val companionStage: Int = 1,
    /**
     * True on the one emission where the raw (non-clamped) stage climbs
     * past the stored high-water mark. The companion Composable uses
     * this for a subtle one-shot accent; otherwise ambient-only.
     */
    val companionIsAdvancing: Boolean = false,
)

/**
 * Home screen state:
 *   - Reads the profile flow (for startDate)
 *   - Ticks every minute so the streak display stays fresh
 *   - Derives the next milestone from the current day-count
 *   - Surfaces the milestone letter prompt + reveal card (PHASE_2 §7)
 */
class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    // Emit a value now, then again every 60 seconds, so the "days / hours / minutes"
    // display updates without needing a manual pull-to-refresh.
    private val tick = flow {
        while (true) {
            emit(clock.millis())
            delay(60_000L)
        }
    }

    /**
     * Local dismissal flag so the letter-prompt card doesn't re-render after
     * the user closes it in the same session. Persisting "dismissed once"
     * isn't worth a DataStore key — the next milestone will re-prompt anyway.
     */
    private val dismissedPromptThisSession = MutableStateFlow<Int?>(null)

    /** Same pattern for the "on this day" card — session-local dismissal. */
    private val dismissedOnThisDayThisSession = MutableStateFlow(false)

    /**
     * Insight card slot (PHASE_2 §9B2). Picked once at VM init by querying
     * the recency markers and running the spaced+probabilistic selector;
     * stored as a session-scoped StateFlow so it doesn't re-roll on every
     * recomposition. Set to null when the selector decides to skip.
     */
    private val insightCardThisSession = MutableStateFlow<InsightCard?>(null)
    private val insightDismissedThisSession = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val now = clock.millis()
            val recencyCutoff = now - INSIGHT_RECENCY_WINDOW_MILLIS
            val recentlyShown = repo.insightCardsShown()
                .filter { it.shownAtEpochMillis > recencyCutoff }
                .map { it.cardKey }
                .toSet()
            val pick = selectInsightCard(
                nowMillis = now,
                lastShownAtMillis = repo.lastInsightShownAtMillis(),
                recentlyShownKeys = recentlyShown,
            )
            if (pick != null) {
                insightCardThisSession.value = pick
                repo.recordInsightCardShown(pick.key, now)
            }
        }
    }

    /**
     * Per-day derived "on this day" item (or null when dismissed / no match).
     * Folded into the main [state] combine as a single arg so we stay inside
     * the typed-arity 5 of [combine]. Re-evaluates when [tick] fires, so a
     * day change rolls the anniversary forward without the user opening the
     * app again.
     */
    private val onThisDayFlow: Flow<OnThisDayItem?> = combine(
        repo.thoughtRecords,
        repo.journalEntries,
        dismissedOnThisDayThisSession,
        tick,
    ) { thoughts: List<ThoughtRecord>, entries: List<JournalEntry>, dismissed: Boolean, _ ->
        if (dismissed) null
        else computeOnThisDay(thoughts, entries, LocalDate.now(clock))
    }

    /**
     * Milestone-letter gating pair (PHASE_2 §9A3). Bundles the letter list
     * with a "has the user logged a craving in the last 24 hours?" flag so
     * we can keep the main combine within the typed-arity 5 limit AND
     * suppress the letter prompt mid-wave.
     */
    private data class LetterGating(
        val letters: List<MilestoneLetter>,
        val hasRecentCraving: Boolean,
        val eveningReviewLoggedToday: Boolean,
        val insight: InsightCard?,
        val checkInsLast14d: Int,
    )

    /** Insight card net of session dismissal — folded into [letterGatingFlow]. */
    private val effectiveInsightFlow: Flow<InsightCard?> = combine(
        insightCardThisSession,
        insightDismissedThisSession,
    ) { card, dismissed -> if (dismissed) null else card }

    /**
     * Combined (current-time, check-ins-last-14-days) flow. Replaces the
     * plain [tick] input in the letter-gating combine so we stay within
     * the 5-arg typed `combine` limit while picking up the check-in
     * signal the companion's stage util needs.
     */
    private data class TickAndCheckIns(val nowMillis: Long, val checkInsLast14d: Int)

    private val tickAndCheckInsFlow: Flow<TickAndCheckIns> = combine(
        tick,
        repo.checkIns,
    ) { nowMillis, checkIns ->
        TickAndCheckIns(
            nowMillis = nowMillis,
            checkInsLast14d = countCheckInsInLast14Days(checkIns),
        )
    }

    private fun countCheckInsInLast14Days(checkIns: List<CheckIn>): Int {
        val cutoffEpochDay = LocalDate.now(clock).minusDays(14).toEpochDay()
        return checkIns.count { row ->
            runCatching { LocalDate.parse(row.date).toEpochDay() > cutoffEpochDay }
                .getOrDefault(false)
        }
    }

    private val letterGatingFlow: Flow<LetterGating> = combine(
        repo.milestoneLetters,
        repo.cravingEvents,
        repo.eveningReviews,
        effectiveInsightFlow,
        tickAndCheckInsFlow,
    ) { letters: List<MilestoneLetter>,
        events: List<CravingEvent>,
        reviews: List<EveningReview>,
        insight: InsightCard?,
        tac: TickAndCheckIns ->
        val cutoff = tac.nowMillis - CRAVING_RECENCY_WINDOW_MILLIS
        val today = LocalDate.now(clock).toString()
        LetterGating(
            letters = letters,
            hasRecentCraving = events.any { it.timestampEpochMillis > cutoff },
            eveningReviewLoggedToday = reviews.any { it.dateIso == today },
            insight = insight,
            checkInsLast14d = tac.checkInsLast14d,
        )
    }

    val state: StateFlow<HomeUiState> = combine(
        repo.profile,
        repo.weeklyReflections,
        letterGatingFlow,
        dismissedPromptThisSession,
        onThisDayFlow,
    ) { profile, reflections, gating, dismissedPrompt, onThisDay ->
        val letters = gating.letters
        val startDate = profile.startDateEpochDay?.let(LocalDate::ofEpochDay)
        val today = LocalDate.now(clock)
        val isSunday = today.dayOfWeek == DayOfWeek.SUNDAY
        val thisWeekMonday = mondayOf(today).toString()
        val alreadyReflected = reflections.any { it.weekStartIso == thisWeekMonday }
        val showCard = isSunday && !alreadyReflected

        // PHASE_2 §9A5 — gate the Home button on opt-in AND not-yet-logged.
        val showEveningReview =
            profile.eveningReviewEnabled && !gating.eveningReviewLoggedToday

        // PHASE_2 §9B1 — companion stage + advance signal. Computed here
        // on every emission; persisted via a side-effect when the raw
        // stage exceeds the stored high-water mark. `streakDays` falls
        // back to 0 for a pre-onboarding state so the companion renders
        // at stage 1 rather than blowing up.
        val streakDaysForCompanion = startDate?.let {
            streakFromStartDate(it, now = clock.instant(), zone = clock.zone).days
        } ?: 0L
        val rawStage = computeRawCompanionStage(
            streakDays = streakDaysForCompanion,
            checkInsLast14d = gating.checkInsLast14d,
        )
        val companionStage = computeCompanionStage(
            CompanionInputs(
                streakDays = streakDaysForCompanion,
                checkInsLast14d = gating.checkInsLast14d,
                highestStageEverReached = profile.companionHighestStage,
            )
        )
        val companionIsAdvancing = rawStage > profile.companionHighestStage
        if (companionIsAdvancing) {
            // Side-effect: raise the persisted high-water mark. The impl
            // is idempotent + raise-only, so this is safe to fire on every
            // emission that meets the guard.
            viewModelScope.launch { repo.raiseCompanionHighestStage(rawStage) }
        }

        if (startDate == null) {
            HomeUiState(
                streak = StreakDuration(0, 0, 0),
                startDate = null,
                nextMilestone = null,
                lifetimeDryDays = profile.previousStreaksTotalDays,
                showWeeklyReflectionCard = showCard,
                onThisDay = onThisDay,
                showEveningReviewButton = showEveningReview,
                insightCard = gating.insight,
                companionEnabled = profile.companionEnabled,
                companionStage = companionStage,
                companionIsAdvancing = companionIsAdvancing,
            )
        } else {
            val streak = streakFromStartDate(startDate, now = clock.instant(), zone = clock.zone)

            // Prompt logic: the highest canonical milestone ≤ streak.days that
            // has no letter yet, wasn't dismissed this session, AND the user
            // hasn't logged a craving in the last 24h (PHASE_2 §9A3 — a
            // reflective ask mid-wave is the wrong time). Re-surfaces naturally
            // as the recency window rolls forward.
            val currentMilestone = MILESTONES_DAYS.lastOrNull { it <= streak.days }
            val promptDays = currentMilestone
                ?.takeIf { m -> letters.none { it.milestoneDays == m } }
                ?.takeIf { it != dismissedPrompt }
                ?.takeUnless { gating.hasRecentCraving }

            // Reveal logic: the oldest letter with milestone < current streak
            // that hasn't been revealed yet.
            val reveal = letters
                .filter { it.milestoneDays < streak.days && it.revealedAtEpochMillis == null }
                .maxByOrNull { it.milestoneDays }

            HomeUiState(
                streak = streak,
                startDate = startDate,
                nextMilestone = nextMilestoneFor(streak.days),
                lifetimeDryDays = streak.days + profile.previousStreaksTotalDays,
                showWeeklyReflectionCard = showCard,
                pendingLetterPrompt = promptDays,
                letterToReveal = reveal,
                onThisDay = onThisDay,
                showEveningReviewButton = showEveningReview,
                insightCard = gating.insight,
                companionEnabled = profile.companionEnabled,
                companionStage = companionStage,
                companionIsAdvancing = companionIsAdvancing,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState(StreakDuration(0, 0, 0), null, null),
    )

    /** Called when the user taps "not now" on the letter prompt. */
    fun dismissLetterPrompt(milestoneDays: Int) {
        dismissedPromptThisSession.value = milestoneDays
    }

    /** Called once the reveal card has been shown and the user dismisses it. */
    fun markLetterRevealed(milestoneDays: Int) {
        viewModelScope.launch { repo.markLetterRevealed(milestoneDays) }
    }

    /** Session-local dismissal for the "on this day" card. */
    fun dismissOnThisDay() {
        dismissedOnThisDayThisSession.value = true
    }

    /** Session-local dismissal for the insight card (PHASE_2 §9B2). */
    fun dismissInsightCard() {
        insightDismissedThisSession.value = true
    }

    companion object {
        /**
         * Window over which a recent craving suppresses the milestone-letter
         * prompt (PHASE_2 §9A3). 24 hours, matching the spec — long enough
         * that the prompt disappears for the rest of the day the user logs,
         * short enough that it's back by tomorrow.
         */
        internal const val CRAVING_RECENCY_WINDOW_MILLIS: Long = 24L * 60 * 60 * 1000

        /**
         * Window over which an insight card stays "recently shown" and is
         * excluded from selection (PHASE_2 §9B2). 60 days per spec — keeps
         * variety honest without making the deck stale.
         */
        internal const val INSIGHT_RECENCY_WINDOW_MILLIS: Long = 60L * 24 * 60 * 60 * 1000
    }
}
