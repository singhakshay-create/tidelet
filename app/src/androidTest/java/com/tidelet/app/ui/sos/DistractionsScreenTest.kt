package com.tidelet.app.ui.sos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class DistractionsScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun shuffle_button_isDisplayed() {
        composeRule.setContent { DistractionsScreen(onDone = {}) }

        composeRule.onNodeWithTag(TestTags.DISTRACTIONS_SHUFFLE).assertIsDisplayed()
    }

    @Test
    fun didIt_button_recordsDistractionsGotThrough_andCallsOnDone() {
        var doneCalled = false
        composeRule.setContent { DistractionsScreen(onDone = { doneCalled = true }) }

        composeRule.onNodeWithTag(TestTags.DISTRACTIONS_DID_IT).performClick()
        composeRule.waitForIdle()

        assertThat(doneCalled).isTrue()
        assertThat(fakeRepo.recordedCravings).containsExactly(
            FakeTideletRepository.LoggedCraving(
                SosToolKey.DISTRACTIONS,
                CravingOutcome.GOT_THROUGH,
            ),
        )
    }

    @Test
    fun nothingWorked_recordsStillStruggling_andCallsOnDone() {
        var doneCalled = false
        composeRule.setContent { DistractionsScreen(onDone = { doneCalled = true }) }

        composeRule.onNodeWithTag(TestTags.DISTRACTIONS_NOTHING_WORKED).performClick()
        composeRule.waitForIdle()

        assertThat(doneCalled).isTrue()
        assertThat(fakeRepo.recordedCravings).containsExactly(
            FakeTideletRepository.LoggedCraving(
                SosToolKey.DISTRACTIONS,
                CravingOutcome.STILL_STRUGGLING,
            ),
        )
    }
}
