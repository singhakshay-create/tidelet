package com.tidelet.app.data.export

import com.tidelet.app.data.db.CheckIn
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.FunctionalAnalysis
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.EveningReview
import com.tidelet.app.data.db.MilestoneLetter
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.db.RefusalPhrase
import com.tidelet.app.data.db.RelapsePlan
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.db.WeeklyReflection

/**
 * Serialise and deserialise the user's entire Tidelet dataset as Markdown.
 *
 * **Why Markdown?** The user asked for "ideally exported/imported in MD format"
 * because the export is something they might want to read, not just archive.
 * So this module produces a document that is:
 *
 *   - human-readable (section headings, friendly dates, prose entries preserved)
 *   - round-trippable (every entity lives in a tagged fenced code block with
 *     `key = value` pairs, so the parser is trivial)
 *   - forward-compatible (the `<!-- format: tidelet-md v1 ... -->` banner lets
 *     future versions pick the right parser)
 *
 * The data model is flat: one section per entity type, each entity serialised
 * as one fenced block. Free-text fields with newlines use `\n` escape
 * sequences inside the block so fields stay single-line; `\\` escapes a
 * literal backslash for round-trip safety.
 *
 * This file has NO Android dependencies on purpose — it's pure Kotlin so the
 * logic is testable in plain JVM code without spinning up an emulator.
 */
object TideletMarkdown {

    /** Format version. Bump when the on-disk layout changes incompatibly. */
    const val FORMAT_VERSION = 1

    /** Fence tag prefix — a reader scanning the file can find entities easily. */
    private const val FENCE_PREFIX = "```tidelet "
    private const val FENCE_CLOSE = "```"

    /** Entity kinds. Strings are public by design so the file stays grep-able. */
    object Kind {
        const val PROFILE = "profile"
        const val CHECK_IN = "checkin"
        const val CRAVING_EVENT = "craving-event"
        const val REASON = "reason"
        const val JOURNAL_ENTRY = "journal-entry"
        const val WEEKLY_REFLECTION = "weekly-reflection"
        const val THOUGHT_RECORD = "thought-record"
        const val FUNCTIONAL_ANALYSIS = "functional-analysis"
        const val RELAPSE_PLAN = "relapse-plan"
        const val REFUSAL_PHRASE = "refusal-phrase"
        const val MILESTONE_LETTER = "milestone-letter"
        /** One fence block per tool; `tool` + `count` fields. */
        const val TOOL_OPEN_COUNT = "tool-open-count"
        /** PHASE_2 §9A5 — one fence block per logged evening review. */
        const val EVENING_REVIEW = "evening-review"
    }

    // ------------------------------------------------------------------
    // Encoding
    // ------------------------------------------------------------------

