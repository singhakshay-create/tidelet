package com.tidelet.app.data.export

import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Glue between the repository and the pure [TideletMarkdown] codec.
 *
 * The codec is deliberately DB-agnostic so it can be unit-tested. This class
 * knows about Room and does the actual suspend-fun reads + writes.
 *
 * **Import semantics** are merge-forward, not destructive:
 *
 *   - Existing rows stay where they are.
 *   - Imported rows are inserted with their original timestamps (via the
 *     repo's `importX` methods), so historical order in the Log and Stats
 *     screens is preserved.
 *   - Singleton entities (profile, relapse plan) are overwritten.
 *   - Check-ins and weekly reflections use upsert — same date = replace.
 *   - Everything else uses plain insert — duplicates are possible if the user
 *     imports the same file twice, but we'd rather err on the side of not
 *     losing data.
 *
 * If we later want a "wipe then import" mode, add a boolean flag. For v1,
 * merge-forward is the safer default.
 */
class ExportImportManager(private val repo: TideletRepository) {

    /**
     * Build a snapshot of everything in the DB + DataStore and return the
     * Markdown document as a string. The caller is expected to write the
     * bytes to disk via the Storage Access Framework.
     */
    suspend fun buildExport(): String {
        val profile = repo.profileSnapshot()
        val snapshot = ExportSnapshot(
            profile = ExportProfile(
                startDateEpochDay = profile.startDateEpochDay,
                typicalDrinksPerDay = profile.typicalDrinksPerDay,
                priceCentsPerDrink = profile.priceCentsPerDrink,
                checkInReminderEnabled = profile.checkInReminderEnabled,
                checkInReminderHour = profile.checkInReminderHour,
                checkInReminderMinute = profile.checkInReminderMinute,
                previousStreaksTotalDays = profile.previousStreaksTotalDays,
                hoursPerDrink = profile.hoursPerDrink,
                eveningReviewEnabled = profile.eveningReviewEnabled,
                companionEnabled = profile.companionEnabled,
                companionHighestStage = profile.companionHighestStage,
            ),
            checkIns = repo.checkIns.first(),
            cravingEvents = repo.cravingEvents.first(),
            reasons = repo.reasons.first(),
            journalEntries = repo.journalEntries.first(),
            weeklyReflections = repo.weeklyReflections.first(),
            thoughtRecords = repo.thoughtRecords.first(),
            functionalAnalyses = repo.functionalAnalyses.first(),
            relapsePlan = repo.findRelapsePlan(),
            refusalPhrases = repo.refusalPhrases.first(),
            milestoneLetters = repo.milestoneLetters.first(),
            toolOpenCounts = repo.toolOpenCounts.first(),
            eveningReviews = repo.eveningReviews.first(),
        )

        return TideletMarkdown.encode(
            snapshot = snapshot,
            exportedAtIso = Instant.now().toString(),
        )
    }

    /**
     * Parse the Markdown and merge-forward into the DB. Returns an
     * [ImportReport] describing what landed — useful for the UI to show a
     * quick "X rows imported" toast.
     *
     * Throws [IllegalArgumentException] if the document clearly isn't one of
     * ours (no format banner and no tidelet fences). The caller should catch
     * and surface a friendly error.
     */
    suspend fun applyImport(markdown: String): ImportReport {
        val parsed = TideletMarkdown.decode(markdown)

        if (parsed.formatVersion == null && parsed.totalRows() == 0) {
            throw IllegalArgumentException("Not a Tidelet export file")
        }

        // Profile (restore before the row-level imports so derived widgets
        // like streaks are accurate immediately)
        parsed.profile?.let { p ->
            repo.restoreProfile(
                startDateEpochDay = p.startDateEpochDay,
                typicalDrinksPerDay = p.typicalDrinksPerDay,
                priceCentsPerDrink = p.priceCentsPerDrink,
                checkInReminderEnabled = p.checkInReminderEnabled,
                checkInReminderHour = p.checkInReminderHour,
                checkInReminderMinute = p.checkInReminderMinute,
                previousStreaksTotalDays = p.previousStreaksTotalDays,
                hoursPerDrink = p.hoursPerDrink,
                eveningReviewEnabled = p.eveningReviewEnabled,
                companionEnabled = p.companionEnabled,
                companionHighestStage = p.companionHighestStage,
            )
        }

        parsed.checkIns.forEach { repo.importCheckIn(it) }
        parsed.cravingEvents.forEach { repo.importCravingEvent(it) }
        parsed.reasons.forEach { repo.importReason(it) }
        parsed.journalEntries.forEach { repo.importJournalEntry(it) }
        parsed.weeklyReflections.forEach { repo.importWeeklyReflection(it) }
        parsed.thoughtRecords.forEach { repo.importThoughtRecord(it) }
        parsed.functionalAnalyses.forEach { repo.importFunctionalAnalysis(it) }
        parsed.refusalPhrases.forEach { repo.importRefusalPhrase(it) }
        parsed.milestoneLetters.forEach { repo.importMilestoneLetter(it) }
        parsed.eveningReviews.forEach { repo.importEveningReview(it) }

        // Tool-open counts: replace-in-place. The intent is "this backup's
        // counts are the source of truth," which matches how we handle the
        // profile. If the user wants to accumulate they can re-open each
        // tool after import.
        if (parsed.toolOpenCounts.isNotEmpty()) {
            repo.setToolOpenCounts(parsed.toolOpenCounts)
        }

        parsed.relapsePlan?.let {
            repo.saveRelapsePlan(
                highRiskSituations = it.highRiskSituations,
                earlyWarningSigns = it.earlyWarningSigns,
                copingPlan = it.copingPlan,
            )
        }

        return ImportReport(
            formatVersion = parsed.formatVersion,
            profileRestored = parsed.profile != null,
            checkIns = parsed.checkIns.size,
            cravingEvents = parsed.cravingEvents.size,
            reasons = parsed.reasons.size,
            journalEntries = parsed.journalEntries.size,
            weeklyReflections = parsed.weeklyReflections.size,
            thoughtRecords = parsed.thoughtRecords.size,
            functionalAnalyses = parsed.functionalAnalyses.size,
            refusalPhrases = parsed.refusalPhrases.size,
            milestoneLetters = parsed.milestoneLetters.size,
            toolOpenCounts = parsed.toolOpenCounts.size,
            eveningReviews = parsed.eveningReviews.size,
            relapsePlanRestored = parsed.relapsePlan != null,
        )
    }
}

data class ImportReport(
    val formatVersion: Int?,
    val profileRestored: Boolean,
    val checkIns: Int,
    val cravingEvents: Int,
    val reasons: Int,
    val journalEntries: Int,
    val weeklyReflections: Int,
    val thoughtRecords: Int,
    val functionalAnalyses: Int,
    val refusalPhrases: Int,
    val milestoneLetters: Int = 0,
    val toolOpenCounts: Int = 0,
    val eveningReviews: Int = 0,
    val relapsePlanRestored: Boolean,
) {
    val totalRows: Int
        get() = checkIns + cravingEvents + reasons + journalEntries + weeklyReflections +
            thoughtRecords + functionalAnalyses + refusalPhrases +
            milestoneLetters + toolOpenCounts + eveningReviews
}
