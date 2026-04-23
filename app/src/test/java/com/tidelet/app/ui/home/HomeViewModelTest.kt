package com.tidelet.app.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.db.WeeklyReflection
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.FixedClocks
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class HomeViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val zone: ZoneId = FixedClocks.UTC

    private val app: TestTideletApplication
        get() = RuntimeEnvironment.getApplication() as TestTideletApplication

    private val fakeRepo: FakeTideletRepository
        get() = app.repository as FakeTideletRepository

    @Test
    fun `state emits zero streak when profile has no startDate`() = runTest {
        // Arrange — fresh app + fake, no start date seeded.
        val today = LocalDate.of(2026, 4, 18)  // Saturday (not Sunday, keep card false)
        val vm = HomeViewModel(app, clock = FixedClocks.atLocalMidnight(today, zone))

        // Act / Assert
        vm.state.test {
            val emitted = awaitItem()
            assertThat(emitted.startDate).isNull()
            assertThat(emitted.streak.days).isEqualTo(0L)
            assertThat(emitted.nextMilestone).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state emits days derived from profile startDate`() = runTest {
        // Arrange — start date was yesterday in UTC.
        val today = LocalDate.of(2026, 4, 18)
        val startDate = today.minusDays(5)
        fakeRepo.setStartDateEpochDay(startDate.toEpochDay())
        val vm = HomeViewModel(app, clock = FixedClocks.atLocalMidnight(today, zone))

        vm.state.test {
            // Skip initial seed emission (profile flow is cold-ish via stateIn; we want
            // the derived value after profile arrives).
            val first = awaitItem()
            val derived = if (first.startDate == null) awaitItem() else first
            assertThat(derived.startDate).isEqualTo(startDate)
            assertThat(derived.streak.days).isEqualTo(5L)
            assertThat(derived.nextMilestone?.days).isEqualTo(7)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weekly reflection card shown on Sunday when unreflected`() = runTest {
        val sunday = LocalDate.of(2026, 4, 19)
        assertThat(sunday.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        val vm = HomeViewModel(app, clock = FixedClocks.atLocalMidnight(sunday, zone))

        vm.state.test {
            val s = awaitItem()
            assertThat(s.showWeeklyReflectionCard).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `weekly reflection card hidden on Sunday when already reflected`() = runTest {
        val sunday = LocalDate.of(2026, 4, 19)
        val thisWeeksMonday = LocalDate.of(2026, 4, 13)
        fakeRepo.seedWeeklyReflections(
            listOf(WeeklyReflection(weekStartIso = thisWeeksMonday.toString(), rating = 4))
        )
        val vm = HomeViewModel(app, clock = FixedClocks.atLocalMidnight(sunday, zone))

        vm.state.test {
            // Drain until we see a state that reflects the seeded reflection list.
            var s = awaitItem()
            while (s.showWeeklyReflectionCard) {
                s = awaitItem()
            }
            assertThat(s.showWeeklyReflectionCard).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onThisDay surfaces a matching past thought record`() = runTest {
        val today = LocalDate.of(2026, 4, 19)
        // Same day-of-month, last year.
        val lastYear = LocalDate.of(2025, 4, 19)
        fakeRepo.seedThoughtRecords(
            listOf(
                ThoughtRecord(
                    id = 42,
                    dateIso = lastYear.toString(),
                    situation = "at a wedding",
                    thought = "just one won't hurt",
                    challenge = "",
                    friendReframe = "",
                )
            )
        )
        val vm = HomeViewModel(app, clock = FixedClocks.atLocalMidnight(today, zone))

        vm.state.test {
            var s = awaitItem()
            while (s.onThisDay == null) s = awaitItem()

            val item = s.onThisDay
            assertThat(item).isInstanceOf(OnThisDayItem.Thought::class.java)
            assertThat((item as OnThisDayItem.Thought).record.id).isEqualTo(42L)
            assertThat(item.monthsAgo).isEqualTo(12)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissOnThisDay clears the card for the session`() = runTest {
        val today = LocalDate.of(2026, 4, 19)
        fakeRepo.seedJournalEntries(
            listOf(JournalEntry(id = 1, dateIso = "2025-04-19", text = "hello from a year ago"))
        )
        val vm = HomeViewModel(app, clock = FixedClocks.atLocalMidnight(today, zone))

        vm.state.test {
            var s = awaitItem()
            while (s.onThisDay == null) s = awaitItem()
            assertThat(s.onThisDay).isNotNull()

            vm.dismissOnThisDay()

            var next = awaitItem()
            while (next.onThisDay != null) next = awaitItem()
            assertThat(next.onThisDay).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
