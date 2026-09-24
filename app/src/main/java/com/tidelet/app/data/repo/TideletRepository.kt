package com.tidelet.app.data.repo

import com.tidelet.app.data.db.CheckIn
import com.tidelet.app.data.db.CheckInDao
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.CravingEventDao
import com.tidelet.app.data.db.EveningReview
import com.tidelet.app.data.db.EveningReviewDao
import com.tidelet.app.data.db.InsightCardShown
import com.tidelet.app.data.db.InsightCardShownDao
import com.tidelet.app.data.db.FunctionalAnalysis
import com.tidelet.app.data.db.FunctionalAnalysisDao
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.JournalEntryDao
import com.tidelet.app.data.db.MilestoneLetter
import com.tidelet.app.data.db.MilestoneLetterDao
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.db.ReasonDao
import com.tidelet.app.data.db.RefusalPhrase
import com.tidelet.app.data.db.RefusalPhraseDao
import com.tidelet.app.data.db.RelapsePlan
import com.tidelet.app.data.db.RelapsePlanDao
import com.tidelet.app.data.db.TagCount
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.db.ThoughtRecordDao
import com.tidelet.app.data.db.TideletDatabase
import com.tidelet.app.data.db.WeeklyReflection
import com.tidelet.app.data.db.WeeklyReflectionDao
import com.tidelet.app.data.prefs.UserPreferences
import com.tidelet.app.data.prefs.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Public contract the ViewModels depend on. Split from the concrete
 * implementation so tests can substitute a FakeTideletRepository.
 */
interface TideletRepository {

    // ---- Profile ----
    val profile: Flow<UserProfile>
    suspend fun completeOnboarding(startDate: LocalDate)
    suspend fun setStartDate(date: LocalDate)

    // ---- Check-ins ----
    val checkIns: Flow<List<CheckIn>>
    suspend fun upsertCheckIn(
        date: LocalDate,
        didDrink: Boolean,
        drinkCount: Int? = null,
        mood: Int? = null,
        trigger: String? = null,
        note: String? = null,
    )
    suspend fun findCheckIn(date: LocalDate): CheckIn?
    suspend fun importCheckIn(checkIn: CheckIn)

    // ---- Craving events ----
    val cravingEvents: Flow<List<CravingEvent>>
    suspend fun logCravingEvent(tool: String, outcome: String, intensity: Int? = null)
    suspend fun logCravingEventReturningId(tool: String, outcome: String, intensity: Int? = null): Long
    suspend fun importCravingEvent(event: CravingEvent): Long

    // ---- Reasons ----
    val reasons: Flow<List<Reason>>
    suspend fun addReason(text: String)
    suspend fun deleteReason(id: Long)
    suspend fun importReason(reason: Reason): Long

    // ---- Journal entries ----
    val journalEntries: Flow<List<JournalEntry>>
    suspend fun addJournalEntry(date: LocalDate, text: String): Long
    suspend fun importJournalEntry(entry: JournalEntry): Long

    // ---- Weekly reflections ----
    val weeklyReflections: Flow<List<WeeklyReflection>>
    suspend fun upsertWeeklyReflection(weekStart: LocalDate, rating: Int, note: String? = null)
    suspend fun findWeeklyReflection(weekStart: LocalDate): WeeklyReflection?
    suspend fun importWeeklyReflection(reflection: WeeklyReflection)

    // ---- Thought records ----
    val thoughtRecords: Flow<List<ThoughtRecord>>
    val thoughtTagCounts: Flow<List<TagCount>>
    suspend fun addThoughtRecord(
        date: LocalDate,
        situation: String,
        thought: String,
        challenge: String,
        friendReframe: String,
        distortionTag: String? = null,
    ): Long
    suspend fun importThoughtRecord(record: ThoughtRecord): Long

    // ---- Functional analyses ----
    val functionalAnalyses: Flow<List<FunctionalAnalysis>>
    suspend fun addFunctionalAnalysis(
        date: LocalDate,
        cravingEventId: Long? = null,
        antecedent: String? = null,
        thoughtAtMoment: String? = null,
        followingAction: String? = null,
    ): Long
    suspend fun importFunctionalAnalysis(analysis: FunctionalAnalysis): Long