    /**
     * Serialize a snapshot of every entity into one Markdown document.
     * Order is stable so diffing two exports is meaningful.
     */
    fun encode(snapshot: ExportSnapshot, exportedAtIso: String): String = buildString {
        appendLine("# Tidelet Export")
        appendLine()
        appendLine("<!-- format: tidelet-md v$FORMAT_VERSION; exported: $exportedAtIso -->")
        appendLine()
        appendLine("This file is a full backup of your Tidelet data. Keep it somewhere safe — you can re-import it from Settings.")
        appendLine()

        // --- Profile (always one, optional) ---
        if (snapshot.profile != null) {
            appendSectionHeader("Profile")
            appendFence(Kind.PROFILE) {
                field("start_date_epoch_day", snapshot.profile.startDateEpochDay)
                field("typical_drinks_per_day", snapshot.profile.typicalDrinksPerDay)
                field("price_cents_per_drink", snapshot.profile.priceCentsPerDrink)
                field("checkin_reminder_enabled", snapshot.profile.checkInReminderEnabled)
                field("checkin_reminder_hour", snapshot.profile.checkInReminderHour)
                field("checkin_reminder_minute", snapshot.profile.checkInReminderMinute)
                field("previous_streaks_total_days", snapshot.profile.previousStreaksTotalDays)
                field("hours_per_drink", snapshot.profile.hoursPerDrink)
                field("evening_review_enabled", snapshot.profile.eveningReviewEnabled)
                field("companion_enabled", snapshot.profile.companionEnabled)
                field("companion_highest_stage", snapshot.profile.companionHighestStage)
            }
        }

        // --- Check-ins ---
        if (snapshot.checkIns.isNotEmpty()) {
            appendSectionHeader("Check-ins (${snapshot.checkIns.size})")
            snapshot.checkIns.forEach { row ->
                appendFence(Kind.CHECK_IN) {
                    field("date", row.date)
                    field("did_drink", row.didDrink)
                    field("drink_count", row.drinkCount)
                    field("mood", row.mood)
                    field("trigger", row.trigger)
                    field("note", row.note)
                    field("created_at", row.createdAtEpochMillis)
                }
            }
        }

        // --- Craving events ---
        if (snapshot.cravingEvents.isNotEmpty()) {
            appendSectionHeader("Craving events (${snapshot.cravingEvents.size})")
            snapshot.cravingEvents.forEach { row ->
                appendFence(Kind.CRAVING_EVENT) {
                    field("id", row.id)
                    field("timestamp", row.timestampEpochMillis)
                    field("tool", row.tool)
                    field("outcome", row.outcome)
                    row.intensity?.let { field("intensity", it) }
                }
            }
        }

        // --- Reasons ---
        if (snapshot.reasons.isNotEmpty()) {
            appendSectionHeader("Reasons (${snapshot.reasons.size})")
            snapshot.reasons.forEach { row ->
                appendFence(Kind.REASON) {
                    field("created", row.createdEpochMillis)
                    field("text", row.text)
                }
            }
        }

        // --- Journal entries ---
        if (snapshot.journalEntries.isNotEmpty()) {
            appendSectionHeader("Journal entries (${snapshot.journalEntries.size})")
            snapshot.journalEntries.forEach { row ->
                appendFence(Kind.JOURNAL_ENTRY) {
                    field("date", row.dateIso)
                    field("created_at", row.createdAtEpochMillis)
                    field("text", row.text)
                }
            }
        }

        // --- Weekly reflections ---
        if (snapshot.weeklyReflections.isNotEmpty()) {
            appendSectionHeader("Weekly reflections (${snapshot.weeklyReflections.size})")
            snapshot.weeklyReflections.forEach { row ->
                appendFence(Kind.WEEKLY_REFLECTION) {
                    field("week_start", row.weekStartIso)
                    field("rating", row.rating)
                    field("note", row.note)
                    field("created_at", row.createdAtEpochMillis)
                }
            }
        }

        // --- Thought records ---
        if (snapshot.thoughtRecords.isNotEmpty()) {
            appendSectionHeader("Thought records (${snapshot.thoughtRecords.size})")
            snapshot.thoughtRecords.forEach { row ->
                appendFence(Kind.THOUGHT_RECORD) {
                    field("date", row.dateIso)
                    field("situation", row.situation)
                    field("thought", row.thought)
                    field("challenge", row.challenge)
                    field("friend_reframe", row.friendReframe)
                    field("distortion_tag", row.distortionTag)
                    field("created_at", row.createdAtEpochMillis)
                }
            }
        }

        // --- Functional analyses ---
        if (snapshot.functionalAnalyses.isNotEmpty()) {
            appendSectionHeader("Functional analyses (${snapshot.functionalAnalyses.size})")
            snapshot.functionalAnalyses.forEach { row ->
                appendFence(Kind.FUNCTIONAL_ANALYSIS) {
                    field("date", row.dateIso)
                    field("craving_event_id", row.cravingEventId)
                    field("antecedent", row.antecedent)
                    field("thought_at_moment", row.thoughtAtMoment)
                    field("following_action", row.followingAction)
                    field("created_at", row.createdAtEpochMillis)
                }
            }
        }

        // --- Relapse plan (singleton) ---
        if (snapshot.relapsePlan != null) {
            appendSectionHeader("Relapse prevention plan")
            appendFence(Kind.RELAPSE_PLAN) {
                field("high_risk_situations", snapshot.relapsePlan.highRiskSituations)
                field("early_warning_signs", snapshot.relapsePlan.earlyWarningSigns)
                field("coping_plan", snapshot.relapsePlan.copingPlan)
                field("updated_at", snapshot.relapsePlan.updatedAtEpochMillis)
            }
        }

        // --- User-authored refusal phrases ---
        if (snapshot.refusalPhrases.isNotEmpty()) {
            appendSectionHeader("Refusal phrases (${snapshot.refusalPhrases.size})")
            snapshot.refusalPhrases.forEach { row ->
                appendFence(Kind.REFUSAL_PHRASE) {
                    field("created", row.createdEpochMillis)
                    field("text", row.text)
                }
            }
        }

        // --- Milestone letters (per-milestone notes to the future self) ---
        if (snapshot.milestoneLetters.isNotEmpty()) {
            appendSectionHeader("Milestone letters (${snapshot.milestoneLetters.size})")
            snapshot.milestoneLetters.forEach { letter ->
                appendFence(Kind.MILESTONE_LETTER) {
                    field("milestone_days", letter.milestoneDays)
                    field("written_at", letter.writtenAtEpochMillis)
                    field("revealed_at", letter.revealedAtEpochMillis)
                    field("text", letter.text)
                }
            }
        }

        // --- Tool-open counters (local-only Stats input) ---
        // Emitted deterministically (sorted by tool key) so two exports taken
        // of the same state are byte-identical — useful for diff-based review.
        if (snapshot.toolOpenCounts.isNotEmpty()) {
            appendSectionHeader("Tool use counts (${snapshot.toolOpenCounts.size})")
            snapshot.toolOpenCounts.entries
                .sortedBy { it.key }
                .forEach { (tool, count) ->
                    appendFence(Kind.TOOL_OPEN_COUNT) {
                        field("tool", tool)
                        field("count", count)
                    }
                }
        }

        // --- Evening reviews (PHASE_2 §9A5) ---
        if (snapshot.eveningReviews.isNotEmpty()) {
            appendSectionHeader("Evening reviews (${snapshot.eveningReviews.size})")
            snapshot.eveningReviews
                .sortedBy { it.dateIso }
                .forEach { review ->
                    appendFence(Kind.EVENING_REVIEW) {
                        field("date", review.dateIso)
                        field("win", review.winText)
                        field("challenge", review.challengeText)
                        field("created_at", review.createdAtEpochMillis)
                    }
                }
        }
    }

