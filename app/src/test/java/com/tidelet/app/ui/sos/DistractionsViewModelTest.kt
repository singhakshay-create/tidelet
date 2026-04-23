package com.tidelet.app.ui.sos

import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CravingOutcome
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
class DistractionsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    @Test
    fun `logDidIt writes DISTRACTIONS GOT_THROUGH`() = runTest {
        val vm = DistractionsViewModel(app)

        vm.logDidIt()
        advanceUntilIdle()

        assertThat(fakeRepo.recordedCravings).containsExactly(
            FakeTideletRepository.LoggedCraving(SosToolKey.DISTRACTIONS, CravingOutcome.GOT_THROUGH),
        )
    }

    @Test
    fun `logNothingWorked writes DISTRACTIONS STILL_STRUGGLING`() = runTest {
        val vm = DistractionsViewModel(app)

        vm.logNothingWorked()
        advanceUntilIdle()

        assertThat(fakeRepo.recordedCravings).containsExactly(
            FakeTideletRepository.LoggedCraving(
                SosToolKey.DISTRACTIONS,
                CravingOutcome.STILL_STRUGGLING,
            ),
        )
    }
}
