package com.tidelet.app.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.TestTags
import com.tidelet.app.testutil.resolveTestApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun welcomeFrame_showsContinueButton() {
        composeRule.setContent { OnboardingFlow() }

        composeRule.onNodeWithTag(TestTags.ONBOARDING_WELCOME_CONTINUE).assertIsDisplayed()
    }

    @Test
    fun continueButton_advancesToStartDateStep() {
        composeRule.setContent { OnboardingFlow() }

        composeRule.onNodeWithTag(TestTags.ONBOARDING_WELCOME_CONTINUE).performClick()

        composeRule.onNodeWithTag(TestTags.ONBOARDING_START_DATE_TODAY).assertIsDisplayed()
    }

    @Test
    fun pickingToday_thenFinish_logsOnboardingWithTodayDate() {
        composeRule.setContent { OnboardingFlow() }

        composeRule.onNodeWithTag(TestTags.ONBOARDING_WELCOME_CONTINUE).performClick()
        composeRule.onNodeWithTag(TestTags.ONBOARDING_START_DATE_TODAY).performClick()
        composeRule.onNodeWithTag(TestTags.ONBOARDING_FINISH).performClick()
        composeRule.waitForIdle()

        assertThat(fakeRepo.recordedOnboarding).hasSize(1)
        assertThat(fakeRepo.recordedOnboarding.first()).isEqualTo(LocalDate.now())
    }
}
