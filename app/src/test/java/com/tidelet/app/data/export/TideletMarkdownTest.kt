package com.tidelet.app.data.export

import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CheckIn
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.FunctionalAnalysis
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.MilestoneLetter
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.db.RefusalPhrase
import com.tidelet.app.data.db.RelapsePlan
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.db.WeeklyReflection
import org.junit.Test

/**
 * Pure-JVM tests for the Markdown codec. These guard the round-trip contract:
 * a user's export, re-imported into a fresh install, must come back exactly as
 * it went in (modulo auto-id renumbering). See PHASE_2_SPEC.md §2.
 */
class TideletMarkdownTest {

    @Test
    fun `encode then decode round-trips checkIns and reasons`() {
        val snapshot = ExportSnapshot(
            profile = ExportProfile(
                startDateEpochDay = 20_000L,
                typicalDrinksPerDay = 3,
                priceCentsPerDrink = 800,
                checkInReminderEnabled = false,
                checkInReminderHour = 20,
                checkInReminderMinute = 0,
            ),
            checkIns = listOf(
                CheckIn(date = "2026-04-18", didDrink = false, mood = 4, note = "good day"),
                CheckIn(date = "2026-04-19", didDrink = true, drinkCount = 2, trigger = "stress"),
            ),
            cravingEvents = emptyList(),
            reasons = listOf(
                Reason(id = 1L, text = "my kids", createdEpochMillis = 1_000L),
                Reason(id = 2L, text = "my health", createdEpochMillis = 2_000L),
            ),
            journalEntries = emptyList(),
            weeklyReflections = emptyList(),
            thoughtRecords = emptyList(),
            functionalAnalyses = emptyList(),
            relapsePlan = null,
            refusalPhrases = emptyList(),
            milestoneLetters = emptyList(),
        )

        val markdown = TideletMarkdown.encode(snapshot, exportedAtIso = "2026-04-20T12:00:00Z")
        val parsed = TideletMarkdown.decode(markdown)

        assertThat(parsed.checkIns).hasSize(2)
        assertThat(parsed.checkIns.first().date).isEqualTo("2026-04-18")
        assertThat(parsed.reasons.map { it.text }).containsExactly("my kids", "my health")
        assertThat(parsed.profile?.typicalDrinksPerDay).isEqualTo(3)
        assertThat(parsed.profile?.priceCentsPerDrink).isEqualTo(800)
    }

    @Test
    fun `empty snapshot encodes and decodes without errors`() {
        val snapshot = ExportSnapshot(
            profile = null,
            checkIns = emptyList(),
            cravingEvents = emptyList(),
            reasons = emptyList(),
            journalEntries = emptyList(),
            weeklyReflections = emptyList(),
            thoughtRecords = emptyList(),
            functionalAnalyses = emptyList(),
            relapsePlan = null,
            refusalPhrases = emptyList(),
            milestoneLetters = emptyList(),
        )

        val markdown = TideletMarkdown.encode(snapshot, exportedAtIso = "2026-04-20T12:00:00Z")
        val parsed = TideletMarkdown.decode(markdown)

        assertThat(parsed.totalRows()).isEqualTo(0)
        assertThat(parsed.profile).isNull()
    }

    @Test
    fun `unicode in user text survives round trip`() {
        val snapshot = ExportSnapshot(
            profile = null,
            checkIns = emptyList(),
            cravingEvents = emptyList(),
            reasons = listOf(
                Reason(text = "आज का दिन – चाय, धूप, सन्नाटा."),  // Hindi + em-dash
                Reason(text = "für die Familie 🌱"),               // German + emoji
            ),
            journalEntries = emptyList(),
            weeklyReflections = emptyList(),
            thoughtRecords = emptyList(),
            functionalAnalyses = emptyList(),
            relapsePlan = null,
            refusalPhrases = emptyList(),
            milestoneLetters = emptyList(),
        )

        val markdown = TideletMarkdown.encode(snapshot, exportedAtIso = "2026-04-20T12:00:00Z")
        val parsed = TideletMarkdown.decode(markdown)

        val texts = parsed.reasons.map { it.text }
        assertThat(texts).contains("आज का दिन – चाय, धूप, सन्नाटा.")
        assertThat(texts).contains("für die Familie 🌱")
    }

    @Test
    fun `decoding random text without tidelet markers returns empty parse`() {
        val garbage = "This is some completely unrelated document\n\nwith no tidelet structure."

        val parsed = TideletMarkdown.decode(garbage)

        assertThat(parsed.totalRows()).isEqualTo(0)
        assertThat(parsed.formatVersion).isNull()
    }

