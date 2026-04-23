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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Defends the import path against malformed input (PHASE_2 §2 acceptance).
 *
 * Two regimes:
 *   1. Plainly-not-a-tidelet-export inputs throw [IllegalArgumentException].
 *      The Settings UI catches the throw and shows a friendly error.
 *   2. Inputs that look like a tidelet export but contain garbled rows
 *      ignore the bad rows silently rather than aborting — partial
 *      acceptance is preferable to losing a whole backup over one bad
 *      character.
 *
 * The critical invariant either way: pre-existing live data is never
 * mutated by an attempted import that fails.
 */
@RunWith(AndroidJUnit4::class)
class ExportImportCorruptInputTest {

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
    fun emptyString_throwsAndDoesNotMutate() = runTest {
        repo.addReason("kept")  // pre-existing live data
        try {
            manager.applyImport("")
            fail("Expected IllegalArgumentException for an empty input")
        } catch (_: IllegalArgumentException) {
            // expected — applyImport recognises this as "not a tidelet export"
        }
        assertThat(repo.reasons.first().map { it.text }).containsExactly("kept")
    }

    @Test
    fun gibberishInput_throwsAndDoesNotMutate() = runTest {
        repo.addReason("kept")
        try {
            manager.applyImport("hello world\nthis is just plain text\nno fences here")
            fail("Expected IllegalArgumentException for plain-text input")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertThat(repo.reasons.first().map { it.text }).containsExactly("kept")
    }

    @Test
    fun truncatedFence_doesNotCrash_andStillProcessesEarlierRows() = runTest {
        // Build a valid export, then truncate the closing fence on the last
        // entry. The format-version banner survives, so the importer treats
        // the file as ours and salvages what it can.
        repo.addReason("alpha")
        repo.addReason("beta")
        val good = manager.buildExport()
        val truncated = good.substringBeforeLast("```")  // drop final close fence

        repo.wipeAll()
        // Should not throw even though the last block is malformed.
        manager.applyImport(truncated)

        // The "alpha" row sits in a complete fence near the top → imports.
        // The "beta" row's fence may or may not survive truncation; we just
        // assert the import didn't blow up and at least one row landed.
        val reasons = repo.reasons.first().map { it.text }
        assertThat(reasons).contains("alpha")
    }

    @Test
    fun unrecognisedKind_isIgnored_existingValidRowsImport() = runTest {
        // Forge a file with our banner + one valid reason + one made-up kind.
        val forged = """
            # Tidelet Export
            <!-- format: tidelet-md v1; exported: 2026-04-18T10:00:00Z -->

            ## Reasons (1)

            ```tidelet reason
            text = forged
            created = 1700000000000
            ```

            ## Mystery (1)

            ```tidelet mystery-future-kind
            field_we_dont_know = 42
            ```
        """.trimIndent()

        manager.applyImport(forged)

        // The valid reason imported; the unknown kind silently skipped.
        assertThat(repo.reasons.first().map { it.text }).containsExactly("forged")
    }

    @Test
    fun malformedDateField_doesNotImportThatRow_butKeepsTheRest() = runTest {
        // Build a real export with one check-in, then corrupt the date in
        // the second one we splice in. parseCheckIn drops a row whose `date`
        // field is missing, so the resulting check-ins list contains only
        // the one valid row.
        repo.upsertCheckIn(date = LocalDate.of(2026, 4, 18), didDrink = false)
        val good = manager.buildExport()

        // Splice in a fence with no date= line (mandatory key is missing).
        val malformed = good + "\n\n```tidelet checkin\ndid_drink = false\n```\n"

        repo.wipeAll()
        manager.applyImport(malformed)

        val checkIns = repo.checkIns.first()
        // Only the valid one survived.
        assertThat(checkIns).hasSize(1)
        assertThat(checkIns.first().date).isEqualTo("2026-04-18")
    }
}
