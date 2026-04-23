package com.tidelet.app.ui.sos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.testutil.TestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SosNavigationTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sosGrid_showsFourTools() {
        composeRule.setContent {
            SosScreen(onClose = {}, onNavigate = {})
        }

        composeRule.onNodeWithTag(TestTags.SOS_GRID_RIDE_WAVE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.SOS_GRID_BREATHE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.SOS_GRID_REASONS).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.SOS_GRID_DISTRACTIONS).assertIsDisplayed()
    }

    @Test
    fun tappingRideTheWaveTile_emitsNavigateWithWaveRoute() {
        var navigated: String? = null
        composeRule.setContent {
            SosScreen(onClose = {}, onNavigate = { navigated = it })
        }

        composeRule.onNodeWithTag(TestTags.SOS_GRID_RIDE_WAVE).performClick()
        composeRule.waitForIdle()

        // The exact route constant is owned by ui/nav/Routes — we just assert we got one
        // and that it targets the wave sub-screen by substring.
        assertThat(navigated).isNotNull()
        assertThat(navigated).contains("wave")
    }

    @Test
    fun tappingBreatheTile_emitsNavigateWithBreatheRoute() {
        var navigated: String? = null
        composeRule.setContent {
            SosScreen(onClose = {}, onNavigate = { navigated = it })
        }

        composeRule.onNodeWithTag(TestTags.SOS_GRID_BREATHE).performClick()
        composeRule.waitForIdle()

        assertThat(navigated).contains("breathe")
    }
}
