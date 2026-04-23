package com.tidelet.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Hourly periodic worker that asks every placed [StreakWidgetProvider] to
 * re-render. Without this, a user who doesn't open the app for 24+ hours
 * sees a stale day-count on the widget until Android's own
 * `updatePeriodMillis` fires — which is best-effort and not guaranteed at
 * fine granularity.
 *
 * One hour is the right cadence: day count changes at most once every 24h,
 * so we refresh ~24× more than strictly needed, but the battery cost is
 * negligible (a DataStore read + a broadcast send).
 */
class StreakWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        StreakWidgetUpdater.refreshAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "tidelet.streak-widget-refresh"

        /**
         * Schedule the worker if it isn't already scheduled. Idempotent —
         * calling this more than once (e.g. every app launch) is safe thanks
         * to [ExistingPeriodicWorkPolicy.KEEP].
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StreakWidgetRefreshWorker>(
                1, TimeUnit.HOURS,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
