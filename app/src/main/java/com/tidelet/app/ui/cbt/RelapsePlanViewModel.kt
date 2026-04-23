package com.tidelet.app.ui.cbt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.RelapsePlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * VM behind the Relapse Prevention Plan screen.
 *
 * One screen, one singleton record. We load the saved plan on init (if any),
 * seed the editable fields from it, and only write back when the user taps
 * save. Auto-save was considered and rejected — the user writes this in a
 * calm moment and we want intent behind the save.
 *
 * The three fields are long text and have no cross-validation. Empty strings
 * are allowed — the whole plan could be empty for a user who only wanted
 * "coping plan" written down. The repo upsert is unconditional.
 */
class RelapsePlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    private val _state = MutableStateFlow(RelapsePlanUiState())
    val state: StateFlow<RelapsePlanUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repo.findRelapsePlan()
            if (existing != null) {
                _state.update {
                    it.copy(
                        highRiskSituations = existing.highRiskSituations,
                        earlyWarningSigns = existing.earlyWarningSigns,
                        copingPlan = existing.copingPlan,
                        loaded = true,
                        hasExistingPlan = true,
                    )
                }
            } else {
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    fun setHighRiskSituations(value: String) {
        _state.update { it.copy(highRiskSituations = value, justSaved = false) }
    }

    fun setEarlyWarningSigns(value: String) {
        _state.update { it.copy(earlyWarningSigns = value, justSaved = false) }
    }

    fun setCopingPlan(value: String) {
        _state.update { it.copy(copingPlan = value, justSaved = false) }
    }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            repo.saveRelapsePlan(
                highRiskSituations = s.highRiskSituations,
                earlyWarningSigns = s.earlyWarningSigns,
                copingPlan = s.copingPlan,
            )
            _state.update { it.copy(justSaved = true, hasExistingPlan = true) }
        }
    }
}

data class RelapsePlanUiState(
    val highRiskSituations: String = "",
    val earlyWarningSigns: String = "",
    val copingPlan: String = "",
    /** Flipped true once the init block has read from the DB. */
    val loaded: Boolean = false,
    /** True if the DB had a row at load time. Used to toggle the heading copy. */
    val hasExistingPlan: Boolean = false,
    /** Flipped true after a successful save; any edit resets it. */
    val justSaved: Boolean = false,
)
