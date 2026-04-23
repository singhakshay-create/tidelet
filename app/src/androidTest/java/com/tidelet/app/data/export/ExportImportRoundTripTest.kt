package com.tidelet.app.data.export

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.TideletDatabase
import com.tidelet.app.data.prefs.UserPreferences
import com.tidelet.app.data.repo.RoomTideletRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * End-to-end export → wipe → import → verify (PHASE_2 §2). Mostly checks
 * that every collection survives the trip with the right row count; per-row
 * assertions go after that on the canonical-content fields.
 *
 * In-memory Room + the real DataStore makes this an integration test rather
 * than a unit one; it lives in androidTest so we get the AndroidX context.
 */
@RunWith(AndroidJUnit4::class)
class ExportImportRoundTripTest {

    private lateinit var db: TideletDatabase
    private lateinit var prefs: UserPreferences
    private lateinit var repo: RoomTideletRepository
    private lateinit var manager: ExportImportManager

    @Before
    fun setUp() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, TideletDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = UserPreferences(ctx)
        prefs.clearAll()
        repo = RoomTideletRepository(db, prefs)
        manager = ExportImportManager(repo)
    }

    @After
    fun tearDown() = runTest {
        db.close()
        prefs.clearAll()
    }

    @Test
    fun fullSnapshot_roundTrips_throughExportAndImport() = runTest {
        // ---- Seed every collection that the codec touches ----
        repo.completeOnboarding(LocalDate.of(2026, 1, 1))
        repo.upsertCheckIn(
            date = LocalDate.of(2026, 4, 18),
            didDrink = false,
            mood = 7,
            note = "Felt steady — no major triggers.",
        )
        repo.addReason("My kids")
        repo.addReason("How clear mornings feel now")
        repo.logCravingEvent(tool = "breathe", outcome = "got_through")
        repo.addJournalEntry(
            date = LocalDate.of(2026, 4, 18),
            text = "Wrote out the spiral and it dissolved.",
        )
        repo.addThoughtRecord(
            date = LocalDate.of(2026, 4, 18),
            situation = "Friday office drinks",
            thought = "I'll never get through this.",
            challenge = "I've gotten through 30 of these.",
            friendReframe = "One Friday at a time.",
            distortionTag = "all_or_nothing",
        )
        repo.upsertEveningReview(
            dateIso = "2026-04-18",
            winText = "Said no without fanfare.",
            challengeText = "Caught a 6pm urge — used Breathe.",
        )
        // Profile-side scalars that the §9A2 / §9A4 / §9B1 work added.
        repo.setStartDate(LocalDate.of(2026, 1, 1))
        prefs.setDrinkingBaseline(typicalDrinksPerDay = 4, priceCentsPerDrink = 1000)
        prefs.setHoursPerDrink(2)
        prefs.addPreviousStreakDays(7L)
        prefs.setEveningReviewEnabled(true)
        prefs.setCompanionEnabled(true)
        prefs.raiseCompanionHighestStage(3)

        // ---- Export → wipe → import ----
        val markdown = manager.buildExport()
        assertThat(markdown).contains("Tidelet Export")
        repo.wipeAll()
        // Confirm the wipe actually happened (sanity).
        assertThat(repo.checkIns.first()).isEmpty()
        assertThat(repo.eveningReviews.first()).isEmpty()

        val report = manager.applyImport(markdown)
        assertThat(report.profileRestored).isTrue()

        // ---- Per-collection assertions: count + a representative field ----
        val checkIns = repo.checkIns.first()
        assertThat(checkIns).hasSize(1)
        assertThat(checkIns.first().mood).isEqualTo(7)

        assertThat(repo.reasons.first().map { it.text })
            .containsExactly("My kids", "How clear mornings feel now")

        val cravings = repo.cravingEvents.first()
        assertThat(cravings).hasSize(1)
        assertThat(cravings.first().tool).isEqualTo("breathe")

        val entries = repo.journalEntries.first()
        assertThat(entries).hasSize(1)
        assertThat(entries.first().text).contains("dissolved")

        val thoughts = repo.thoughtRecords.first()
        assertThat(thoughts).hasSize(1)
        assertThat(thoughts.first().distortionTag).isEqualTo("all_or_nothing")

        val reviews = repo.eveningReviews.first()
        assertThat(reviews).hasSize(1)
        assertThat(reviews.first().winText).contains("fanfare")

        // Profile-side scalars survived too.
        val profile = repo.profileSnapshot()
        assertThat(profile.typicalDrinksPerDay).isEqualTo(4)
        assertThat(profile.priceCentsPerDrink).isEqualTo(1000)
        assertThat(profile.hoursPerDrink).isEqualTo(2)
        assertThat(profile.previousStreaksTotalDays).isEqualTo(7L)
        assertThat(profile.eveningReviewEnabled).isTrue()
        assertThat(profile.companionEnabled).isTrue()
        assertThat(profile.companionHighestStage).isEqualTo(3)
    }

    @Test
    fun emptySnapshot_roundTrips_withoutErrors() = runTest {
        // Nothing seeded — export is essentially the banner + maybe a profile
        // (defaults). Import should succeed without throwing or partial writes.
        val markdown = manager.buildExport()
        repo.wipeAll()
        val report = manager.applyImport(markdown)

        assertThat(report.checkIns).isEqualTo(0)
        assertThat(report.reasons).isEqualTo(0)
        assertThat(repo.checkIns.first()).isEmpty()
        assertThat(repo.reasons.first()).isEmpty()
    }

    @Test
    fun roundTrip_preservesUnicodeContent() = runTest {
        // Wide net for non-ASCII: smart quotes, emoji, RTL, CJK. The codec
        // historically used a custom escape for newlines; this guards against
        // regressions in that path mangling user text.
        val unicodeText = "Mañana — مرحبًا — 早上好 — \"smart quotes\" — \uD83C\uDF31"
        repo.addJournalEntry(date = LocalDate.of(2026, 4, 18), text = unicodeText)
        val markdown = manager.buildExport()

        repo.wipeAll()
        manager.applyImport(markdown)

        val entries = repo.journalEntries.first()
        assertThat(entries).hasSize(1)
        assertThat(entries.first().text).isEqualTo(unicodeText)
    }
}
