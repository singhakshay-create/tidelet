package com.tidelet.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import kotlinx.coroutines.launch

/**
 * Tiny ViewModel for the Breathe screen.
 *
 * Breathing doesn't have the same "how did that go?" outcome question as Ride the Wave —
 * if the user completed any breathing at all, we log that as a got-through attempt
 * so Stats/insights can count the tool as used. If they bail with zero cycles we log
 * nothing (no point polluting the log with a screen they didn't actually use).
 */
class BreatheViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    init {
        viewModelScope.launch { repo.recordToolOpen(SosToolKey.BREATHE) }
    }

    fun logCompleted() {
        viewModelScope.launch {
            repo.logCravingEvent(
                tool = SosToolKey.BREATHE,
                outcome = CravingOutcome.GOT_THROUGH,
            )
        }
    }
}
