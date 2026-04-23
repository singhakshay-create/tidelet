package com.tidelet.app.fakes

import com.tidelet.app.data.db.CheckIn
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.EveningReview
import com.tidelet.app.data.db.FunctionalAnalysis
import com.tidelet.app.data.db.InsightCardShown
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.MilestoneLetter
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.db.RefusalPhrase
import com.tidelet.app.data.db.RelapsePlan
import com.tidelet.app.data.db.TagCount
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.db.WeeklyReflection
import com.tidelet.app.data.prefs.UserProfile
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory [TideletRepository] for tests.
 *
 * Each collection is a [MutableStateFlow] so ViewModels observing
 * `repo.profile`, `repo.reasons`, etc. react to seeded state.
 * Public `recorded*` lists capture mutations so tests can assert on intent
 * without fishing them back through Flows.
 */
class FakeTideletRepository : TideletRepository {

    private val _profile = MutableStateFlow(defaultProfile)
    private val _checkIns = MutableStateFlow<List<CheckIn>>(emptyList())
    private val _cravingEvents = MutableStateFlow<List<CravingEvent>>(emptyList())
    private val _reasons = MutableStateFlow<List<Reason>>(emptyList())
    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val _weeklyReflections = MutableStateFlow<List<WeeklyReflection>>(emptyList())
    private val _thoughtRecords = MutableStateFlow<List<ThoughtRecord>>(emptyList())
    private val _thoughtTagCounts = MutableStateFlow<List<TagCount>>(emptyList())
    private val _functionalAnalyses = MutableStateFlow<List<FunctionalAnalysis>>(emptyList())
    private val _relapsePlan = MutableStateFlow<RelapsePlan?>(null)
    private val _refusalPhrases = MutableStateFlow<List<RefusalPhrase>>(emptyList())
    private val _milestoneLetters = MutableStateFlow<List<MilestoneLetter>>(emptyList())
    private val _toolOpenCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _eveningReviews = MutableStateFlow<List<EveningReview>>(emptyList())
    private val _insightCardsShown = MutableStateFlow<List<InsightCardShown>>(emptyList())

    private val idSeq = AtomicLong(0L)

    data class LoggedCraving(val tool: String, val outcome: String)

    val recordedCravings = mutableListOf<LoggedCraving>()
    val recordedOnboarding = mutableListOf<LocalDate>()
    val recordedCheckInUpserts = mutableListOf<CheckIn>()
    val recordedReasonsAdded = mutableListOf<String>()
    val recordedReasonsDeleted = mutableListOf<Long>()
    val recordedFunctionalAnalyses = mutableListOf<FunctionalAnalysis>()
    var wipeCount: Int = 0
        private set

    fun setProfile(profile: UserProfile) = _profile.update { profile }
    fun setStartDateEpochDay(day: Long) =
        _profile.update { it.copy(startDateEpochDay = day, onboardingComplete = true) }
    fun seedReasons(reasons: List<Reason>) = _reasons.update { reasons }
    fun seedCheckIns(rows: List<CheckIn>) = _checkIns.update { rows }
    fun seedWeeklyReflections(rows: List<WeeklyReflection>) = _weeklyReflections.update { rows }
    fun seedThoughtRecords(rows: List<ThoughtRecord>) = _thoughtRecords.update { rows }
    fun seedJournalEntries(rows: List<JournalEntry>) = _journalEntries.update { rows }
    fun seedMilestoneLetters(rows: List<MilestoneLetter>) = _milestoneLetters.update { rows }
    fun seedEveningReviews(rows: List<EveningReview>) = _eveningReviews.update { rows }

    override val profile: Flow<UserProfile> = _profile.asStateFlow()

    override suspend fun completeOnboarding(startDate: LocalDate) {
        recordedOnboarding += startDate
        _profile.update {
            it.copy(
                onboardingComplete = true,
                startDateEpochDay = startDate.toEpochDay(),
                goalMode = "QUIT",
            )
        }
    }

    override suspend fun setStartDate(date: LocalDate) {
        _profile.update { it.copy(startDateEpochDay = date.toEpochDay()) }
    }

    override val checkIns: Flow<List<CheckIn>> = _checkIns.asStateFlow()

