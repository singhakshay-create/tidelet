package com.tidelet.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences replaces the old SharedPreferences API with:
 *   - type safety (Keys are typed)
 *   - async reads via Flow (no disk I/O on the main thread)
 *
 * We use it for simple scalar state: the user profile and onboarding flag.
 * Complex structured data (logs, events) goes in Room instead.
 */

// Extension property on Context — idiomatic way to create a singleton DataStore.
private val Context.dataStore by preferencesDataStore(name = "tidelet_user_prefs")

private object Keys {
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    val START_DATE_EPOCH_DAY = longPreferencesKey("start_date_epoch_day")
    val GOAL_MODE = stringPreferencesKey("goal_mode")                  // "QUIT" (v1 only)
    val CHECKIN_REMINDER_HOUR = intPreferencesKey("checkin_reminder_hour")
    val CHECKIN_REMINDER_MINUTE = intPreferencesKey("checkin_reminder_minute")
    val CHECKIN_REMINDER_ENABLED = booleanPreferencesKey("checkin_reminder_enabled")

    // Used by the Stats "money saved" estimate. Both are editable in Settings.
    val TYPICAL_DRINKS_PER_DAY = intPreferencesKey("typical_drinks_per_day")
    val PRICE_CENTS_PER_DRINK = intPreferencesKey("price_cents_per_drink")

    // PHASE_2 §9A2 — hours-reclaimed estimate. Hours (whole integer) the
    // user typically burned on each drink plus post-drink recovery; the
    // Stats card multiplies it by drinks/day × streak days. Default 1 —
    // hedged estimate that doesn't inflate the payoff.
    val HOURS_PER_DRINK = intPreferencesKey("hours_per_drink")

    // PHASE_2 §9A5 — evening mini-review opt-in. Default false. When true,
    // Home shows an "Evening review" entry button after sundown until the
    // user logs (or the day rolls over).
    val EVENING_REVIEW_ENABLED = booleanPreferencesKey("evening_review_enabled")

    // PHASE_2 §9B1 — compassionate visual companion. Opt-in; default off.
    // The companion has a monotonic "high-water mark" — once a stage is
    // reached, it never visibly regresses even if the streak resets.
    val COMPANION_ENABLED = booleanPreferencesKey("companion_enabled")
    val COMPANION_HIGHEST_STAGE = intPreferencesKey("companion_highest_stage")

    // PHASE_2 §9A4 — "streak grace language". Sum of dry days accumulated
    // across every previous streak that the user has since reset away from.
    // Combined with the current streak at read-time to produce lifetime dry
    // days, so the user sees cumulative progress even after a reset.
    val PREVIOUS_STREAKS_TOTAL_DAYS = longPreferencesKey("previous_streaks_total_days")

    // Tool-open counters (PHASE_2 §6) — how many times the user has opened
    // each SOS / CBT tool. Local-only; surfaced on Stats, included in export.
    // Stored per-tool via dynamically named keys so we don't have to recompile
    // when a new tool ships.
    fun toolCountKey(toolKey: String) = intPreferencesKey("tool_count_$toolKey")
}

data class UserProfile(
    val onboardingComplete: Boolean,
    /** java.time.LocalDate.toEpochDay(). Null if onboarding hasn't completed. */
    val startDateEpochDay: Long?,
    val goalMode: String,
    val checkInReminderHour: Int,
    val checkInReminderMinute: Int,
    val checkInReminderEnabled: Boolean,
    /**
     * User's self-reported "typical drinks per day" pre-quit — the basis for
     * the money-saved estimate in Stats. Defaults to 3, a reasonable midpoint
     * between a social drinker and heavy drinker that avoids inflating the
     * number unrealistically.
     */
    val typicalDrinksPerDay: Int,
    /**
     * Price of a typical drink in cents (so we stay in integer math). Defaults
     * to 800 ($8) — bar/restaurant midpoint in the US. Again, configurable.
     */
    val priceCentsPerDrink: Int,
    /**
     * Banked dry days from previous streaks the user has since reset away
     * from. Zero for a first-time user; grows monotonically on every reset.
     * Combine with [StreakDuration.days] on the current streak to get
     * lifetime dry days. See PHASE_2 §9A4.
     */
    val previousStreaksTotalDays: Long = 0L,
    /**
     * Whole hours the user estimates they burn per drink (drinking + recovery).
     * Feeds the "hours reclaimed" Stats card (PHASE_2 §9A2). Default 1 — an
     * intentionally modest estimate so the payoff reads as conservative.
     */
    val hoursPerDrink: Int = 1,
    /**
     * Whether Home should surface the optional evening mini-review entry
     * button (PHASE_2 §9A5). Off by default — opt-in via Settings.
     */
    val eveningReviewEnabled: Boolean = false,
    /**
     * Whether the compassionate visual companion (PHASE_2 §9B1) is shown at
     * the top of Home. Off by default — opt-in via Settings.
     */
    val companionEnabled: Boolean = false,
    /**
     * Monotonic high-water mark for the companion's stage (PHASE_2 §9B1).
     * Stored 1..5. The rendered stage is `max(computedStage, highestEver)`
     * so the visual never visibly steps backward after a reset.
     */
    val companionHighestStage: Int = 1,
)

class UserPreferences(private val context: Context) {

