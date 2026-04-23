package com.tidelet.app.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.widget.StreakWidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Holds the onboarding flow's step + start-date, and writes them to DataStore on finish.
 *
 * We extend AndroidViewModel because we need the Application context to reach the repository
 * (we're intentionally not using Hilt for v1).
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    fun finishOnboarding(startDate: LocalDate) {
        viewModelScope.launch {
            repo.completeOnboarding(startDate)
            // If the user has the home-screen widget placed, refresh it
            // immediately so the count reflects their freshly-set start date
            // without waiting for the hourly WorkManager tick.
            StreakWidgetUpdater.refreshAll(getApplication())
        }
    }
}
