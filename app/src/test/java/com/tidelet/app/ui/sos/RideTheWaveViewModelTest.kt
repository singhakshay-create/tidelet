package com.tidelet.app.ui.sos

import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.FixedClocks
import com.tidelet.app.testutil.MainDispatcherRule
import com.tidelet.app.testutil.TestTideletApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
class RideTheWaveViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    private val fixedClock = FixedClocks.atIso("2026-04-18T12:00:00Z")

    @Test
    fun `timer starts at 900 seconds`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)

        assertThat(vm.state.value.totalSeconds).isEqualTo(15 * 60)
        assertThat(vm.state.value.remainingSeconds).isEqualTo(15 * 60)
        assertThat(vm.state.value.phase).isEqualTo(WavePhase.Running)
    }

    @Test
    fun `timer decrements as virtual time advances`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)
        runCurrent()

        advanceTimeBy(3_000L)
        runCurrent()

        assertThat(vm.state.value.remainingSeconds).isLessThan(15 * 60)
    }

    @Test
    fun `endEarly transitions phase to Done`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)

        vm.endEarly()
        advanceUntilIdle()

        assertThat(vm.state.value.phase).isEqualTo(WavePhase.Done)
        assertThat(vm.state.value.soundscape).isEqualTo(Soundscape.Silent)
    }

    @Test
    fun `logGotThrough writes RIDE_THE_WAVE GOT_THROUGH and opens analysis panel`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)

        vm.logGotThrough()
        advanceUntilIdle()

        assertThat(fakeRepo.recordedCravings.last()).isEqualTo(
            FakeTideletRepository.LoggedCraving(
                SosToolKey.RIDE_THE_WAVE,
                CravingOutcome.GOT_THROUGH,
            ),
        )
        assertThat(vm.state.value.showAnalysisPanel).isTrue()
        assertThat(vm.state.value.lastCravingEventId).isNotNull()
    }

    @Test
    fun `logDrank does NOT open analysis panel`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)

        vm.logDrank()
        advanceUntilIdle()

        assertThat(fakeRepo.recordedCravings.last().outcome).isEqualTo(CravingOutcome.DRANK)
        assertThat(vm.state.value.showAnalysisPanel).isFalse()
    }

    @Test
    fun `saveAnalysis persists functional analysis and closes panel`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)
        vm.logGotThrough()
        advanceUntilIdle()

        vm.saveAnalysis(
            antecedent = "stress",
            thought = "I need this",
            followingAction = "called a friend",
        )
        advanceUntilIdle()

        assertThat(fakeRepo.recordedFunctionalAnalyses).hasSize(1)
        val analysis = fakeRepo.recordedFunctionalAnalyses.first()
        assertThat(analysis.antecedent).isEqualTo("stress")
        assertThat(analysis.thoughtAtMoment).isEqualTo("I need this")
        assertThat(analysis.followingAction).isEqualTo("called a friend")
        assertThat(vm.state.value.showAnalysisPanel).isFalse()
        assertThat(vm.state.value.analysisSaved).isTrue()
    }

    @Test
    fun `dismissAnalysis closes panel without persisting`() = runTest {
        val vm = RideTheWaveViewModel(app, fixedClock)
        vm.logGotThrough()
        advanceUntilIdle()

        vm.dismissAnalysis()
        advanceUntilIdle()

        assertThat(fakeRepo.recordedFunctionalAnalyses).isEmpty()
        assertThat(vm.state.value.showAnalysisPanel).isFalse()
    }
}