    @Test
    fun `round trip preserves thought records with distortion tags`() {
        val snapshot = ExportSnapshot(
            profile = null,
            checkIns = emptyList(),
            cravingEvents = emptyList(),
            reasons = emptyList(),
            journalEntries = emptyList(),
            weeklyReflections = emptyList(),
            thoughtRecords = listOf(
                ThoughtRecord(
                    dateIso = "2026-04-18",
                    situation = "Friday afternoon, office drinks",
                    thought = "I need this to relax",
                    challenge = "Is that 100% true?",
                    friendReframe = "A walk would also relax me",
                    distortionTag = "permission_giving",
                ),
            ),
            functionalAnalyses = emptyList(),
            relapsePlan = null,
            refusalPhrases = emptyList(),
            milestoneLetters = emptyList(),
        )

        val markdown = TideletMarkdown.encode(snapshot, exportedAtIso = "2026-04-20T12:00:00Z")
        val parsed = TideletMarkdown.decode(markdown)

        assertThat(parsed.thoughtRecords).hasSize(1)
        assertThat(parsed.thoughtRecords.first().distortionTag).isEqualTo("permission_giving")
        assertThat(parsed.thoughtRecords.first().situation).isEqualTo("Friday afternoon, office drinks")
    }

    @Test
    fun `round-trips milestone letters with revealedAt null and non-null`() {
        val snapshot = emptySnapshot().copy(
            milestoneLetters = listOf(
                MilestoneLetter(
                    milestoneDays = 7,
                    text = "Hi future me.\nThis is only day 7.",
                    writtenAtEpochMillis = 1_700_000_000L,
                    revealedAtEpochMillis = null,
                ),
                MilestoneLetter(
                    milestoneDays = 30,
                    text = "You made it. Keep going.",
                    writtenAtEpochMillis = 1_800_000_000L,
                    revealedAtEpochMillis = 1_810_000_000L,
                ),
            ),
        )

        val md = TideletMarkdown.encode(snapshot, exportedAtIso = "2026-04-20T12:00:00Z")
        val parsed = TideletMarkdown.decode(md)

        assertThat(parsed.milestoneLetters).hasSize(2)

        val seven = parsed.milestoneLetters.first { it.milestoneDays == 7 }
        assertThat(seven.text).isEqualTo("Hi future me.\nThis is only day 7.")
        assertThat(seven.writtenAtEpochMillis).isEqualTo(1_700_000_000L)
        assertThat(seven.revealedAtEpochMillis).isNull()

        val thirty = parsed.milestoneLetters.first { it.milestoneDays == 30 }
        assertThat(thirty.revealedAtEpochMillis).isEqualTo(1_810_000_000L)
    }

    @Test
    fun `round-trips tool-open counts`() {
        val snapshot = emptySnapshot().copy(
            toolOpenCounts = mapOf(
                "breathe" to 23,
                "ride_the_wave" to 7,
                "reasons" to 4,
            ),
        )

        val md = TideletMarkdown.encode(snapshot, exportedAtIso = "2026-04-20T12:00:00Z")
        val parsed = TideletMarkdown.decode(md)

        assertThat(parsed.toolOpenCounts).containsExactly(
            "breathe", 23,
            "ride_the_wave", 7,
            "reasons", 4,
        )
    }

    @Test
    fun `negative tool count is rejected on decode`() {
        // Craft a minimal valid document with a single tool-open-count fence
        // whose count is negative — the parser should drop it.
        val md = """
            # Tidelet Export

            <!-- format: tidelet-md v1; exported: 2026-04-20T12:00:00Z -->

            ```tidelet tool-open-count
            tool = breathe
            count = -5
            ```
        """.trimIndent()

        val parsed = TideletMarkdown.decode(md)
        assertThat(parsed.toolOpenCounts).isEmpty()
    }

    private fun emptySnapshot(): ExportSnapshot = ExportSnapshot(
        profile = null,
        checkIns = emptyList(),
        cravingEvents = emptyList(),
        reasons = emptyList(),
        journalEntries = emptyList(),
        weeklyReflections = emptyList(),
        thoughtRecords = emptyList(),
        functionalAnalyses = emptyList(),
        relapsePlan = null,
        refusalPhrases = emptyList(),
    )

    @Test
    fun `unused fixtures compile — future round-trip coverage hooks`() {
        // These fixtures are referenced to ensure their constructors keep
        // compiling against the codec. Expand the encode/decode tests above
        // to cover each in turn as the codec grows.
        @Suppress("UNUSED_VARIABLE")
        val cravingEvent = CravingEvent(tool = "breathe", outcome = "got_through")
        @Suppress("UNUSED_VARIABLE")
        val journal = JournalEntry(dateIso = "2026-04-18", text = "thinking of you")
        @Suppress("UNUSED_VARIABLE")
        val weekly = WeeklyReflection(weekStartIso = "2026-04-13", rating = 4, note = null)
        @Suppress("UNUSED_VARIABLE")
        val analysis = FunctionalAnalysis(dateIso = "2026-04-18", antecedent = "stress")
        @Suppress("UNUSED_VARIABLE")
        val plan = RelapsePlan(
            highRiskSituations = "after work",
            earlyWarningSigns = "thinking it's deserved",
            copingPlan = "call a friend",
        )
        @Suppress("UNUSED_VARIABLE")
        val phrase = RefusalPhrase(text = "thanks, I'm not drinking this month")
        @Suppress("UNUSED_VARIABLE")
        val letter = MilestoneLetter(milestoneDays = 30, text = "future me: hello")
    }
}
