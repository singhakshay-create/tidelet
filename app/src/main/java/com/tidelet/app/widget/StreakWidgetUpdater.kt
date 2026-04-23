package com.tidelet.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent

/**
 * Tiny helper that pokes every installed [StreakWidgetProvider] instance to
 * re-render. Call from any Context — ViewModel, Application, Worker — after
 * the user edits data the widget depends on.
 *
 * No-op when no widget is placed on the home screen (Android silently drops
 * the broadcast), so callers don't need to check first.
 */
object StreakWidgetUpdater {

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val component = StreakWidgetProvider.componentOf(appContext)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return

        val intent = Intent(appContext, StreakWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        appContext.sendBroadcast(intent)
    }
}
