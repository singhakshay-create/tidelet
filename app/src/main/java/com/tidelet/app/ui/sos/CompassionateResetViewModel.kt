package com.tidelet.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Minimal VM that lets the Compassionate Reset screen offer an optional
 * in-line functional analysis. The screen itself was previously stateless;
 * now that it hosts a persistable form we need a small owner for
 * coroutine-scoped writes.
 *
 * The analysis is its own entity, so we don't disturb the CheckIn the user
 * may or may not log after this screen — those are separate surfaces by
 * design (see class doc on CompassionateResetScreen).
 */
class CompassionateResetViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    private val _state = MutableStateFlow(CompassionateResetUiState())
    val state: StateFlow<CompassionateResetUiState> = _state.asStateFlow()

    fun saveAnalysis(antecedent: String?, thought: String?, followingAction: String?) {
        viewModelScope.launch {
            repo.addFunctionalAnalysis(
                date = LocalDate.now(),
                // Not joined to a specific CravingEvent: the DRANK event was
                // logged by RideTheWaveViewModel before landing here, and
                // round-tripping its id across nav is more wiring than it's
                // worth for v1. We index by date instead.
                cravingEventId = null,
                antecedent = antecedent,
                thoughtAtMoment = thought,
                followingAction = followingAction,
            )
            _state.update { it.copy(analysisSaved = true) }
        }
    }

    fun dismissAnalysis() {
        _state.update { it.copy(analysisDismissed = true) }
    }
}

data class CompassionateResetUiState(
    val analysisSaved: Boolean = false,
    val analysisDismissed: Boolean = false,
)
