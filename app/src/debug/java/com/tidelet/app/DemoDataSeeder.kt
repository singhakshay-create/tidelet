package com.tidelet.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.tidelet.app.data.db.CravingEvent
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * Seeds a synthetic 23-day sobriety streak plus a handful of craving/urge
 * log entries, so a debug build shows a populated app instead of an empty
 * first run — handy for screenshots and reviewers.
 *
 * This file lives under `app/src/debug/` — a Gradle "debug source set".
 * Gradle compiles `src/main` together with whichever source set matches the
 * variant being built (`debug` or `release`), so this class only exists at
 * all when building a debug variant; `assembleRelease` never sees this file
 * or the demo data it contains. `app/src/release/.../DemoDataSeeder.kt` is a
 * no-op twin with the same function signature, so the one call site in
 * `TideletApplication.onCreate()` (in `src/main`, compiled into every
 * variant) has something to resolve against either way.
 */
object DemoDataSeeder {

    // A separate, tiny DataStore just to remember "we already tried seeding
    // once on this install." Kept apart from the real UserPreferences so
    // this debug-only file never has to touch the production prefs schema.
    private val Context.demoSeedStore by preferencesDataStore(name = "tidelet_demo_seed_state")
    private val ALREADY_SEEDED = booleanPreferencesKey("already_seeded")

    /**
     * Fire-and-forget. Launches its own coroutine so the call site in
     * `onCreate()` stays a single non-suspending line, matching how
     * `TideletApplication` already kicks off `StreakWidgetRefreshWorker`.
     */
    fun seedIfNeeded(
        context: Context,
        repository: TideletRepository,
        clock: Clock = Clock.systemDefaultZone(),
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val store = context.demoSeedStore
            val alreadyAttempted = store.data.first()[ALREADY_SEEDED] ?: false
            if (alreadyAttempted) return@launch

            // Mark it attempted right away, before doing any seeding work.
            // "One-time" means we only ever try once per install — whether
            // or not we actually end up seeding below — so a reinstall in
            // debug (fresh DataStore) can seed again, but relaunching the
            // same install never appends duplicate demo entries.
            store.edit { it[ALREADY_SEEDED] = true }

            // Never seed on top of real data: bail if onboarding has already
            // happened or any craving events already exist.
            val profile = repository.profile.first()
            val hasRealData = profile.onboardingComplete || repository.cravingEvents.first().isNotEmpty()
            if (hasRealData) return@launch

            seedDemoData(repository, clock)
        }
    }

    private data class DemoCraving(
        val dayOffset: Long,
        val tool: String,
        val outcome: String,
        val intensity: Int,
    )

    private suspend fun seedDemoData(repository: TideletRepository, clock: Clock) {
        // Use the app's real clock (same convention as HomeViewModel etc.)
        // rather than LocalDate.now(), so this behaves consistently with
        // the rest of the streak math and stays testable.
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val startDate = today.minusDays(23)

        // Sets the sobriety start date AND flips onboardingComplete = true,
        // so the app opens straight to Home instead of the onboarding flow.
        repository.completeOnboarding(startDate)

        // A handful of plausible craving/urge log entries spread across the
        // 23-day streak, with varied tools, outcomes, and intensities.
        val demoCravings = listOf(
            DemoCraving(dayOffset = 2, tool = SosToolKey.RIDE_THE_WAVE, outcome = CravingOutcome.GOT_THROUGH, intensity = 6),
            DemoCraving(dayOffset = 6, tool = SosToolKey.BREATHE, outcome = CravingOutcome.GOT_THROUGH, intensity = 4),
            DemoCraving(dayOffset = 10, tool = SosToolKey.REASONS, outcome = CravingOutcome.STILL_STRUGGLING, intensity = 8),
            DemoCraving(dayOffset = 15, tool = SosToolKey.DISTRACTIONS, outcome = CravingOutcome.GOT_THROUGH, intensity = 5),
            DemoCraving(dayOffset = 19, tool = SosToolKey.JOURNAL, outcome = CravingOutcome.GOT_THROUGH, intensity = 3),
        )
        demoCravings.forEach { demo ->
            val atMillis = startDate.plusDays(demo.dayOffset)
                .atTime(19, 30)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            repository.importCravingEvent(
                CravingEvent(
                    timestampEpochMillis = atMillis,
                    tool = demo.tool,
                    outcome = demo.outcome,
                    intensity = demo.intensity,
                ),
            )
        }
    }
}
