package com.tidelet.app.ui.cbt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.RefusalPhrase
import com.tidelet.app.data.db.SosToolKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * VM behind Drink Refusal Rehearsal.
 *
 * Two buckets of phrases are on this screen:
 *
 *   - **Built-in** suggestions (from strings.xml, owned by the UI layer — not
 *     persisted, not editable). The screen hands the count of built-ins into
 *     [practiceRandom] so the VM can fairly sample across both buckets.
 *   - **User** additions (persisted rows in refusal_phrase).
 *
 * The "practice" feature picks a phrase at random from the union of the two
 * and highlights it. We surface the picked id/key via [practiceTargetId] so
 * the screen can render it (with a stable identifier across rebuilds until
 * the user taps again).
 *
 * Logs a tiny craving-event entry when the user actually rehearses, mirroring
 * the other SOS tools. Outcome = GOT_THROUGH since rehearsal is purely
 * proactive practice.
 */
class RefusalRehearsalViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    /** User-authored phrases only. Built-ins come from resources at the UI layer. */
    val userPhrases: StateFlow<List<RefusalPhrase>> = repo.refusalPhrases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _state = MutableStateFlow(RefusalRehearsalUiState())
    val state: StateFlow<RefusalRehearsalUiState> = _state.asStateFlow()

    fun setNewPhraseDraft(text: String) {
        _state.update { it.copy(newPhraseDraft = text) }
    }

    fun addPhrase() {
        val draft = _state.value.newPhraseDraft.trim()
        if (draft.isEmpty()) return
        viewModelScope.launch {
            repo.addRefusalPhrase(draft)
            _state.update { it.copy(newPhraseDraft = "") }
        }
    }

    fun deletePhrase(id: Long) {
        viewModelScope.launch { repo.deleteRefusalPhrase(id) }
    }

    /**
     * Pick a phrase at random from the union of built-ins + user phrases.
     * If the user has no phrases and the built-ins list is empty (shouldn't
     * happen in practice), we no-op.
     */
    fun practiceRandom(builtinCount: Int) {
        val userCount = userPhrases.value.size
        val total = builtinCount + userCount
        if (total == 0) return
        val idx = (0 until total).random()
        val target = if (idx < builtinCount) {
            PracticeTarget.Builtin(index = idx)
        } else {
            val userIdx = idx - builtinCount
            val phrase = userPhrases.value[userIdx]
            PracticeTarget.User(phraseId = phrase.id)
        }
        _state.update { it.copy(practiceTarget = target, rehearsedCount = it.rehearsedCount + 1) }

        // Fire-and-forget analytics-style log. Rehearsing counts as a
        // successful practice event, not a "near miss" craving.
        viewModelScope.launch {
            repo.logCravingEvent(
                tool = SosToolKey.REFUSAL_PRACTICE,
                outcome = CravingOutcome.GOT_THROUGH,
            )
        }
    }

    fun clearPracticeTarget() {
        _state.update { it.copy(practiceTarget = null) }
    }
}

data class RefusalRehearsalUiState(
    val newPhraseDraft: String = "",
    /** Currently-highlighted phrase, or null when nothing is picked. */
    val practiceTarget: PracticeTarget? = null,
    /** Session-local counter — survives recompositions but not process death. */
    val rehearsedCount: Int = 0,
)

/** Tag-union for "which phrase did the picker land on". */
sealed interface PracticeTarget {
    data class Builtin(val index: Int) : PracticeTarget
    data class User(val phraseId: Long) : PracticeTarget
}
