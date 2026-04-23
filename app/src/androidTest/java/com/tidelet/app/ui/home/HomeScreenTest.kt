package com.tidelet.app.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tidelet.app.data.db.WeeklyReflection
import com.tidelet.app.fakes.FakeTideletRepository
import com.tidelet.app.testutil.TestTags
import com.tidelet.app.testutil.resolveTestApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeRepo: FakeTideletRepository

    @Before
    fun setUp() {
        fakeRepo = resolveTestApp().second
    }

    @Test
    fun rendersZeroStateWhenProfileHasNoStartDate() {
        composeRule.setContent { HomeScreen(onOpenSos = {}) }

        composeRule.onNodeWithTag(TestTags.HOME_STREAK_DAYS).assertIsDisplayed()
        // Zero days is the initial streak when no startDate is set.
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun rendersDayCountWhenStartDateIsYesterday() {
        val yesterday = LocalDate.now().minusDays(1)
        fakeRepo.setStartDateEpochDay(yesterday.toEpochDay())

        composeRule.setContent { HomeScreen(onOpenSos = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.HOME_STREAK_DAYS).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.HOME_NEXT_MILESTONE).assertIsDisplayed()
    }

    @Test
    fun weeklyReflectionCardHiddenWhenAlreadyReflected() {
        // Seed a reflection for the current week's Monday so the card is suppressed
        // regardless of which day the test runs.
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fakeRepo.seedWeeklyReflections(
            listOf(WeeklyReflection(weekStartIso = monday.toString(), rating = 4)),
        )

        composeRule.setContent { HomeScreen(onOpenSos = {}) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TestTags.HOME_WEEKLY_REFLECTION_CARD)
            .assertDoesNotExist()
    }
}