    private fun StringBuilder.appendSectionHeader(title: String) {
        appendLine("## $title")
        appendLine()
    }

    private inline fun StringBuilder.appendFence(kind: String, body: (StringBuilder) -> Unit) {
        append(FENCE_PREFIX).append(kind).append('\n')
        body(this)
        append(FENCE_CLOSE).append('\n').append('\n')
    }

    private fun StringBuilder.field(key: String, value: Any?) {
        // Nulls are written as empty — the parser treats missing/empty keys as null
        // so no information is lost on round-trip.
        val encoded = when (value) {
            null -> ""
            is String -> escape(value)
            else -> value.toString()
        }
        append(key).append(" = ").append(encoded).append('\n')
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "")

    private fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> { append('\n'); i += 2; continue }
                    '\\' -> { append('\\'); i += 2; continue }
                }
            }
            append(c)
            i += 1
        }
    }

    // ------------------------------------------------------------------
    // Decoding
    // ------------------------------------------------------------------

    /**
     * Parse an earlier export back into a [ParsedImport]. This is *lossy only
     * in intent* — fields we don't recognise are ignored, fields missing from
     * the source yield nulls/defaults. Call-site decides how to handle the
     * version mismatch.
     */
    fun decode(text: String): ParsedImport {
        val lines = text.lines()

        // Read version from banner if present.
        val versionRegex = Regex("format:\\s*tidelet-md\\s+v(\\d+)")
        val declaredVersion = lines
            .firstNotNullOfOrNull { versionRegex.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

        val checkIns = mutableListOf<CheckIn>()
        val cravingEvents = mutableListOf<CravingEvent>()
        val reasons = mutableListOf<Reason>()
        val journalEntries = mutableListOf<JournalEntry>()
        val weeklyReflections = mutableListOf<WeeklyReflection>()
        val thoughtRecords = mutableListOf<ThoughtRecord>()
        val functionalAnalyses = mutableListOf<FunctionalAnalysis>()
        val refusalPhrases = mutableListOf<RefusalPhrase>()
        val milestoneLetters = mutableListOf<MilestoneLetter>()
        val toolOpenCounts = mutableMapOf<String, Int>()
        val eveningReviews = mutableListOf<EveningReview>()
        var profile: ExportProfile? = null
        var relapsePlan: RelapsePlan? = null

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith(FENCE_PREFIX)) {
                val kind = line.substring(FENCE_PREFIX.length).trim()
                val body = mutableMapOf<String, String>()
                i += 1
                while (i < lines.size && lines[i].trimEnd() != FENCE_CLOSE) {
                    val entry = lines[i]
                    val eq = entry.indexOf('=')
                    if (eq > 0) {
                        val key = entry.substring(0, eq).trim()
                        val raw = entry.substring(eq + 1).trimStart()
                        body[key] = raw
                    }
                    i += 1
                }
                // Skip the closing fence
                if (i < lines.size) i += 1

                when (kind) {
                    Kind.PROFILE -> profile = parseProfile(body)
                    Kind.CHECK_IN -> parseCheckIn(body)?.let(checkIns::add)
                    Kind.CRAVING_EVENT -> parseCravingEvent(body)?.let(cravingEvents::add)
                    Kind.REASON -> parseReason(body)?.let(reasons::add)
                    Kind.JOURNAL_ENTRY -> parseJournalEntry(body)?.let(journalEntries::add)
                    Kind.WEEKLY_REFLECTION -> parseWeeklyReflection(body)?.let(weeklyReflections::add)
                    Kind.THOUGHT_RECORD -> parseThoughtRecord(body)?.let(thoughtRecords::add)
                    Kind.FUNCTIONAL_ANALYSIS -> parseFunctionalAnalysis(body)?.let(functionalAnalyses::add)
                    Kind.RELAPSE_PLAN -> relapsePlan = parseRelapsePlan(body)
                    Kind.REFUSAL_PHRASE -> parseRefusalPhrase(body)?.let(refusalPhrases::add)
                    Kind.MILESTONE_LETTER -> parseMilestoneLetter(body)?.let(milestoneLetters::add)
                    Kind.TOOL_OPEN_COUNT -> parseToolOpenCount(body)?.let { (tool, count) ->
                        toolOpenCounts[tool] = count
                    }
                    Kind.EVENING_REVIEW -> parseEveningReview(body)?.let(eveningReviews::add)
                    // Unknown kinds: silently ignore — forward compatibility.
                }
            } else {
                i += 1
            }
        }

        return ParsedImport(
            formatVersion = declaredVersion,
            profile = profile,
            checkIns = checkIns,
            cravingEvents = cravingEvents,
            reasons = reasons,
            journalEntries = journalEntries,
            weeklyReflections = weeklyReflections,
            thoughtRecords = thoughtRecords,
            functionalAnalyses = functionalAnalyses,
            relapsePlan = relapsePlan,
            refusalPhrases = refusalPhrases,
            milestoneLetters = milestoneLetters,
            toolOpenCounts = toolOpenCounts.toMap(),
            eveningReviews = eveningReviews,
        )
    }

    // --- Per-entity parsers (private helpers) ---

    private fun parseProfile(body: Map<String, String>): ExportProfile =
        ExportProfile(
            startDateEpochDay = body.longOrNull("start_date_epoch_day"),
            typicalDrinksPerDay = body.intOrNull("typical_drinks_per_day"),
            priceCentsPerDrink = body.intOrNull("price_cents_per_drink"),
            checkInReminderEnabled = body.boolOrNull("checkin_reminder_enabled"),
            checkInReminderHour = body.intOrNull("checkin_reminder_hour"),
            checkInReminderMinute = body.intOrNull("checkin_reminder_minute"),
            // Older exports won't have this key — longOrNull returns null,
            // which the importer treats as "leave the current value alone".
            previousStreaksTotalDays = body.longOrNull("previous_streaks_total_days"),
            hoursPerDrink = body.intOrNull("hours_per_drink"),
            eveningReviewEnabled = body.boolOrNull("evening_review_enabled"),
            companionEnabled = body.boolOrNull("companion_enabled"),
            companionHighestStage = body.intOrNull("companion_highest_stage"),
        )

    private fun parseCheckIn(body: Map<String, String>): CheckIn? {
        val date = body.stringOrNull("date") ?: return null
        return CheckIn(
            date = date,
            didDrink = body.boolOrNull("did_drink") ?: false,
            drinkCount = body.intOrNull("drink_count"),
            mood = body.intOrNull("mood"),
            trigger = body.stringOrNull("trigger"),
            note = body.stringOrNull("note"),
            createdAtEpochMillis = body.longOrNull("created_at") ?: System.currentTimeMillis(),
        )
    }

    private fun parseCravingEvent(body: Map<String, String>): CravingEvent? {
        val tool = body.stringOrNull("tool") ?: return null
        val outcome = body.stringOrNull("outcome") ?: return null
        return CravingEvent(
            timestampEpochMillis = body.longOrNull("timestamp") ?: System.currentTimeMillis(),
            tool = tool,
            outcome = outcome,
            intensity = body.intOrNull("intensity"),
        )
    }

    private fun parseReason(body: Map<String, String>): Reason? {
        val text = body.stringOrNull("text") ?: return null
        return Reason(
            text = text,
            createdEpochMillis = body.longOrNull("created") ?: System.currentTimeMillis(),
        )
    }

    private fun parseJournalEntry(body: Map<String, String>): JournalEntry? {
        val dateIso = body.stringOrNull("date") ?: return null
        val text = body.stringOrNull("text") ?: return null
        return JournalEntry(
            dateIso = dateIso,
            text = text,
            createdAtEpochMillis = body.longOrNull("created_at") ?: System.currentTimeMillis(),
        )
    }

    private fun parseWeeklyReflection(body: Map<String, String>): WeeklyReflection? {
        val weekStart = body.stringOrNull("week_start") ?: return null
        val rating = body.intOrNull("rating") ?: return null
        return WeeklyReflection(
            weekStartIso = weekStart,
            rating = rating,
            note = body.stringOrNull("note"),
            createdAtEpochMillis = body.longOrNull("created_at") ?: System.currentTimeMillis(),
        )
    }

    private fun parseThoughtRecord(body: Map<String, String>): ThoughtRecord? {
        val date = body.stringOrNull("date") ?: return null
        return ThoughtRecord(
            dateIso = date,
            situation = body.stringOrNull("situation").orEmpty(),
            thought = body.stringOrNull("thought").orEmpty(),
            challenge = body.stringOrNull("challenge").orEmpty(),
            friendReframe = body.stringOrNull("friend_reframe").orEmpty(),
            distortionTag = body.stringOrNull("distortion_tag"),
            createdAtEpochMillis = body.longOrNull("created_at") ?: System.currentTimeMillis(),
        )
    }

    private fun parseFunctionalAnalysis(body: Map<String, String>): FunctionalAnalysis? {
        val date = body.stringOrNull("date") ?: return null
        val a = body.stringOrNull("antecedent")
        val t = body.stringOrNull("thought_at_moment")
        val f = body.stringOrNull("following_action")
        if (a == null && t == null && f == null) return null
        return FunctionalAnalysis(
            dateIso = date,
            cravingEventId = body.longOrNull("craving_event_id"),
            antecedent = a,
            thoughtAtMoment = t,
            followingAction = f,
            createdAtEpochMillis = body.longOrNull("created_at") ?: System.currentTimeMillis(),
        )
    }

    private fun parseRelapsePlan(body: Map<String, String>): RelapsePlan =
        RelapsePlan(
            id = RelapsePlan.SINGLETON_ID,
            highRiskSituations = body.stringOrNull("high_risk_situations").orEmpty(),
            earlyWarningSigns = body.stringOrNull("early_warning_signs").orEmpty(),
            copingPlan = body.stringOrNull("coping_plan").orEmpty(),
            updatedAtEpochMillis = body.longOrNull("updated_at") ?: System.currentTimeMillis(),
        )

    private fun parseRefusalPhrase(body: Map<String, String>): RefusalPhrase? {
        val text = body.stringOrNull("text") ?: return null
        return RefusalPhrase(
            text = text,
            createdEpochMillis = body.longOrNull("created") ?: System.currentTimeMillis(),
        )
    }

    private fun parseMilestoneLetter(body: Map<String, String>): MilestoneLetter? {
        val days = body.intOrNull("milestone_days") ?: return null
        val text = body.stringOrNull("text") ?: return null
        return MilestoneLetter(
            milestoneDays = days,
            text = text,
            writtenAtEpochMillis = body.longOrNull("written_at") ?: System.currentTimeMillis(),
            revealedAtEpochMillis = body.longOrNull("revealed_at"),
        )
    }

    private fun parseToolOpenCount(body: Map<String, String>): Pair<String, Int>? {
        val tool = body.stringOrNull("tool") ?: return null
        val count = body.intOrNull("count") ?: return null
        if (count < 0) return null
        return tool to count
    }

    private fun parseEveningReview(body: Map<String, String>): EveningReview? {
        val date = body.stringOrNull("date") ?: return null
        return EveningReview(
            dateIso = date,
            // Either field may be empty (caller already enforced "at least one
            // is non-blank" at write time). Keep whatever was on disk verbatim.
            winText = body.stringOrNull("win").orEmpty(),
            challengeText = body.stringOrNull("challenge").orEmpty(),
            createdAtEpochMillis = body.longOrNull("created_at") ?: System.currentTimeMillis(),
        )
    }

    // --- Tiny helpers for the parsers ---

    private fun Map<String, String>.stringOrNull(key: String): String? {
        val raw = this[key] ?: return null
        if (raw.isEmpty()) return null
        return unescape(raw)
    }

    private fun Map<String, String>.intOrNull(key: String): Int? =
        this[key]?.takeIf { it.isNotEmpty() }?.toIntOrNull()

    private fun Map<String, String>.longOrNull(key: String): Long? =
        this[key]?.takeIf { it.isNotEmpty() }?.toLongOrNull()

    private fun Map<String, String>.boolOrNull(key: String): Boolean? {
        val raw = this[key] ?: return null
        if (raw.isEmpty()) return null
        return when (raw.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }
}

