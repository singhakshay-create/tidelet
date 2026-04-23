package com.tidelet.app.ui.sos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.db.SosToolKey
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.MainDispatcherRule
import com.tidelet.app.testutil.TestTideletApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
class ReasonsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    @Test
    fun `reasons flow mirrors repo reasons`() = runTest {
        fakeRepo.seedReasons(listOf(Reason(id = 1, text = "my kid")))
        val vm = ReasonsViewModel(app)

        vm.reasons.test {
            // stateIn seeds an initial empty list; wait for the repo-backed emission.
            var latest = awaitItem()
            while (latest.isEmpty()) latest = awaitItem()
            assertThat(latest.map { it.text }).containsExactly("my kid")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addReason delegates trimmed text to the repository`() = runTest {
        val vm = ReasonsViewModel(app)

        vm.addReason("  I like mornings  ")
        advanceUntilIdle()

        assertThat(fakeRepo.recordedReasonsAdded).containsExactly("I like mornings")
    }

    @Test
    fun `deleteReason delegates the id to the repository`() = runTest {
        fakeRepo.seedReasons(listOf(Reason(id = 42, text = "x")))
        val vm = ReasonsViewModel(app)

        vm.deleteReason(42)
        advanceUntilIdle()

        assertThat(fakeRepo.recordedReasonsDeleted).containsExactly(42L)
    }

    @Test
    fun `logViewed(false) writes no craving event`() = runTest {
        val vm = ReasonsViewModel(app)

        vm.logViewed(hadReasons = false)
        advanceUntilIdle()

        assertThat(fakeRepo.recordedCravings).isEmpty()
    }

    @Test
    fun `logViewed(true) writes REASONS GOT_THROUGH event`() = runTest {
        val vm = ReasonsViewModel(app)

        vm.logViewed(hadReasons = true)
        advanceUntilIdle()

        assertThat(fakeRepo.recordedCravings).containsExactly(
            FakeTideletRepository.LoggedCraving(SosToolKey.REASONS, CravingOutcome.GOT_THROUGH),
        )
    }
}
