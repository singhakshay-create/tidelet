package com.tidelet.app

import android.content.Context
import com.tidelet.app.data.repo.TideletRepository
import java.time.Clock

/**
 * No-op twin of the real `DemoDataSeeder` in `app/src/debug/`. It exists
 * only so `TideletApplication.onCreate()` (in `src/main`, compiled into
 * every build variant) has something to resolve against when the *release*
 * variant is compiled — which merges `src/main` with `src/release`, never
 * `src/debug`. This stub carries no demo data and no seeding logic, and
 * does nothing at runtime; it's also gated behind `BuildConfig.DEBUG` at the
 * call site as a second, belt-and-suspenders guard.
 */
object DemoDataSeeder {
    @Suppress("UNUSED_PARAMETER")
    fun seedIfNeeded(
        context: Context,
        repository: TideletRepository,
        clock: Clock = Clock.systemDefaultZone(),
    ) {
        // Intentionally empty — demo data must never ship in a release build.
    }
}
