package com.tidelet.app.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/**
 * VM behind the optional evening mini-review (PHASE_2 §9A5).
 *
 * Two free-text fields — one win, one challenge — keyed by today's date.
 * Submit is allowed when either field is non-blank; both blank is a no-op
 * (treated like "Not now"). On save, the screen pops back to Home.
 */
data class EveningReviewUiState(
    val winText: String = "",
    val challengeText: String = "",
    val saved: Boolean = false,
) {
    /** Save is enabled only when at least one field has substantive content. */
    val canSave: Boolean get() =
        winText.trim().isNotEmpty() || challengeText.trim().isNotEmpty()
}

class EveningReviewViewModel @JvmOverloads constructor(
    application: Application,
    repoOverride: TideletRepository? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    private val _state = MutableStateFlow(EveningReviewUiState())
    val state: StateFlow<EveningReviewUiState> = _state.asStateFlow()

    init {
        // Pre-fill if today's review already exists — same-day editing is OK
        // because the DAO upserts by date. Lets a user fix a typo without
        // navigating around the app to find the entry.
        viewModelScope.launch {
            val today = LocalDate.now(clock).toString()
            repo.findEveningReview(today)?.let { existing ->
                _state.update {
                    it.copy(winText = existing.winText, challengeText = existing.challengeText)
                }
            }
        }
    }

    fun setWin(text: String) = _state.update { it.copy(winText = text) }
    fun setChallenge(text: String) = _state.update { it.copy(challengeText = text) }

    /**
     * Persist the review for today. No-op when both fields are blank — the
     * screen's Save button is disabled in that case anyway, but we double-
     * check here so a programmatic call can't write an empty row.
     */
    fun save() {
        if (!_state.value.canSave) return
        val today = LocalDate.now(clock).toString()
        viewModelScope.launch {
            repo.upsertEveningReview(
                dateIso = today,
                winText = _state.value.winText.trim(),
                challengeText = _state.value.challengeText.trim(),
            )
            _state.update { it.copy(saved = true) }
        }
    }
}
