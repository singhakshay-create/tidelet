package com.tidelet.app.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tidelet.app.testutil.TestTideletApplication
import com.tidelet.app.util.StreakDuration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pure-function coverage for [streakWidgetText]. Runs under Robolectric
 * only so we can resolve the string resources — no widget / RemoteViews
 * plumbing is involved.
 *
 * Uses [TestTideletApplication] (same as every other Robolectric test here)
 * so the real [com.tidelet.app.TideletApplication.onCreate] doesn't schedule
 * the WorkManager widget-refresh worker against an uninitialized WorkManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestTideletApplication::class, sdk = [34])
class StreakWidgetTextTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `null streak shows CTA`() {
        assertThat(streakWidgetText(context, null)).isEqualTo("Start your streak")
    }

    @Test
    fun `zero-day streak shows CTA`() {
        val streak = StreakDuration(days = 0, hours = 3, minutes = 12)
        assertThat(streakWidgetText(context, streak)).isEqualTo("Start your streak")
    }

    @Test
    fun `one-day streak uses singular form`() {
        val streak = StreakDuration(days = 1, hours = 0, minutes = 0)
        assertThat(streakWidgetText(context, streak)).isEqualTo("1 day")
    }

    @Test
    fun `multi-day streak uses plural form`() {
        val streak = StreakDuration(days = 42, hours = 0, minutes = 0)
        assertThat(streakWidgetText(context, streak)).isEqualTo("42 days")
    }
}