/**
 * Plain DTO that mirrors the bits of UserProfile we actually round-trip.
 * (We don't round-trip `onboardingComplete` or `goalMode` — those are
 * re-derived at import time.)
 */
data class ExportProfile(
    val startDateEpochDay: Long?,
    val typicalDrinksPerDay: Int?,
    val priceCentsPerDrink: Int?,
    val checkInReminderEnabled: Boolean?,
    val checkInReminderHour: Int?,
    val checkInReminderMinute: Int?,
    /**
     * Banked dry days from previous streaks (PHASE_2 §9A4). Absent in older
     * exports — tolerated at import time; treated as 0 by the importer.
     */
    val previousStreaksTotalDays: Long? = null,
    /**
     * Hours-per-drink estimate for the "hours reclaimed" Stats card
     * (PHASE_2 §9A2). Absent in older exports.
     */
    val hoursPerDrink: Int? = null,
    /** Evening mini-review opt-in (PHASE_2 §9A5). Absent in older exports. */
    val eveningReviewEnabled: Boolean? = null,
    /** Visual companion opt-in (PHASE_2 §9B1). Absent in older exports. */
    val companionEnabled: Boolean? = null,
    /** Companion high-water mark, 1..5 (PHASE_2 §9B1). Absent in older exports. */
    val companionHighestStage: Int? = null,
)

