package com.tidelet.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.tidelet.app.MainActivity
import com.tidelet.app.R
import com.tidelet.app.data.prefs.UserPreferences
import com.tidelet.app.util.streakFromStartDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Home-screen widget that shows the user's current streak day-count.
 *
 * Design constraints matching the rest of the app:
 *   - Reads DataStore directly (not via the app's repository singleton) —
 *     widget code can execute in a background process and must not depend on
 *     [com.tidelet.app.TideletApplication.repository] being awake.
 *   - Does one blocking DataStore read per refresh — a few ms, well within
 *     `onUpdate`'s budget. We don't launch coroutines here because
 *     AppWidgetProvider's lifetime ends as soon as `onUpdate` returns.
 *   - No network, no analytics, fully local — same privacy stance as the app.
 *
 * Refresh triggers:
 *   - Android's own `updatePeriodMillis` (set in streak_widget_info.xml).
 *   - Our WorkManager worker ([StreakWidgetRefreshWorker]) which requests a
 *     refresh once per hour so the day-count ticks even when the app is
 *     backgrounded for long stretches.
 *   - Explicit calls from [StreakWidgetUpdater.refreshAll] after the user
 *     edits their start date in onboarding or Settings.
 */
class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val text = resolveWidgetText(context)
        val tapIntent = launchAppPendingIntent(context)

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.streak_widget).apply {
                setTextViewText(R.id.widget_streak_text, text)
                setOnClickPendingIntent(R.id.widget_root, tapIntent)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun resolveWidgetText(context: Context): String {
        val profile = runBlocking { UserPreferences(context).profile.first() }
        val startDate = profile.startDateEpochDay?.let(LocalDate::ofEpochDay)
        val streak = startDate?.let { streakFromStartDate(it) }
        return streakWidgetText(context, streak)
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Returns the component name used to address this provider. */
        fun componentOf(context: Context): ComponentName =
            ComponentName(context, StreakWidgetProvider::class.java)
    }
}
