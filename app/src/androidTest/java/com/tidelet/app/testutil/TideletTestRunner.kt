package com.tidelet.app.testutil

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom runner that swaps in [TestTideletApplication] for instrumented tests
 * so ViewModels see a [com.tidelet.app.fakes.FakeTideletRepository] instead
 * of the real Room + DataStore stack.
 *
 * Registered via `testInstrumentationRunner` in app/build.gradle.kts.
 */
class TideletTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, TestTideletApplication::class.java.name, context)
}
