package com.tidelet.app.widget

import android.content.Context
import com.tidelet.app.R
import com.tidelet.app.util.StreakDuration

/**
 * Pure function that maps a [StreakDuration] (or null — no start date yet) to
 * the short string the home-screen widget shows.
 *
 * Kept Android-free apart from the Context it needs to resolve string
 * resources, so it can be unit-tested with Robolectric without any widget or
 * RemoteViews plumbing.
 */
fun streakWidgetText(context: Context, streak: StreakDuration?): String = when {
    streak == null || streak.days == 0L ->
        context.getString(R.string.widget_start_your_streak)
    streak.days == 1L ->
        context.getString(R.string.widget_streak_singular, streak.days)
    else ->
        context.getString(R.string.widget_streak_plural, streak.days)
}
