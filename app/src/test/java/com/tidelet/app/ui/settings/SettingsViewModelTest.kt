package com.tidelet.app.ui.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.prefs.UserProfile
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

/**
 * Unit tests for [SettingsViewModel].
 *
 * Since Phase 2 §3, `state` is backed by a `combine(_actions, repo.profile)`
 * that's started via [kotlinx.coroutines.flow.SharingStarted.WhileSubscribed]
 * — it produces only when something subscribes. So these tests use Turbine
 * (`.state.test { ... }`) instead of `state.value`; raw `.value` would read
 * the initial-value placeholder before the upstream has run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class SettingsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    @Test
    fun `wipeAllData calls repo wipeAll and lands on Wiped lastAction`() = runTest {
        fakeRepo.seedReasons(listOf(Reason(id = 1L, text = "because")))
        val vm = SettingsViewModel(app)

        vm.state.test {
            // Drain the initial emission — profile hasn't yet propagated through.
            awaitItem()

            vm.wipeAllData()
            advanceUntilIdle()

            // Walk the emissions until we see the final Wiped state.
            var s = awaitItem()
            while (s.lastAction != LastAction.Wiped) s = awaitItem()

            assertThat(fakeRepo.wipeCount).isEqualTo(1)
            assertThat(s.busy).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissMessage clears message and resets lastAction to Idle`() = runTest {
        val vm = SettingsViewModel(app)

        vm.state.test {
            awaitItem() // initial

            vm.dismissMessage()
            advanceUntilIdle()

            var s = awaitItem()
            while (s.lastAction != LastAction.Idle) s = awaitItem()

            assertThat(s.message).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateBaseline clamps drinks-per-day to the 1–30 range`() = runTest {
        val vm = SettingsViewModel(app)

        vm.updateBaseline(drinksPerDay = 999, priceCents = 500, hoursPerDrink = 1)
        advanceUntilIdle()

        val snapshot = fakeRepo.profile.snapshot()
        assertThat(snapshot.typicalDrinksPerDay).isEqualTo(30)
        assertThat(snapshot.priceCentsPerDrink).isEqualTo(500)
    }

    @Test
    fun `updateBaseline clamps negative price to zero`() = runTest {
        val vm = SettingsViewModel(app)

        vm.updateBaseline(drinksPerDay = 4, priceCents = -100, hoursPerDrink = 1)
        advanceUntilIdle()

        val snapshot = fakeRepo.profile.snapshot()
        assertThat(snapshot.priceCentsPerDrink).isEqualTo(0)
        assertThat(snapshot.typicalDrinksPerDay).isEqualTo(4)
    }

    @Test
    fun `updateBaseline clamps hoursPerDrink to 0–24 range`() = runTest {
        val vm = SettingsViewModel(app)

        vm.updateBaseline(drinksPerDay = 3, priceCents = 800, hoursPerDrink = 99)
        advanceUntilIdle()

        assertThat(fakeRepo.profile.snapshot().hoursPerDrink).isEqualTo(24)

        vm.updateBaseline(drinksPerDay = 3, priceCents = 800, hoursPerDrink = -5)
        advanceUntilIdle()

        assertThat(fakeRepo.profile.snapshot().hoursPerDrink).isEqualTo(0)
    }

    /**
     * Tiny helper: blocking snapshot of the fake's profile flow. FakeRepo
     * exposes profile as a StateFlow so the current value is immediately
     * available.
     */
    private fun kotlinx.coroutines.flow.Flow<UserProfile>.snapshot(): UserProfile =
        (this as kotlinx.coroutines.flow.StateFlow<UserProfile>).value
}
