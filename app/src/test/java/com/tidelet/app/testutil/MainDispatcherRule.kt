package com.tidelet.app.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that swaps `Dispatchers.Main` for a `TestDispatcher` around each
 * test. Required any time a ViewModel launches into `viewModelScope` — that
 * scope uses `Dispatchers.Main.immediate`, which throws on a plain JVM.
 *
 * Share a single dispatcher between the rule and tests that need to drive
 * time: `val dispatcher = StandardTestDispatcher(); MainDispatcherRule(dispatcher)`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
