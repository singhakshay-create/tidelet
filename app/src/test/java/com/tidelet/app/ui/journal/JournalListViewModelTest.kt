package com.tidelet.app.ui.journal

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.MainDispatcherRule
import com.tidelet.app.testutil.TestTideletApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class JournalListViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app: TestTideletApplication
        get() = RuntimeEnvironment.getApplication() as TestTideletApplication

    private val fakeRepo: FakeTideletRepository
        get() = app.repository as FakeTideletRepository

    private fun entry(id: Long, text: String, day: Int = id.toInt()) = JournalEntry(
        id = id,
        dateIso = "2025-04-%02d".format(day),
        text = text,
        createdAtEpochMillis = id * 1000L,
    )

    @Test
    fun `empty repo reports empty state`() = runTest {
        val vm = JournalListViewModel(app)
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded) s = awaitItem()
            assertThat(s.isEmpty).isTrue()
            assertThat(s.filtered).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty query surfaces all entries`() = runTest {
        fakeRepo.seedJournalEntries(listOf(entry(1, "alpha"), entry(2, "beta")))
        val vm = JournalListViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (!s.loaded || s.all.isEmpty()) s = awaitItem()
            assertThat(s.filtered.map { it.text }).containsExactly("alpha", "beta")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setQuery filters entries by case-insensitive substring`() = runTest {
        fakeRepo.seedJournalEntries(
            listOf(
                entry(1, "This week felt calm."),
                entry(2, "Wanted a beer tonight but skipped it."),
                entry(3, "Calm is a practice, not a mood."),
            )
        )
        val vm = JournalListViewModel(app)

        vm.setQuery("CALM")
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded || s.all.isEmpty() || s.filtered.size == 3) s = awaitItem()

            assertThat(s.isSearching).isTrue()
            assertThat(s.filtered.map { it.id }).containsExactly(1L, 3L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query with no substring matches reports hasNoMatches`() = runTest {
        fakeRepo.seedJournalEntries(listOf(entry(1, "hello")))
        val vm = JournalListViewModel(app)

        vm.setQuery("nothing-matches")
        vm.state.test {
            var s = awaitItem()
            // Wait for the seeded entries to arrive AND the query to apply.
            while (!s.loaded || s.all.isEmpty() || !s.isSearching) s = awaitItem()
            assertThat(s.filtered).isEmpty()
            assertThat(s.hasNoMatches).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearQuery restores all entries`() = runTest {
        fakeRepo.seedJournalEntries(listOf(entry(1, "alpha")))
        val vm = JournalListViewModel(app)

        vm.setQuery("zzz")
        vm.clearQuery()
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded || s.all.isEmpty() || s.isSearching) s = awaitItem()
            assertThat(s.filtered.map { it.id }).containsExactly(1L)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
