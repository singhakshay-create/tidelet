package com.tidelet.app.ui.onboarding

import com.google.common.truth.Truth.assertThat
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class OnboardingViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val app get() = RuntimeEnvironment.getApplication() as TestTideletApplication
    private val fakeRepo get() = app.repository as FakeTideletRepository

    @Test
    fun `finishOnboarding delegates the chosen start date to the repository`() = runTest {
        val vm = OnboardingViewModel(app)
        val startDate = LocalDate.of(2026, 4, 18)

        vm.finishOnboarding(startDate)
        advanceUntilIdle()

        assertThat(fakeRepo.recordedOnboarding).containsExactly(startDate)
    }
}
