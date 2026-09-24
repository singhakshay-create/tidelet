package com.tidelet.app.util

import com.google.common.truth.Truth.assertThat
import com.tidelet.app.fakes.FakeTideletRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * Exercises repository-level input-validation contracts against the fake.
 * The fake replicates the same trimming / skip-blank rules as
 * [com.tidelet.app.data.repo.RoomTideletRepository], so these double as a
 * specification for what those rules must be.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ValidationTest {

    @Test
    fun `addReason with only whitespace is a no-op`() = runTest {
        val repo = FakeTideletRepository()

        repo.addReason("   ")
        repo.addReason("")

        assertThat(repo.recordedReasonsAdded).isEmpty()
    }

    @Test
    fun `addReason trims surrounding whitespace`() = runTest {
        val repo = FakeTideletRepository()

        repo.addReason("  hello  ")

        assertThat(repo.recordedReasonsAdded).containsExactly("hello")
    }

    @Test
    fun `addRefusalPhrase with only whitespace is a no-op`() = runTest {
        val repo = FakeTideletRepository()

        repo.addRefusalPhrase("\t\n ")

        // No state change: the phrases list stays empty. first() takes one
        // value and stops; collect {} on this hot flow would never return.
        assertThat(repo.refusalPhrases.first()).isEmpty()
    }

    @Test
    fun `addJournalEntry returns -1 when the text is blank`() = runTest {
        val repo = FakeTideletRepository()

        val id = repo.addJournalEntry(LocalDate.of(2026, 4, 18), "   ")

        assertThat(id).isEqualTo(-1L)
    }

    @Test
    fun `addFunctionalAnalysis returns -1 when every field is blank`() = runTest {
        val repo = FakeTideletRepository()

        val id = repo.addFunctionalAnalysis(
            date = LocalDate.of(2026, 4, 18),
            antecedent = null,
            thoughtAtMoment = "   ",
            followingAction = "",
        )

        assertThat(id).isEqualTo(-1L)
        assertThat(repo.recordedFunctionalAnalyses).isEmpty()
    }

    @Test
    fun `addFunctionalAnalysis writes when any one field has content`() = runTest {
        val repo = FakeTideletRepository()

        val id = repo.addFunctionalAnalysis(
            date = LocalDate.of(2026, 4, 18),
            antecedent = "stress",
        )

        assertThat(id).isGreaterThan(0L)
        assertThat(repo.recordedFunctionalAnalyses).hasSize(1)
    }
}