/** Snapshot of the DB + profile to hand to the serialiser. */
data class ExportSnapshot(
    val profile: ExportProfile?,
    val checkIns: List<CheckIn>,
    val cravingEvents: List<CravingEvent>,
    val reasons: List<Reason>,
    val journalEntries: List<JournalEntry>,
    val weeklyReflections: List<WeeklyReflection>,
    val thoughtRecords: List<ThoughtRecord>,
    val functionalAnalyses: List<FunctionalAnalysis>,
    val relapsePlan: RelapsePlan?,
    val refusalPhrases: List<RefusalPhrase>,
    val milestoneLetters: List<MilestoneLetter> = emptyList(),
    val toolOpenCounts: Map<String, Int> = emptyMap(),
    val eveningReviews: List<EveningReview> = emptyList(),
)

/**
 * Result of decoding an export file. The importer's job is to take this and
 * upsert it into the DB with sensible "merge, don't wipe" semantics.
 */
data class ParsedImport(
    val formatVersion: Int?,
    val profile: ExportProfile?,
    val checkIns: List<CheckIn>,
    val cravingEvents: List<CravingEvent>,
    val reasons: List<Reason>,
    val journalEntries: List<JournalEntry>,
    val weeklyReflections: List<WeeklyReflection>,
    val thoughtRecords: List<ThoughtRecord>,
    val functionalAnalyses: List<FunctionalAnalysis>,
    val relapsePlan: RelapsePlan?,
    val refusalPhrases: List<RefusalPhrase>,
    val milestoneLetters: List<MilestoneLetter> = emptyList(),
    val toolOpenCounts: Map<String, Int> = emptyMap(),
    val eveningReviews: List<EveningReview> = emptyList(),
) {
    fun totalRows(): Int =
        checkIns.size + cravingEvents.size + reasons.size + journalEntries.size +
            weeklyReflections.size + thoughtRecords.size + functionalAnalyses.size +
            refusalPhrases.size + milestoneLetters.size + toolOpenCounts.size +
            eveningReviews.size +
            (if (relapsePlan != null) 1 else 0) +
            (if (profile != null) 1 else 0)
}
