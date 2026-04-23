package com.tidelet.app.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.util.NextMilestone
import com.tidelet.app.util.StreakDuration
import com.tidelet.app.util.nextMilestoneFor
import com.tidelet.app.util.streakFromStartDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/**
 * Stats tab state.
 *
 * Built in response to the Reframe competitive assessment: users rank "tangible
 * progress surface" (days since, money saved, next milestone) as one of the
 * single most motivating features in the category.
 *
 * Intentionally glanceable: four small cards, no graphs. Reframe's Stats tab is
 * commonly cited as "cluttered and overwhelming" in negative reviews — our
 * restraint is a competitive position, not a gap to fill.
 *
 * All values are derived on-device from Room + DataStore. No network.
 */
data class StatsUiState(
    /** Null when the user has no start date yet (shouldn't happen post-onboarding). */
    val streak: StreakDuration?,
    val nextMilestone: NextMilestone?,
    /** Total drink-free check-ins recorded since onboarding. */
    val drinkFreeDaysTotal: Int,
    /** Drink-free check-ins within the current calendar month (local). */
    val drinkFreeDaysThisMonth: Int,
    /** Total days user has logged (drink or drink-free) — for denominator context. */
    val daysLogged: Int,
    /**
     * Money saved in cents. See [estimateMoneySavedCents] for how this is
     * computed — in short: drink-free check-ins × typical drinks/day × $/drink.
     */
    val moneySavedCents: Long,
    /** Exposed so the "money saved" card can explain the assumption. */
    val typicalDrinksPerDay: Int,
    val priceCentsPerDrink: Int,
    /**
     * Estimated hours reclaimed on the current streak (PHASE_2 §9A2).
     * Computed as streakDays × typicalDrinksPerDay × hoursPerDrink. Lives
     * alongside money saved as a second "progress beyond dollars" card.
     */
    val hoursReclaimed: Long,
    /** Exposed so the hours-reclaimed card can explain the assumption. */
    val hoursPerDrink: Int,
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    val state: StateFlow<StatsUiState> = combine(
        repo.profile,
        repo.checkIns,
    ) { profile, checkIns ->
        val startDate = profile.startDateEpochDay?.let(LocalDate::ofEpochDay)
        val streak = startDate?.let { streakFromStartDate(it) }
        val drinkFreeDaysTotal = checkIns.count { !it.didDrink }
        val thisMonth = YearMonth.now()
        val drinkFreeDaysThisMonth = checkIns.count { row ->
            !row.didDrink && isInMonth(row.date, thisMonth)
        }
        val moneySavedCents = estimateMoneySavedCents(
            drinkFreeDays = drinkFreeDaysTotal,
            typicalDrinksPerDay = profile.typicalDrinksPerDay,
            priceCentsPerDrink = profile.priceCentsPerDrink,
        )
        val hoursReclaimed = estimateHoursReclaimed(
            streakDays = streak?.days ?: 0L,
            typicalDrinksPerDay = profile.typicalDrinksPerDay,
            hoursPerDrink = profile.hoursPerDrink,
        )
        StatsUiState(
            streak = streak,
            nextMilestone = streak?.let { nextMilestoneFor(it.days) },
            drinkFreeDaysTotal = drinkFreeDaysTotal,
            drinkFreeDaysThisMonth = drinkFreeDaysThisMonth,
            daysLogged = checkIns.size,
            moneySavedCents = moneySavedCents,
            typicalDrinksPerDay = profile.typicalDrinksPerDay,
            priceCentsPerDrink = profile.priceCentsPerDrink,
            hoursReclaimed = hoursReclaimed,
            hoursPerDrink = profile.hoursPerDrink,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StatsUiState(
            streak = null,
            nextMilestone = null,
            drinkFreeDaysTotal = 0,
            drinkFreeDaysThisMonth = 0,
            daysLogged = 0,
            moneySavedCents = 0L,
            typicalDrinksPerDay = 3,
            priceCentsPerDrink = 800,
            hoursReclaimed = 0L,
            hoursPerDrink = 1,
        ),
    )

    private fun isInMonth(isoDate: String, month: YearMonth): Boolean = try {
        YearMonth.from(LocalDate.parse(isoDate)) == month
    } catch (_: Exception) {
        false
    }
}

/**
 * drink-free days × typical drinks/day × price-per-drink.
 *
 * Multiplied as Long to stay safe once the user passes ~250 days × 3 × 800c
 * (still well within Int, but Long is cheap here and makes future expansion
 * — e.g. yearly totals — risk-free).
 */
internal fun estimateMoneySavedCents(
    drinkFreeDays: Int,
    typicalDrinksPerDay: Int,
    priceCentsPerDrink: Int,
): Long = drinkFreeDays.toLong() * typicalDrinksPerDay.toLong() * priceCentsPerDrink.toLong()

/**
 * Hours reclaimed on the current streak (PHASE_2 §9A2).
 *
 * streakDays × drinks/day × hours/drink. Deliberately a "what you'd have
 * spent" framing rather than a sum of actual drinks — the latter would
 * require per-day counts that many users don't log. The estimate is
 * consistent with the money-saved card's assumption model.
 */
internal fun estimateHoursReclaimed(
    streakDays: Long,
    typicalDrinksPerDay: Int,
    hoursPerDrink: Int,
): Long = streakDays * typicalDrinksPerDay.toLong() * hoursPerDrink.toLong()
