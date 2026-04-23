package com.tidelet.app

import android.app.Application
import com.tidelet.app.data.db.TideletDatabase
import com.tidelet.app.data.prefs.UserPreferences
import com.tidelet.app.data.repo.RoomTideletRepository
import com.tidelet.app.data.repo.TideletRepository
import com.tidelet.app.widget.StreakWidgetRefreshWorker

/**
 * Application class — created once when the process starts.
 *
 * We use it as a tiny DI (dependency-injection) container: lazy-init the database,
 * prefs, and repository and expose them to the rest of the app.
 *
 * If this app grows past 20 screens, replace this with Hilt. For now, manual wiring
 * is simpler and has zero ceremony.
 *
 * `open` so tests can swap in a subclass that exposes an overridable repository —
 * see TestTideletApplication under src/test and src/androidTest.
 *
 * Wired into the manifest via `android:name=".TideletApplication"`.
 */
open class TideletApplication : Application() {

    open val repository: TideletRepository by lazy {
        RoomTideletRepository(
            db = TideletDatabase.get(this),
            prefs = UserPreferences(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Kick off the hourly widget refresh worker. Idempotent — if it's
        // already scheduled, WorkManager keeps the existing request.
        // Guarded so tests that subclass TideletApplication (e.g. the
        // TestTideletApplication used by Robolectric) can opt out.
        if (shouldScheduleWidgetRefresh()) {
            StreakWidgetRefreshWorker.schedule(this)
        }
    }

    /**
     * Override in test subclasses to skip the WorkManager schedule — tests
     * don't need a periodic worker firing during the test run.
     */
    protected open fun shouldScheduleWidgetRefresh(): Boolean = true
}