    val profile: Flow<UserProfile> = context.dataStore.data.map { prefs: Preferences ->
        UserProfile(
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            startDateEpochDay = prefs[Keys.START_DATE_EPOCH_DAY],
            goalMode = prefs[Keys.GOAL_MODE] ?: "QUIT",
            checkInReminderHour = prefs[Keys.CHECKIN_REMINDER_HOUR] ?: 20,
            checkInReminderMinute = prefs[Keys.CHECKIN_REMINDER_MINUTE] ?: 0,
            checkInReminderEnabled = prefs[Keys.CHECKIN_REMINDER_ENABLED] ?: false,
            typicalDrinksPerDay = prefs[Keys.TYPICAL_DRINKS_PER_DAY] ?: 3,
            priceCentsPerDrink = prefs[Keys.PRICE_CENTS_PER_DRINK] ?: 800,
            previousStreaksTotalDays = prefs[Keys.PREVIOUS_STREAKS_TOTAL_DAYS] ?: 0L,
            hoursPerDrink = prefs[Keys.HOURS_PER_DRINK] ?: 1,
            eveningReviewEnabled = prefs[Keys.EVENING_REVIEW_ENABLED] ?: false,
            companionEnabled = prefs[Keys.COMPANION_ENABLED] ?: false,
            companionHighestStage = (prefs[Keys.COMPANION_HIGHEST_STAGE] ?: 1).coerceIn(1, 5),
        )
    }

    suspend fun completeOnboarding(startDateEpochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = true
            prefs[Keys.START_DATE_EPOCH_DAY] = startDateEpochDay
            prefs[Keys.GOAL_MODE] = "QUIT"
        }
    }

    suspend fun setStartDate(epochDay: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.START_DATE_EPOCH_DAY] = epochDay }
    }

    suspend fun setCheckInReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHECKIN_REMINDER_ENABLED] = enabled
            prefs[Keys.CHECKIN_REMINDER_HOUR] = hour
            prefs[Keys.CHECKIN_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setDrinkingBaseline(typicalDrinksPerDay: Int, priceCentsPerDrink: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TYPICAL_DRINKS_PER_DAY] = typicalDrinksPerDay.coerceAtLeast(0)
            prefs[Keys.PRICE_CENTS_PER_DRINK] = priceCentsPerDrink.coerceAtLeast(0)
        }
    }

    /**
     * Set the hours-per-drink estimate used by the "hours reclaimed" Stats
     * card (PHASE_2 §9A2). Coerced to a sensible 0–24 range so a typo
     * doesn't produce nonsense multipliers.
     */
    suspend fun setHoursPerDrink(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOURS_PER_DRINK] = hours.coerceIn(0, 24)
        }
    }

    /**
     * Toggle the optional evening-review prompt (PHASE_2 §9A5). Off by default.
     */
    suspend fun setEveningReviewEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EVENING_REVIEW_ENABLED] = enabled
        }
    }

    /** Toggle the optional visual companion (PHASE_2 §9B1). Off by default. */
    suspend fun setCompanionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPANION_ENABLED] = enabled
        }
    }

    /**
     * Raise the companion high-water mark (PHASE_2 §9B1). Only writes when
     * [stage] is strictly greater than the current stored value; never
     * lowers the mark. Clamped to 1..5.
     */
    suspend fun raiseCompanionHighestStage(stage: Int) {
        val clamped = stage.coerceIn(1, 5)
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COMPANION_HIGHEST_STAGE] ?: 1
            if (clamped > current) prefs[Keys.COMPANION_HIGHEST_STAGE] = clamped
        }
    }

    /**
     * Add [days] to the lifetime "previous streaks" tally. Called by the
     * repository when the user resets away from an existing streak — the
     * banked days are what they'd already accumulated under the old start
     * date. Negative values are ignored (defensive: repo always passes ≥ 0).
     */
    suspend fun addPreviousStreakDays(days: Long) {
        if (days <= 0L) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PREVIOUS_STREAKS_TOTAL_DAYS] ?: 0L
            prefs[Keys.PREVIOUS_STREAKS_TOTAL_DAYS] = current + days
        }
    }

    /**
     * Overwrite the lifetime "previous streaks" tally. Called only by the
     * import flow — we replace in place so restoring a backup produces the
     * same lifetime counter the user had when they exported.
     */
    suspend fun setPreviousStreaksTotalDays(days: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREVIOUS_STREAKS_TOTAL_DAYS] = days.coerceAtLeast(0L)
        }
    }

    /** Wipe all user prefs — used by the Settings "delete all my data" action. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    /**
     * Live map of tool-key → open count. Keys are [com.tidelet.app.data.db.SosToolKey]
     * string constants. Surfaced on Stats; never leaves the device.
     */
    val toolOpenCounts: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        prefs.asMap()
            .mapNotNull { (k, v) ->
                val name = k.name
                if (name.startsWith("tool_count_") && v is Int) {
                    name.removePrefix("tool_count_") to v
                } else null
            }
            .toMap()
    }

    /** Increment the open-count for a given tool. Safe to call on every entry. */
    suspend fun incrementToolCount(toolKey: String) {
        context.dataStore.edit { prefs ->
            val key = Keys.toolCountKey(toolKey)
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    /**
     * Replace every `tool_count_*` key with the values in [counts]. Called by
     * the import flow to restore counts from a backup. Existing keys not in
     * the map are cleared so the map represents the full post-condition.
     */
    suspend fun setToolOpenCounts(counts: Map<String, Int>) {
        context.dataStore.edit { prefs ->
            // Clear existing tool_count_* keys so the import's view wins.
            val existingKeys = prefs.asMap().keys
                .filter { it.name.startsWith("tool_count_") }
            existingKeys.forEach { key ->
                @Suppress("UNCHECKED_CAST")
                prefs.remove(key as androidx.datastore.preferences.core.Preferences.Key<Any>)
            }
            counts.forEach { (tool, count) ->
                prefs[Keys.toolCountKey(tool)] = count
            }
        }
    }
}
