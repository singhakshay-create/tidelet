package com.tidelet.app.testutil

import androidx.test.core.app.ApplicationProvider
import com.tidelet.app.fakes.FakeTideletRepository

/**
 * Resolve the running [TestTideletApplication] and its default fake repo.
 *
 * Use this from `@Before` in a Compose UI test to seed state on the same
 * fake the Application hands out to ViewModels via manual DI.
 *
 * ```
 * private lateinit var fakeRepo: FakeTideletRepository
 *
 * @Before fun setup() {
 *     val (_, repo) = resolveTestApp()
 *     fakeRepo = repo
 *     fakeRepo.seedReasons(...)
 * }
 * ```
 */
fun resolveTestApp(): Pair<TestTideletApplication, FakeTideletRepository> {
    val app = ApplicationProvider.getApplicationContext<TestTideletApplication>()
    val repo = app.repository as FakeTideletRepository
    return app to repo
}
