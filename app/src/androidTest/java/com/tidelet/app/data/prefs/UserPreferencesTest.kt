package com.tidelet.app.data.prefs

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Exercises [UserPreferences] against the real DataStore in the test process.
 * Because DataStore is file-backed and process-scoped, each test calls
 * [UserPreferences.clearAll] in `@Before` / `@After` to isolate state.
 */
@RunWith(AndroidJUnit4::class)
class UserPreferencesTest {

    private lateinit var prefs: UserPreferences

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        prefs = UserPreferences(context)
        prefs.clearAll()
    }

    @After
    fun tearDown() = runTest { prefs.clearAll() }

    @Test
    fun profile_emitsDefaultsBeforeAnyWrites() = runTest {
        val p = prefs.profile.first()

        assertThat(p.onboardingComplete).isFalse()
        assertThat(p.startDateEpochDay).isNull()
        assertThat(p.goalMode).isEqualTo("QUIT")
        assertThat(p.checkInReminderEnabled).isFalse()
        assertThat(p.typicalDrinksPerDay).isEqualTo(3)
        assertThat(p.priceCentsPerDrink).isEqualTo(800)
    }

    @Test
    fun completeOnboarding_flipsFlag_andStoresEpochDay() = runTest {
        val startDate = LocalDate.of(2026, 4, 18)

        prefs.completeOnboarding(startDate.toEpochDay())

        val p = prefs.profile.first()
        assertThat(p.onboardingComplete).isTrue()
        assertThat(p.startDateEpochDay).isEqualTo(startDate.toEpochDay())
        assertThat(p.goalMode).isEqualTo("QUIT")
    }

    @Test
    fun setStartDate_updatesOnlyStartDate() = runTest {
        prefs.completeOnboarding(LocalDate.of(2026, 4, 1).toEpochDay())
        prefs.setCheckInReminder(enabled = true, hour = 21, minute = 30)

        prefs.setStartDate(LocalDate.of(2026, 4, 18).toEpochDay())

        val p = prefs.profile.first()
        assertThat(p.startDateEpochDay).isEqualTo(LocalDate.of(2026, 4, 18).toEpochDay())
        assertThat(p.checkInReminderEnabled).isTrue()
        assertThat(p.checkInReminderHour).isEqualTo(21)
        assertThat(p.checkInReminderMinute).isEqualTo(30)
    }

    @Test
    fun clearAll_resetsToDefaults() = runTest {
        prefs.completeOnboarding(LocalDate.of(2026, 4, 1).toEpochDay())
        prefs.setDrinkingBaseline(typicalDrinksPerDay = 5, priceCentsPerDrink = 1_000)

        prefs.clearAll()

        val p = prefs.profile.first()
        assertThat(p.onboardingComplete).isFalse()
        assertThat(p.startDateEpochDay).isNull()
        assertThat(p.typicalDrinksPerDay).isEqualTo(3)
        assertThat(p.priceCentsPerDrink).isEqualTo(800)
        // PHASE_2 §9A4 — banked dry days start at 0 and stay at 0 on wipe.
        assertThat(p.previousStreaksTotalDays).isEqualTo(0L)
    }

    @Test
    fun previousStreaksTotalDays_defaultsToZero() = runTest {
        val p = prefs.profile.first()
        assertThat(p.previousStreaksTotalDays).isEqualTo(0L)
    }

    @Test
    fun addPreviousStreakDays_accumulatesAcrossResets() = runTest {
        // Simulates: user had a 7-day streak, reset. Then a 12-day streak, reset.
        prefs.addPreviousStreakDays(7L)
        prefs.addPreviousStreakDays(12L)

        val p = prefs.profile.first()
        assertThat(p.previousStreaksTotalDays).isEqualTo(19L)
    }

    @Test
    fun addPreviousStreakDays_ignoresZeroAndNegative() = runTest {
        prefs.addPreviousStreakDays(5L)
        prefs.addPreviousStreakDays(0L)    // no-op — same-day "reset" shouldn't bank anything
        prefs.addPreviousStreakDays(-3L)   // defensive — future proof against bad callers

        val p = prefs.profile.first()
        assertThat(p.previousStreaksTotalDays).isEqualTo(5L)
    }

    @Test
    fun setPreviousStreaksTotalDays_overwritesForImport() = runTest {
        prefs.addPreviousStreakDays(10L)
        // Import path — backup says lifetime total was 42; replace in place.
        prefs.setPreviousStreaksTotalDays(42L)

        val p = prefs.profile.first()
        assertThat(p.previousStreaksTotalDays).isEqualTo(42L)
    }

    // ---- PHASE_2 §9B1 — visual companion persistence ----

    @Test
    fun companionHighestStage_defaultsToOneAndPersistsRaises() = runTest {
        // Brand-new prefs — default 1.
        assertThat(prefs.profile.first().companionHighestStage).isEqualTo(1)

        prefs.raiseCompanionHighestStage(3)
        assertThat(prefs.profile.first().companionHighestStage).isEqualTo(3)
    }

    @Test
    fun companionHighestStage_neverLowersEvenWhenAskedTo() = runTest {
        prefs.raiseCompanionHighestStage(4)
        prefs.raiseCompanionHighestStage(2)   // attempt to lower — ignored
        prefs.raiseCompanionHighestStage(1)

        assertThat(prefs.profile.first().companionHighestStage).isEqualTo(4)
    }

    @Test
    fun companionHighestStage_clampsTo1Through5() = runTest {
        prefs.raiseCompanionHighestStage(99)
        assertThat(prefs.profile.first().companionHighestStage).isEqualTo(5)
    }

    @Test
    fun companionEnabled_defaultsOffAndToggles() = runTest {
        assertThat(prefs.profile.first().companionEnabled).isFalse()
        prefs.setCompanionEnabled(true)
        assertThat(prefs.profile.first().companionEnabled).isTrue()
    }
}
