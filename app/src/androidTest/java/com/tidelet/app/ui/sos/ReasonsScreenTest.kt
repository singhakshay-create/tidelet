package com.tidelet.app.ui.sos

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.Reason
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.TestTags
import com.tidelet.app.testutil.resolveTestApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReasonsScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun emptyState_showsAddFirstReasonCta() {
        composeRule.setContent { ReasonsScreen(onBack = {}) }

        composeRule.onNodeWithTag(TestTags.REASONS_EMPTY_CTA).assertIsDisplayed()
    }

    @Test
    fun seededReason_renderAsChip_withDeleteAction() {
        fakeRepo.seedReasons(listOf(Reason(id = 10L, text = "my kids")))

        composeRule.setContent { ReasonsScreen(onBack = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("${TestTags.REASONS_CHIP_PREFIX}10").assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.REASONS_EMPTY_CTA).assertDoesNotExist()
    }

    @Test
    fun typingAndSubmitting_addsChip_viaRepository() {
        composeRule.setContent { ReasonsScreen(onBack = {}) }

        composeRule.onNodeWithTag(TestTags.REASONS_INPUT).performTextInput("I like clear mornings")
        composeRule.onNodeWithTag(TestTags.REASONS_SUBMIT).performClick()
        composeRule.waitForIdle()

        assertThat(fakeRepo.recordedReasonsAdded).containsExactly("I like clear mornings")
    }

    @Test
    fun deleteAction_removesChip_andRecordsDeletion() {
        fakeRepo.seedReasons(listOf(Reason(id = 42L, text = "focus")))
        composeRule.setContent { ReasonsScreen(onBack = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("${TestTags.REASONS_DELETE_PREFIX}42").performClick()
        composeRule.waitForIdle()

        assertThat(fakeRepo.recordedReasonsDeleted).containsExactly(42L)
    }
}