    override suspend fun upsertCheckIn(
        date: LocalDate,
        didDrink: Boolean,
        drinkCount: Int?,
        mood: Int?,
        trigger: String?,
        note: String?,
    ) {
        val row = CheckIn(
            date = date.toString(),
            didDrink = didDrink,
            drinkCount = drinkCount,
            mood = mood,
            trigger = trigger,
            note = note,
        )
        recordedCheckInUpserts += row
        _checkIns.update { prev -> prev.filterNot { it.date == row.date } + row }
    }

    override suspend fun findCheckIn(date: LocalDate): CheckIn? =
        _checkIns.value.firstOrNull { it.date == date.toString() }

    override suspend fun importCheckIn(checkIn: CheckIn) {
        _checkIns.update { prev -> prev.filterNot { it.date == checkIn.date } + checkIn }
    }

    override val cravingEvents: Flow<List<CravingEvent>> = _cravingEvents.asStateFlow()

    override suspend fun logCravingEvent(tool: String, outcome: String) {
        logCravingEventReturningId(tool, outcome)
    }

    override suspend fun logCravingEventReturningId(tool: String, outcome: String): Long {
        recordedCravings += LoggedCraving(tool, outcome)
        val id = idSeq.incrementAndGet()
        _cravingEvents.update { it + CravingEvent(id = id, tool = tool, outcome = outcome) }
        return id
    }

    override suspend fun importCravingEvent(event: CravingEvent): Long {
        val id = if (event.id == 0L) idSeq.incrementAndGet() else event.id
        _cravingEvents.update { it + event.copy(id = id) }
        return id
    }

    override val reasons: Flow<List<Reason>> = _reasons.asStateFlow()

    override suspend fun addReason(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        recordedReasonsAdded += trimmed
        val id = idSeq.incrementAndGet()
        _reasons.update { it + Reason(id = id, text = trimmed) }
    }

    override suspend fun deleteReason(id: Long) {
        recordedReasonsDeleted += id
        _reasons.update { rows -> rows.filterNot { it.id == id } }
    }

    override suspend fun importReason(reason: Reason): Long {
        val id = if (reason.id == 0L) idSeq.incrementAndGet() else reason.id
        _reasons.update { it + reason.copy(id = id) }
        return id
    }

    override val journalEntries: Flow<List<JournalEntry>> = _journalEntries.asStateFlow()

