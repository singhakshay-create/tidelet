package com.tidelet.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.Reason
import com.tidelet.app.data.db.SosToolKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReasonsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    init {
        viewModelScope.launch { repo.recordToolOpen(SosToolKey.REASONS) }
    }

    /**
     * Collected lazily — UI re-renders when the user adds or deletes a reason.
     * [SharingStarted.WhileSubscribed] releases the underlying Flow when the
     * screen goes out of composition, which matters here because the query
     * comes straight from Room.
     */
    val reasons: StateFlow<List<Reason>> = repo.reasons
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    fun addReason(text: String) {
        viewModelScope.launch { repo.addReason(text) }
    }

    fun deleteReason(id: Long) {
        viewModelScope.launch { repo.deleteReason(id) }
    }

    /**
     * Call when the user finishes browsing their reasons. We log it as a
     * "got through" event for the REASONS tool so Phase 2 Stats can show how
     * often each tool actually helped. If they arrived and left without any
     * reasons in the list we skip the log (there was nothing to read).
     */
    fun logViewed(hadReasons: Boolean, intensity: Int? = null) {
        if (!hadReasons) return
        viewModelScope.launch {
            repo.logCravingEvent(
                tool = SosToolKey.REASONS,
                outcome = CravingOutcome.GOT_THROUGH,
                intensity = intensity,
            )
        }
    }
}
