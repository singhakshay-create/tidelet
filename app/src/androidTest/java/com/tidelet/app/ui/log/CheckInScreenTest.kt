package com.tidelet.app.ui.log

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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

@RunWith(AndroidJUnit4::class)
class CheckInScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun save_disabledUntilDidDrinkAnswered() {
        composeRule.setContent { CheckInScreen(onDone = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.CHECKIN_SAVE).assertIsNotEnabled()
    }

    @Test
    fun pickingYes_revealsDrinkCountAndTriggerChips() {
        composeRule.setContent { CheckInScreen(onDone = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.CHECKIN_DID_DRINK_YES).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.CHECKIN_DRINK_COUNT).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.CHECKIN_TRIGGER_CHIPS).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.CHECKIN_SAVE).assertIsEnabled()
    }

    @Test
    fun pickingNo_hidesTriggerChips_andEnablesSave() {
        composeRule.setContent { CheckInScreen(onDone = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.CHECKIN_DID_DRINK_NO).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.CHECKIN_TRIGGER_CHIPS).assertDoesNotExist()
        composeRule.onNodeWithTag(TestTags.CHECKIN_SAVE).assertIsEnabled()
    }

    @Test
    fun save_dispatchesUpsertToRepo_andCallsOnDone() {
        var doneCalled = false
        composeRule.setContent { CheckInScreen(onDone = { doneCalled = true }) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.CHECKIN_DID_DRINK_NO).performClick()
        composeRule.onNodeWithTag(TestTags.CHECKIN_SAVE).performClick()
        composeRule.waitForIdle()

        assertThat(fakeRepo.recordedCheckInUpserts).hasSize(1)
        val row = fakeRepo.recordedCheckInUpserts.first()
        assertThat(row.didDrink).isFalse()
        assertThat(row.trigger).isNull()
        assertThat(doneCalled).isTrue()
    }
}
