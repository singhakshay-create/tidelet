package com.tidelet.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import kotlinx.coroutines.launch

/**
 * Thin VM for the "Do something else" screen.
 *
 * The list of distractions is loaded from `@array/builtin_distractions` in the
 * Composable itself (it's static data — no reason to hoist it through the VM).
 * The VM's only job is to log outcomes when the user taps "I did it" or
 * "Nothing worked".
 */
class DistractionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    init {
        viewModelScope.launch { repo.recordToolOpen(SosToolKey.DISTRACTIONS) }
    }

    fun logDidIt() = log(CravingOutcome.GOT_THROUGH)
    fun logNothingWorked() = log(CravingOutcome.STILL_STRUGGLING)

    private fun log(outcome: String) {
        viewModelScope.launch {
            repo.logCravingEvent(tool = SosToolKey.DISTRACTIONS, outcome = outcome)
        }
    }
}
