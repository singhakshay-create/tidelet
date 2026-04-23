package com.tidelet.app.ui.sos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.TestTags
import com.tidelet.app.testutil.resolveTestApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RideTheWaveScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun timerText_startsAt1500() {
        composeRule.setContent { RideTheWaveScreen(onDone = {}) }

        composeRule.onNodeWithTag(TestTags.WAVE_TIMER_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("15:00").assertIsDisplayed()
    }

    @Test
    fun endEarlyButton_transitionsToOutcomeButtons() {
        composeRule.setContent { RideTheWaveScreen(onDone = {}) }

        composeRule.onNodeWithTag(TestTags.WAVE_END_EARLY).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.WAVE_GOT_THROUGH).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.WAVE_DRANK).assertIsDisplayed()
    }

    @Test
    fun tappingGotThrough_showsAnalysisPanel_andLogsEvent() {
        composeRule.setContent { RideTheWaveScreen(onDone = {}) }

        composeRule.onNodeWithTag(TestTags.WAVE_END_EARLY).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.WAVE_GOT_THROUGH).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.WAVE_ANALYSIS_PANEL).assertIsDisplayed()
        assertThat(fakeRepo.recordedCravings.last()).isEqualTo(
            FakeTideletRepository.LoggedCraving(
                SosToolKey.RIDE_THE_WAVE,
                CravingOutcome.GOT_THROUGH,
            ),
        )
    }

    @Test
    fun tappingDrank_logsEvent_andDoesNotShowAnalysisPanel() {
        var drankInvoked = false
        composeRule.setContent {
            RideTheWaveScreen(onDone = {}, onDrankFlow = { drankInvoked = true })
        }

        composeRule.onNodeWithTag(TestTags.WAVE_END_EARLY).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.WAVE_DRANK).performClick()
        composeRule.waitForIdle()

        assertThat(fakeRepo.recordedCravings.last().outcome).isEqualTo(CravingOutcome.DRANK)
        assertThat(drankInvoked).isTrue()
    }
}
