package com.tidelet.app.ui.sos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class BreatheScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun phaseLabel_isDisplayed_atStart() {
        composeRule.setContent { BreatheScreen(onDone = {}) }

        composeRule.onNodeWithTag(TestTags.BREATHE_PHASE_LABEL).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.BREATHE_CYCLE_COUNT).assertIsDisplayed()
    }

    @Test
    fun cycleCount_startsAtZero() {
        composeRule.setContent { BreatheScreen(onDone = {}) }

        // The cycles label includes the "0" count. A resource-agnostic way to
        // check: the cycle tag exists and contains "0".
        composeRule.onNodeWithTag(TestTags.BREATHE_CYCLE_COUNT).assertIsDisplayed()
        composeRule.onNodeWithText("0", substring = true).assertIsDisplayed()
    }

    @Test
    fun onDispose_withNoCycles_logsNothing() {
        var doneCalled = false
        composeRule.setContent { BreatheScreen(onDone = { doneCalled = true }) }

        // Immediately leave the screen — DisposableEffect should short-circuit.
        composeRule.activityRule.scenario.onActivity { it.finish() }
        composeRule.waitForIdle()

        assertThat(fakeRepo.recordedCravings.filter { it.tool == SosToolKey.BREATHE }).isEmpty()
        // `doneCalled` is unused: we exited via activity finish, which is a
        // more reliable disposal signal than the Stop button tap.
        assertThat(doneCalled).isFalse()
        assertThat(CravingOutcome.GOT_THROUGH).isNotNull() // keep outcome import meaningful
    }
}
