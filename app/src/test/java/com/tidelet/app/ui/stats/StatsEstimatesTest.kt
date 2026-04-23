package com.tidelet.app.ui.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-JVM tests for the small estimate helpers behind the Stats cards.
 *
 * Keeping these on the JVM side means we can verify arithmetic — including
 * the zero/overflow edges — without a Robolectric shadow or instrumentation.
 */
class StatsEstimatesTest {

    @Test
    fun `money saved is zero on day zero`() {
        val cents = estimateMoneySavedCents(
            drinkFreeDays = 0,
            typicalDrinksPerDay = 3,
            priceCentsPerDrink = 800,
        )
        assertThat(cents).isEqualTo(0L)
    }

    @Test
    fun `money saved scales linearly in all three factors`() {
        val cents = estimateMoneySavedCents(
            drinkFreeDays = 14,
            typicalDrinksPerDay = 4,
            priceCentsPerDrink = 250,
        )
        // 14 × 4 × 250 = 14000 cents
        assertThat(cents).isEqualTo(14_000L)
    }

    @Test
    fun `hours reclaimed multiplies streak days × drinks per day × hours per drink`() {
        // 3 drinks/day × 1h/drink × 14 days = 42 hours
        assertThat(
            estimateHoursReclaimed(
                streakDays = 14L,
                typicalDrinksPerDay = 3,
                hoursPerDrink = 1,
            )
        ).isEqualTo(42L)

        // Double the hours per drink → double the payoff
        assertThat(
            estimateHoursReclaimed(
                streakDays = 14L,
                typicalDrinksPerDay = 3,
                hoursPerDrink = 2,
            )
        ).isEqualTo(84L)
    }

    @Test
    fun `hours reclaimed is zero on day zero regardless of baseline`() {
        assertThat(
            estimateHoursReclaimed(
                streakDays = 0L,
                typicalDrinksPerDay = 5,
                hoursPerDrink = 3,
            )
        ).isEqualTo(0L)
    }

    @Test
    fun `hours reclaimed is zero when hours per drink is zero`() {
        // User might set hours/drink = 0 to hide the card's payoff framing.
        // Math should cooperate without special-casing.
        assertThat(
            estimateHoursReclaimed(
                streakDays = 100L,
                typicalDrinksPerDay = 3,
                hoursPerDrink = 0,
            )
        ).isEqualTo(0L)
    }
}
