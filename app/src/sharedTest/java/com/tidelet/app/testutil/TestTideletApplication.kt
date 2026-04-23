package com.tidelet.app.testutil

import com.tidelet.app.TideletApplication
import com.tidelet.app.data.repo.TideletRepository
import com.tidelet.app.fakes.FakeTideletRepository

/**
 * Application subclass used by both Robolectric unit tests and instrumented
 * tests. It swaps the real Room-backed repository for a default
 * [FakeTideletRepository], with an override hook for tests that want to seed
 * specific state before the SUT reads it.
 *
 * Unit tests: activate with
 * ```
 * @RunWith(RobolectricTestRunner::class)
 * @Config(application = TestTideletApplication::class)
 * ```
 *
 * Instrumented tests: wire via [TideletTestRunner] (registered as
 * `testInstrumentationRunner` in app/build.gradle.kts) and retrieve with
 * `ApplicationProvider.getApplicationContext<TestTideletApplication>()`.
 */
class TestTideletApplication : TideletApplication() {

    private val fake: FakeTideletRepository = FakeTideletRepository()

    override val repository: TideletRepository
        get() = overrideRepository ?: fake

    /** Defaults to a pristine [FakeTideletRepository]; tests may swap. */
    var overrideRepository: TideletRepository? = null

    /** Skip the widget-refresh WorkManager schedule in tests — not needed. */
    override fun shouldScheduleWidgetRefresh(): Boolean = false
}
