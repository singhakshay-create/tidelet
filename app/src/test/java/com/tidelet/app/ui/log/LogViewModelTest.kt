package com.tidelet.app.ui.log

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CheckIn
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class LogViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    @Test
    fun `state splits today from history`() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        fakeRepo.seedCheckIns(
            listOf(
                CheckIn(date = today.toString(), didDrink = false),
                CheckIn(date = yesterday.toString(), didDrink = true, drinkCount = 2),
            )
        )
        val vm = LogViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (s.today == null && s.history.isEmpty()) s = awaitItem()
            assertThat(s.today?.date).isEqualTo(today)
            assertThat(s.history).hasSize(1)
            assertThat(s.history.first().date).isEqualTo(yesterday)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state drops rows with malformed date strings`() = runTest {
        val today = LocalDate.now()
        fakeRepo.seedCheckIns(
            listOf(
                CheckIn(date = today.toString(), didDrink = false),
                CheckIn(date = "garbage", didDrink = true),
            )
        )
        val vm = LogViewModel(app)

        vm.state.test {
            var s = awaitItem()
            while (s.today == null) s = awaitItem()
            // The garbage row should be silently filtered.
            assertThat(s.history).isEmpty()
            assertThat(s.today?.date).isEqualTo(today)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