    // ---- Relapse plan ----
    val relapsePlan: Flow<RelapsePlan?>
    suspend fun findRelapsePlan(): RelapsePlan?
    suspend fun saveRelapsePlan(highRiskSituations: String, earlyWarningSigns: String, copingPlan: String)

    // ---- Refusal phrases ----
    val refusalPhrases: Flow<List<RefusalPhrase>>
    suspend fun addRefusalPhrase(text: String)
    suspend fun importRefusalPhrase(phrase: RefusalPhrase): Long
    suspend fun deleteRefusalPhrase(id: Long)

    // ---- Tool-open counters (local-only Stats input) ----
    val toolOpenCounts: Flow<Map<String, Int>>
    suspend fun recordToolOpen(tool: String)

    /**
     * Replace all tool-open counters with the given map. Used by the import
     * flow to restore counts from a backup — the import's counts become the
     * source of truth, overwriting any accumulated values.
     */
    suspend fun setToolOpenCounts(counts: Map<String, Int>)

    // ---- Milestone letters ----
    val milestoneLetters: Flow<List<MilestoneLetter>>
    suspend fun findMilestoneLetter(milestoneDays: Int): MilestoneLetter?
    suspend fun saveMilestoneLetter(milestoneDays: Int, text: String)
    suspend fun findUnrevealedLetterBefore(currentDays: Int): MilestoneLetter?
    suspend fun markLetterRevealed(milestoneDays: Int)
    suspend fun importMilestoneLetter(letter: MilestoneLetter)

    // ---- Evening mini-review (PHASE_2 §9A5) ----
    val eveningReviews: Flow<List<EveningReview>>
    suspend fun findEveningReview(dateIso: String): EveningReview?
    suspend fun upsertEveningReview(dateIso: String, winText: String, challengeText: String)
    suspend fun importEveningReview(review: EveningReview)

    // ---- Insight cards (PHASE_2 §9B2) ----
    suspend fun insightCardsShown(): List<InsightCardShown>
    suspend fun lastInsightShownAtMillis(): Long?
    suspend fun recordInsightCardShown(cardKey: String, atMillis: Long)

    // ---- Visual companion (PHASE_2 §9B1) ----
    suspend fun raiseCompanionHighestStage(stage: Int)

    // ---- Snapshot / restore ----
    suspend fun profileSnapshot(): UserProfile
    suspend fun restoreProfile(
        startDateEpochDay: Long?,
        typicalDrinksPerDay: Int?,
        priceCentsPerDrink: Int?,
        checkInReminderEnabled: Boolean?,
        checkInReminderHour: Int?,
        checkInReminderMinute: Int?,
        previousStreaksTotalDays: Long? = null,
        hoursPerDrink: Int? = null,
        eveningReviewEnabled: Boolean? = null,
        companionEnabled: Boolean? = null,
        companionHighestStage: Int? = null,
    )

    // ---- Destructive ----
    suspend fun wipeAll()
}

/**
 * Room- and DataStore-backed implementation of [TideletRepository].
 * This is the single place we combine Room + DataStore behind a clean API.
 * No business logic lives here — that belongs in the ViewModels.
 */
