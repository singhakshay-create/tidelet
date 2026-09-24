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

        // Debug-only: populate a demo 23-day streak + a few craving logs so
        // a debug build shows a populated app (screenshots, reviewers).
        // BuildConfig.DEBUG is a compile-time constant AGP generates per
        // build type (true for debug, false for release) — this is a
        // belt-and-suspenders guard on top of DemoDataSeeder itself being a
        // no-op in release builds (see app/src/release/.../DemoDataSeeder.kt).
        // Also guarded by shouldSeedDemoData() (same pattern as the widget
        // worker below) since BuildConfig.DEBUG is true for unit tests too —
        // a Robolectric test shouldn't have a background coroutine writing
        // demo data during app startup, on principle, even though it turned
        // out not to be the cause of any specific test failure.
        if (BuildConfig.DEBUG && shouldSeedDemoData()) {
            DemoDataSeeder.seedIfNeeded(this, repository)
        }
    }

    /**
     * Override in test subclasses to skip the WorkManager schedule — tests
     * don't need a periodic worker firing during the test run.
     */
    protected open fun shouldScheduleWidgetRefresh(): Boolean = true

    /**
     * Override in test subclasses to skip demo-data seeding — tests supply
     * their own fake/fixture data and shouldn't race against a background
     * coroutine writing to a real DataStore during app startup.
     */
    protected open fun shouldSeedDemoData(): Boolean = true
}
