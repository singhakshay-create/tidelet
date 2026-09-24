package com.tidelet.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * VM for the SOS Journal screen.
 *
 * Journal is the "last rung" of the SOS toolkit — after Ride the Wave and
 * Breathe, if the user is still spinning, naming the thing in words often
 * breaks the loop. We use a single evidence-based prompt ("what's really
 * going on right now?") and save whatever the user types to [JournalEntry]
 * keyed to today.
 *
 * The VM holds draft text so rotation/backgrounding doesn't wipe it, and
 * exposes a [saved] flag the screen uses to pivot from the editor to a
 * short confirmation state.
 */
class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state.asStateFlow()

    fun setText(newText: String) {
        _state.update { it.copy(text = newText) }
    }

    /**
     * Persist the draft and log a GOT_THROUGH craving event. No-ops on empty
     * text (prevents a tap on Save from logging a blank row — the screen
     * disables the button in that case too, but we belt-and-brace).
     */
    fun save(intensity: Int? = null) {
        val text = _state.value.text.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repo.addJournalEntry(date = LocalDate.now(), text = text)
            repo.logCravingEvent(
                tool = SosToolKey.JOURNAL,
                outcome = CravingOutcome.GOT_THROUGH,
                intensity = intensity,
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun skip(intensity: Int? = null) {
        viewModelScope.launch {
            repo.logCravingEvent(
                tool = SosToolKey.JOURNAL,
                outcome = CravingOutcome.STILL_STRUGGLING,
                intensity = intensity,
            )
        }
    }
}

data class JournalUiState(
    val text: String = "",
    val saved: Boolean = false,
)