class RoomTideletRepository(
    db: TideletDatabase,
    private val prefs: UserPreferences,
) : TideletRepository {
    private val checkInDao: CheckInDao = db.checkInDao()
    private val cravingEventDao: CravingEventDao = db.cravingEventDao()
    private val reasonDao: ReasonDao = db.reasonDao()
    private val journalEntryDao: JournalEntryDao = db.journalEntryDao()
    private val weeklyReflectionDao: WeeklyReflectionDao = db.weeklyReflectionDao()
    private val thoughtRecordDao: ThoughtRecordDao = db.thoughtRecordDao()
    private val functionalAnalysisDao: FunctionalAnalysisDao = db.functionalAnalysisDao()
    private val relapsePlanDao: RelapsePlanDao = db.relapsePlanDao()
    private val refusalPhraseDao: RefusalPhraseDao = db.refusalPhraseDao()
    private val milestoneLetterDao: MilestoneLetterDao = db.milestoneLetterDao()
    private val eveningReviewDao: EveningReviewDao = db.eveningReviewDao()
    private val insightCardShownDao: InsightCardShownDao = db.insightCardShownDao()

    // ---- Profile ----

    override val profile: Flow<UserProfile> = prefs.profile

    override suspend fun completeOnboarding(startDate: LocalDate) {
        prefs.completeOnboarding(startDate.toEpochDay())
    }

    override suspend fun setStartDate(date: LocalDate) {
        // PHASE_2 §9A4 — "streak grace language". When the user moves their
        // start date FORWARD (i.e. shortens an existing streak, as happens
        // on a reset), bank the dry days they'd accumulated under the old
        // start date into the lifetime total so they don't vanish. Moving
        // the start date backward is a correction, not a reset — skip the
        // accumulator there. Brand-new users (no prior start date) also skip.
        val snap = prefs.profile.first()
        val oldEpoch = snap.startDateEpochDay
        if (oldEpoch != null) {
            val oldStart = LocalDate.ofEpochDay(oldEpoch)
            if (date.isAfter(oldStart)) {
                val banked = java.time.temporal.ChronoUnit.DAYS
                    .between(oldStart, LocalDate.now())
                    .coerceAtLeast(0L)
                if (banked > 0L) prefs.addPreviousStreakDays(banked)
            }
        }
        prefs.setStartDate(date.toEpochDay())
    }

    // ---- Check-ins ----

    override val checkIns: Flow<List<CheckIn>> = checkInDao.observeAll()

    override suspend fun upsertCheckIn(
        date: LocalDate,
        didDrink: Boolean,
        drinkCount: Int?,
        mood: Int?,
        trigger: String?,
        note: String?,
    ) {
        checkInDao.upsert(
            CheckIn(
                date = date.toString(),
                didDrink = didDrink,
                drinkCount = drinkCount,
                mood = mood,
                trigger = trigger,
                note = note,
            )
        )
    }

    override suspend fun findCheckIn(date: LocalDate): CheckIn? =
        checkInDao.findByDate(date.toString())

    override suspend fun importCheckIn(checkIn: CheckIn) = checkInDao.upsert(checkIn)

    // ---- Craving events ----

    override val cravingEvents: Flow<List<CravingEvent>> = cravingEventDao.observeAll()

    override suspend fun logCravingEvent(tool: String, outcome: String, intensity: Int?) {
        cravingEventDao.insert(CravingEvent(tool = tool, outcome = outcome, intensity = intensity))
    }

    override suspend fun logCravingEventReturningId(tool: String, outcome: String, intensity: Int?): Long =
        cravingEventDao.insert(CravingEvent(tool = tool, outcome = outcome, intensity = intensity))

    override suspend fun importCravingEvent(event: CravingEvent): Long =
        cravingEventDao.insert(event)

    // ---- Reasons ----

    override val reasons: Flow<List<Reason>> = reasonDao.observeAll()

    override suspend fun addReason(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        reasonDao.insert(Reason(text = trimmed))
    }

    override suspend fun deleteReason(id: Long) {
        reasonDao.deleteById(id)
    }

    override suspend fun importReason(reason: Reason): Long = reasonDao.insert(reason)

    // ---- Journal entries ----

    override val journalEntries: Flow<List<JournalEntry>> = journalEntryDao.observeAll()

    override suspend fun addJournalEntry(date: LocalDate, text: String): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return -1L
        return journalEntryDao.insert(
            JournalEntry(dateIso = date.toString(), text = trimmed),
        )
    }

    override suspend fun importJournalEntry(entry: JournalEntry): Long =
        journalEntryDao.insert(entry)

    // ---- Weekly reflections ----

    override val weeklyReflections: Flow<List<WeeklyReflection>> =
        weeklyReflectionDao.observeAll()

    override suspend fun upsertWeeklyReflection(
        weekStart: LocalDate,
        rating: Int,
        note: String?,
    ) {
        weeklyReflectionDao.upsert(
            WeeklyReflection(
                weekStartIso = weekStart.toString(),
                rating = rating.coerceIn(1, 5),
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            )
        )
    }

    override suspend fun findWeeklyReflection(weekStart: LocalDate): WeeklyReflection? =
        weeklyReflectionDao.findByWeekStart(weekStart.toString())

    override suspend fun importWeeklyReflection(reflection: WeeklyReflection) =
        weeklyReflectionDao.upsert(reflection)

    // ---- Thought records ----

    override val thoughtRecords: Flow<List<ThoughtRecord>> = thoughtRecordDao.observeAll()

    override val thoughtTagCounts: Flow<List<TagCount>> = thoughtRecordDao.observeTagCounts()

    override suspend fun addThoughtRecord(
        date: LocalDate,
        situation: String,
        thought: String,
        challenge: String,
        friendReframe: String,
        distortionTag: String?,
    ): Long = thoughtRecordDao.insert(
        ThoughtRecord(
            dateIso = date.toString(),
            situation = situation.trim(),
            thought = thought.trim(),
            challenge = challenge.trim(),
            friendReframe = friendReframe.trim(),
            distortionTag = distortionTag?.takeIf { it.isNotBlank() },
        )
    )

    override suspend fun importThoughtRecord(record: ThoughtRecord): Long =
        thoughtRecordDao.insert(record)

    // ---- Functional analyses ----

    override val functionalAnalyses: Flow<List<FunctionalAnalysis>> =
        functionalAnalysisDao.observeAll()

    override suspend fun addFunctionalAnalysis(
        date: LocalDate,
        cravingEventId: Long?,
        antecedent: String?,
        thoughtAtMoment: String?,
        followingAction: String?,
    ): Long {
        val a = antecedent?.takeIf { it.isNotBlank() }
        val t = thoughtAtMoment?.trim()?.takeIf { it.isNotEmpty() }
        val f = followingAction?.trim()?.takeIf { it.isNotEmpty() }
        if (a == null && t == null && f == null) return -1L

        return functionalAnalysisDao.insert(
            FunctionalAnalysis(
                dateIso = date.toString(),
                cravingEventId = cravingEventId,
                antecedent = a,
                thoughtAtMoment = t,
                followingAction = f,
            )
        )
    }

    override suspend fun importFunctionalAnalysis(analysis: FunctionalAnalysis): Long =
        functionalAnalysisDao.insert(analysis)

    // ---- Relapse plan ----

    override val relapsePlan: Flow<RelapsePlan?> = relapsePlanDao.observeSingleton()

    override suspend fun findRelapsePlan(): RelapsePlan? = relapsePlanDao.findSingleton()

    override suspend fun saveRelapsePlan(
        highRiskSituations: String,
        earlyWarningSigns: String,
        copingPlan: String,
    ) {
        relapsePlanDao.upsert(
            RelapsePlan(
                id = RelapsePlan.SINGLETON_ID,
                highRiskSituations = highRiskSituations.trim(),
                earlyWarningSigns = earlyWarningSigns.trim(),
                copingPlan = copingPlan.trim(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        )
    }

    // ---- Refusal phrases ----

    override val refusalPhrases: Flow<List<RefusalPhrase>> = refusalPhraseDao.observeAll()

    override suspend fun addRefusalPhrase(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        refusalPhraseDao.insert(RefusalPhrase(text = trimmed))
    }

    override suspend fun importRefusalPhrase(phrase: RefusalPhrase): Long =
        refusalPhraseDao.insert(phrase)

    override suspend fun deleteRefusalPhrase(id: Long) {
        refusalPhraseDao.deleteById(id)
    }

    // ---- Tool-open counters ----

    override val toolOpenCounts: Flow<Map<String, Int>> = prefs.toolOpenCounts

    override suspend fun recordToolOpen(tool: String) {
        prefs.incrementToolCount(tool)
    }

    override suspend fun setToolOpenCounts(counts: Map<String, Int>) {
        prefs.setToolOpenCounts(counts)
    }

    // ---- Milestone letters ----

    override val milestoneLetters: Flow<List<MilestoneLetter>> = milestoneLetterDao.observeAll()

    override suspend fun findMilestoneLetter(milestoneDays: Int): MilestoneLetter? =
        milestoneLetterDao.findByMilestone(milestoneDays)

    override suspend fun saveMilestoneLetter(milestoneDays: Int, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        milestoneLetterDao.upsert(
            MilestoneLetter(
                milestoneDays = milestoneDays,
                text = trimmed,
            )
        )
    }

    override suspend fun findUnrevealedLetterBefore(currentDays: Int): MilestoneLetter? =
        milestoneLetterDao.findOldestUnrevealedBefore(currentDays)

    override suspend fun markLetterRevealed(milestoneDays: Int) {
        milestoneLetterDao.markRevealed(milestoneDays, System.currentTimeMillis())
    }

    override suspend fun importMilestoneLetter(letter: MilestoneLetter) {
        milestoneLetterDao.upsert(letter)
    }

    // ---- Evening mini-review (PHASE_2 §9A5) ----

    override val eveningReviews: Flow<List<EveningReview>> = eveningReviewDao.observeAll()

    override suspend fun findEveningReview(dateIso: String): EveningReview? =
        eveningReviewDao.findByDate(dateIso)

    override suspend fun upsertEveningReview(
        dateIso: String,
        winText: String,
        challengeText: String,
    ) {
        eveningReviewDao.upsert(
            EveningReview(
                dateIso = dateIso,
                winText = winText,
                challengeText = challengeText,
            )
        )
    }

    override suspend fun importEveningReview(review: EveningReview) {
        eveningReviewDao.upsert(review)
    }

    // ---- Insight cards (PHASE_2 §9B2) ----

    override suspend fun insightCardsShown(): List<InsightCardShown> =
        insightCardShownDao.all()

    override suspend fun lastInsightShownAtMillis(): Long? =
        insightCardShownDao.lastShownAtMillis()

    override suspend fun recordInsightCardShown(cardKey: String, atMillis: Long) {
        insightCardShownDao.upsert(InsightCardShown(cardKey, atMillis))
    }

    // ---- Visual companion (PHASE_2 §9B1) ----

    override suspend fun raiseCompanionHighestStage(stage: Int) {
        prefs.raiseCompanionHighestStage(stage)
    }

    // ---- Snapshot / restore ----

    override suspend fun profileSnapshot(): UserProfile = prefs.profile.first()

    override suspend fun restoreProfile(
        startDateEpochDay: Long?,
        typicalDrinksPerDay: Int?,
        priceCentsPerDrink: Int?,
        checkInReminderEnabled: Boolean?,
        checkInReminderHour: Int?,
        checkInReminderMinute: Int?,
        previousStreaksTotalDays: Long?,
        hoursPerDrink: Int?,
        eveningReviewEnabled: Boolean?,
        companionEnabled: Boolean?,
        companionHighestStage: Int?,
    ) {
        // Restore is a raw write — we deliberately bypass the setStartDate
        // accumulator because the backup we're loading already carries the
        // authoritative [previousStreaksTotalDays] counter.
        startDateEpochDay?.let { prefs.setStartDate(it) }
        if (typicalDrinksPerDay != null && priceCentsPerDrink != null) {
            prefs.setDrinkingBaseline(typicalDrinksPerDay, priceCentsPerDrink)
        }
        if (checkInReminderEnabled != null && checkInReminderHour != null && checkInReminderMinute != null) {
            prefs.setCheckInReminder(checkInReminderEnabled, checkInReminderHour, checkInReminderMinute)
        }
        previousStreaksTotalDays?.let { prefs.setPreviousStreaksTotalDays(it) }
        hoursPerDrink?.let { prefs.setHoursPerDrink(it) }
        eveningReviewEnabled?.let { prefs.setEveningReviewEnabled(it) }
        companionEnabled?.let { prefs.setCompanionEnabled(it) }
        companionHighestStage?.let { prefs.raiseCompanionHighestStage(it) }
    }

    // ---- Destructive ----

    override suspend fun wipeAll() {
        checkInDao.clear()
        cravingEventDao.clear()
        reasonDao.clear()
        journalEntryDao.clear()
        weeklyReflectionDao.clear()
        thoughtRecordDao.clear()
        functionalAnalysisDao.clear()
        relapsePlanDao.clear()
        refusalPhraseDao.clear()
        milestoneLetterDao.clear()
        eveningReviewDao.clear()
        insightCardShownDao.clear()
        prefs.clearAll()
    }
}