    override suspend fun addJournalEntry(date: LocalDate, text: String): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return -1L
        val id = idSeq.incrementAndGet()
        _journalEntries.update { it + JournalEntry(id = id, dateIso = date.toString(), text = trimmed) }
        return id
    }

    override suspend fun importJournalEntry(entry: JournalEntry): Long {
        val id = if (entry.id == 0L) idSeq.incrementAndGet() else entry.id
        _journalEntries.update { it + entry.copy(id = id) }
        return id
    }

    override val weeklyReflections: Flow<List<WeeklyReflection>> = _weeklyReflections.asStateFlow()

    override suspend fun upsertWeeklyReflection(weekStart: LocalDate, rating: Int, note: String?) {
        val row = WeeklyReflection(
            weekStartIso = weekStart.toString(),
            rating = rating.coerceIn(1, 5),
            note = note?.trim()?.takeIf { it.isNotEmpty() },
        )
        _weeklyReflections.update { prev -> prev.filterNot { it.weekStartIso == row.weekStartIso } + row }
    }

    override suspend fun findWeeklyReflection(weekStart: LocalDate): WeeklyReflection? =
        _weeklyReflections.value.firstOrNull { it.weekStartIso == weekStart.toString() }

    override suspend fun importWeeklyReflection(reflection: WeeklyReflection) {
        _weeklyReflections.update { prev ->
            prev.filterNot { it.weekStartIso == reflection.weekStartIso } + reflection
        }
    }

    override val thoughtRecords: Flow<List<ThoughtRecord>> = _thoughtRecords.asStateFlow()
    override val thoughtTagCounts: Flow<List<TagCount>> = _thoughtTagCounts.asStateFlow()

    override suspend fun addThoughtRecord(
        date: LocalDate,
        situation: String,
        thought: String,
        challenge: String,
        friendReframe: String,
        distortionTag: String?,
    ): Long {
        val id = idSeq.incrementAndGet()
        _thoughtRecords.update {
            it + ThoughtRecord(
                id = id,
                dateIso = date.toString(),
                situation = situation.trim(),
                thought = thought.trim(),
                challenge = challenge.trim(),
                friendReframe = friendReframe.trim(),
                distortionTag = distortionTag?.takeIf { it.isNotBlank() },
            )
        }
        return id
    }

    override suspend fun importThoughtRecord(record: ThoughtRecord): Long {
        val id = if (record.id == 0L) idSeq.incrementAndGet() else record.id
        _thoughtRecords.update { it + record.copy(id = id) }
        return id
    }

    override val functionalAnalyses: Flow<List<FunctionalAnalysis>> = _functionalAnalyses.asStateFlow()

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
        val id = idSeq.incrementAndGet()
        val row = FunctionalAnalysis(
            id = id,
            dateIso = date.toString(),
            cravingEventId = cravingEventId,
            antecedent = a,
            thoughtAtMoment = t,
            followingAction = f,
        )
        recordedFunctionalAnalyses += row
        _functionalAnalyses.update { it + row }
        return id
    }

    override suspend fun importFunctionalAnalysis(analysis: FunctionalAnalysis): Long {
        val id = if (analysis.id == 0L) idSeq.incrementAndGet() else analysis.id
        _functionalAnalyses.update { it + analysis.copy(id = id) }
        return id
    }

    override val relapsePlan: Flow<RelapsePlan?> = _relapsePlan.asStateFlow()

    override suspend fun findRelapsePlan(): RelapsePlan? = _relapsePlan.value

    override suspend fun saveRelapsePlan(
        highRiskSituations: String,
        earlyWarningSigns: String,
        copingPlan: String,
    ) {
        _relapsePlan.update {
            RelapsePlan(
                id = RelapsePlan.SINGLETON_ID,
                highRiskSituations = highRiskSituations.trim(),
                earlyWarningSigns = earlyWarningSigns.trim(),
                copingPlan = copingPlan.trim(),
            )
        }
    }

    override val refusalPhrases: Flow<List<RefusalPhrase>> = _refusalPhrases.asStateFlow()

    override suspend fun addRefusalPhrase(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val id = idSeq.incrementAndGet()
        _refusalPhrases.update { it + RefusalPhrase(id = id, text = trimmed) }
    }

    override suspend fun importRefusalPhrase(phrase: RefusalPhrase): Long {
        val id = if (phrase.id == 0L) idSeq.incrementAndGet() else phrase.id
        _refusalPhrases.update { it + phrase.copy(id = id) }
        return id
    }

    override suspend fun deleteRefusalPhrase(id: Long) {
        _refusalPhrases.update { rows -> rows.filterNot { it.id == id } }
    }

    override val toolOpenCounts: Flow<Map<String, Int>> = _toolOpenCounts.asStateFlow()

    override suspend fun recordToolOpen(tool: String) {
        _toolOpenCounts.update { prev ->
            prev + (tool to ((prev[tool] ?: 0) + 1))
        }
    }

    override suspend fun setToolOpenCounts(counts: Map<String, Int>) {
        _toolOpenCounts.value = counts.toMap()
    }

    override val milestoneLetters: Flow<List<MilestoneLetter>> = _milestoneLetters.asStateFlow()

    override suspend fun findMilestoneLetter(milestoneDays: Int): MilestoneLetter? =
        _milestoneLetters.value.firstOrNull { it.milestoneDays == milestoneDays }

    override suspend fun saveMilestoneLetter(milestoneDays: Int, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val row = MilestoneLetter(milestoneDays = milestoneDays, text = trimmed)
        _milestoneLetters.update { prev ->
            prev.filterNot { it.milestoneDays == milestoneDays } + row
        }
    }

    override suspend fun findUnrevealedLetterBefore(currentDays: Int): MilestoneLetter? =
        _milestoneLetters.value
            .filter { it.milestoneDays < currentDays && it.revealedAtEpochMillis == null }
            .maxByOrNull { it.milestoneDays }

    override suspend fun markLetterRevealed(milestoneDays: Int) {
        _milestoneLetters.update { rows ->
            rows.map { row ->
                if (row.milestoneDays == milestoneDays)
                    row.copy(revealedAtEpochMillis = System.currentTimeMillis())
                else row
            }
        }
    }

    override suspend fun importMilestoneLetter(letter: MilestoneLetter) {
        _milestoneLetters.update { prev ->
            prev.filterNot { it.milestoneDays == letter.milestoneDays } + letter
        }
    }

    override val eveningReviews: Flow<List<EveningReview>> = _eveningReviews.asStateFlow()

    override suspend fun findEveningReview(dateIso: String): EveningReview? =
        _eveningReviews.value.firstOrNull { it.dateIso == dateIso }

    override suspend fun upsertEveningReview(
        dateIso: String,
        winText: String,
        challengeText: String,
    ) {
        _eveningReviews.update { prev ->
            prev.filterNot { it.dateIso == dateIso } + EveningReview(
                dateIso = dateIso,
                winText = winText,
                challengeText = challengeText,
            )
        }
    }

    override suspend fun importEveningReview(review: EveningReview) {
        _eveningReviews.update { prev ->
            prev.filterNot { it.dateIso == review.dateIso } + review
        }
    }

    override suspend fun insightCardsShown(): List<InsightCardShown> = _insightCardsShown.value

    override suspend fun lastInsightShownAtMillis(): Long? =
        _insightCardsShown.value.maxOfOrNull { it.shownAtEpochMillis }

    override suspend fun recordInsightCardShown(cardKey: String, atMillis: Long) {
        _insightCardsShown.update { prev ->
            prev.filterNot { it.cardKey == cardKey } + InsightCardShown(cardKey, atMillis)
        }
    }

    override suspend fun raiseCompanionHighestStage(stage: Int) {
        _profile.update { prev ->
            val clamped = stage.coerceIn(1, 5)
            if (clamped > prev.companionHighestStage) {
                prev.copy(companionHighestStage = clamped)
            } else {
                prev
            }
        }
    }

    override suspend fun profileSnapshot(): UserProfile = _profile.value

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
        _profile.update { prev ->
            prev.copy(
                startDateEpochDay = startDateEpochDay ?: prev.startDateEpochDay,
                typicalDrinksPerDay = typicalDrinksPerDay ?: prev.typicalDrinksPerDay,
                priceCentsPerDrink = priceCentsPerDrink ?: prev.priceCentsPerDrink,
                checkInReminderEnabled = checkInReminderEnabled ?: prev.checkInReminderEnabled,
                checkInReminderHour = checkInReminderHour ?: prev.checkInReminderHour,
                checkInReminderMinute = checkInReminderMinute ?: prev.checkInReminderMinute,
                previousStreaksTotalDays = previousStreaksTotalDays
                    ?: prev.previousStreaksTotalDays,
                hoursPerDrink = hoursPerDrink ?: prev.hoursPerDrink,
                eveningReviewEnabled = eveningReviewEnabled ?: prev.eveningReviewEnabled,
                companionEnabled = companionEnabled ?: prev.companionEnabled,
                // Raise-only semantics match the impl — backup may have a
                // higher stage, but we never lower below what's already local.
                companionHighestStage = maxOf(
                    prev.companionHighestStage,
                    companionHighestStage ?: prev.companionHighestStage,
                ),
            )
        }
    }

    override suspend fun wipeAll() {
        wipeCount += 1
        _checkIns.value = emptyList()
        _cravingEvents.value = emptyList()
        _reasons.value = emptyList()
        _journalEntries.value = emptyList()
        _weeklyReflections.value = emptyList()
        _thoughtRecords.value = emptyList()
        _functionalAnalyses.value = emptyList()
        _relapsePlan.value = null
        _refusalPhrases.value = emptyList()
        _milestoneLetters.value = emptyList()
        _eveningReviews.value = emptyList()
        _insightCardsShown.value = emptyList()
        _profile.value = defaultProfile
    }

    companion object {
        val defaultProfile = UserProfile(
            onboardingComplete = false,
            startDateEpochDay = null,
            goalMode = "QUIT",
            checkInReminderHour = 20,
            checkInReminderMinute = 0,
            checkInReminderEnabled = false,
            typicalDrinksPerDay = 3,
            priceCentsPerDrink = 800,
            previousStreaksTotalDays = 0L,
            hoursPerDrink = 1,
            eveningReviewEnabled = false,
            companionEnabled = false,
            companionHighestStage = 1,
        )
    }
}
