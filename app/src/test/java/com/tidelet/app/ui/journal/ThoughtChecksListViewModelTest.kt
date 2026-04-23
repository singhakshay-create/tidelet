package com.tidelet.app.ui.journal

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.ThoughtRecord
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

/**
 * Unit tests for the read-side Thought Checks list VM.
 *
 * Focuses on the grouping + ordering invariants that a manual-QA glance
 * wouldn't catch: most-frequent group first, untagged last, within-group
 * order preserved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class ThoughtChecksListViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app: TestTideletApplication
        get() = RuntimeEnvironment.getApplication() as TestTideletApplication

    private val fakeRepo: FakeTideletRepository
        get() = app.repository as FakeTideletRepository

    private fun record(
        id: Long,
        thought: String,
        tag: String?,
        day: Int = 1,
        createdAt: Long = id * 1000L,
    ) = ThoughtRecord(
        id = id,
        dateIso = "2025-04-%02d".format(day),
        situation = "",
        thought = thought,
        challenge = "",
        friendReframe = "",
        distortionTag = tag,
        createdAtEpochMillis = createdAt,
    )

    @Test
    fun `empty repo produces empty, loaded state`() = runTest {
        val vm = ThoughtChecksListViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (!s.loaded) s = awaitItem()
            assertThat(s.isEmpty).isTrue()
            assertThat(s.total).isEqualTo(0)
            assertThat(s.groups).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `groups records by distortion tag, ordered by count desc, untagged last`() = runTest {
        fakeRepo.seedThoughtRecords(
            listOf(
                // 3 all-or-nothing
                record(1, "A", "all_or_nothing"),
                record(2, "B", "all_or_nothing"),
                record(3, "C", "all_or_nothing"),
                // 2 permission-giving
                record(4, "D", "permission_giving"),
                record(5, "E", "permission_giving"),
                // 1 untagged
                record(6, "F", null),
            )
        )
        val vm = ThoughtChecksListViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (!s.loaded || s.total == 0) s = awaitItem()

            assertThat(s.total).isEqualTo(6)
            assertThat(s.groups).hasSize(3)

            // First group: the most common tag.
            assertThat(s.groups[0].tagKey).isEqualTo("all_or_nothing")
            assertThat(s.groups[0].count).isEqualTo(3)

            assertThat(s.groups[1].tagKey).isEqualTo("permission_giving")
            assertThat(s.groups[1].count).isEqualTo(2)

            // Untagged always sits last regardless of count parity.
            assertThat(s.groups[2].tagKey).isNull()
            assertThat(s.groups[2].count).isEqualTo(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank tag strings collapse into the untagged group`() = runTest {
        fakeRepo.seedThoughtRecords(
            listOf(
                record(1, "A", "  "), // whitespace tag
                record(2, "B", null),
                record(3, "C", ""),
            )
        )
        val vm = ThoughtChecksListViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (!s.loaded || s.total == 0) s = awaitItem()
            assertThat(s.groups).hasSize(1)
            assertThat(s.groups[0].tagKey).isNull()
            assertThat(s.groups[0].count).isEqualTo(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ties broken alphabetically by tag key for deterministic order`() = runTest {
        fakeRepo.seedThoughtRecords(
            listOf(
                record(1, "A", "permission_giving"),
                record(2, "B", "all_or_nothing"),
            )
        )
        val vm = ThoughtChecksListViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (!s.loaded || s.total == 0) s = awaitItem()
            // Both have count == 1, so alphabetical tiebreak: "all_..." before "permission_..."
            assertThat(s.groups.map { it.tagKey }).containsExactly(
                "all_or_nothing", "permission_giving",
            ).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
