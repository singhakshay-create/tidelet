package com.tidelet.app.ui.log

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CheckIn
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.FixedClocks
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class CheckInViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    private val today = LocalDate.of(2026, 4, 18)
    private val clock = FixedClocks.atLocalMidnight(today)

    private fun vmWith(date: String?): CheckInViewModel {
        val handle = SavedStateHandle(mapOf("date" to date))
        return CheckInViewModel(app, handle, clock)
    }

    @Test
    fun `parseDateOrToday falls back to today on missing nav arg`() = runTest {
        val vm = vmWith(date = null)
        advanceUntilIdle()

        assertThat(vm.state.value.date).isEqualTo(today)
    }

    @Test
    fun `parseDateOrToday falls back to today on malformed nav arg`() = runTest {
        val vm = vmWith(date = "not-a-date")
        advanceUntilIdle()

        assertThat(vm.state.value.date).isEqualTo(today)
    }

    @Test
    fun `init loads existing CheckIn into state`() = runTest {
        fakeRepo.seedCheckIns(
            listOf(
                CheckIn(
                    date = today.toString(),
                    didDrink = true,
                    drinkCount = 3,
                    mood = 3,
                    trigger = "stress",
                    note = "long day",
                ),
            )
        )
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()

        val s = vm.state.value
        assertThat(s.loaded).isTrue()
        assertThat(s.didDrink).isTrue()
        assertThat(s.drinkCount).isEqualTo(3)
        assertThat(s.mood).isEqualTo(3)
        assertThat(s.trigger).isEqualTo("stress")
        assertThat(s.note).isEqualTo("long day")
    }

    @Test
    fun `canSave toggles true once didDrink is set`() = runTest {
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()

        assertThat(vm.state.value.canSave).isFalse()
        vm.setDidDrink(false)
        assertThat(vm.state.value.canSave).isTrue()
    }

    @Test
    fun `incrementDrinkCount clamps at 30`() = runTest {
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()

        repeat(35) { vm.incrementDrinkCount() }

        assertThat(vm.state.value.drinkCount).isEqualTo(30)
    }

    @Test
    fun `decrementDrinkCount clamps at 1`() = runTest {
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()

        repeat(5) { vm.decrementDrinkCount() }

        assertThat(vm.state.value.drinkCount).isEqualTo(1)
    }

    @Test
    fun `toggleTrigger sets then clears on same key`() = runTest {
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()

        vm.toggleTrigger("stress")
        assertThat(vm.state.value.trigger).isEqualTo("stress")

        vm.toggleTrigger("stress")
        assertThat(vm.state.value.trigger).isNull()
    }

    @Test
    fun `save writes with trigger null when didDrink is false`() = runTest {
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()
        vm.setDidDrink(false)
        vm.toggleTrigger("stress")  // should be dropped because didDrink=false

        vm.save()
        advanceUntilIdle()

        val upserted = fakeRepo.recordedCheckInUpserts.last()
        assertThat(upserted.didDrink).isFalse()
        assertThat(upserted.trigger).isNull()
        assertThat(upserted.drinkCount).isNull()
    }

    @Test
    fun `save flips saved=true`() = runTest {
        val vm = vmWith(date = today.toString())
        advanceUntilIdle()
        vm.setDidDrink(false)

        vm.save()
        advanceUntilIdle()

        assertThat(vm.state.value.saved).isTrue()
    }
}
