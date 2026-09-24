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
 * VM for the CBT Thought Check screen.
 *
 * Holds the user's in-flight answers across the four prompts and the
 * optional distortion tag, so rotation/backgrounding doesn't wipe them.
 * On save we persist a [ThoughtRecord] plus log a GOT_THROUGH
 * [CravingEvent] under [SosToolKey.THOUGHT_CHECK] so the Stats tab sees
 * this rung as having landed. Skipping logs STILL_STRUGGLING, matching
 * the pattern used by JournalViewModel.
 */
class ThoughtCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    private val _state = MutableStateFlow(ThoughtCheckUiState())
    val state: StateFlow<ThoughtCheckUiState> = _state.asStateFlow()

    fun setSituation(v: String) = _state.update { it.copy(situation = v) }
    fun setThought(v: String) = _state.update { it.copy(thought = v) }
    fun setChallenge(v: String) = _state.update { it.copy(challenge = v) }
    fun setFriendReframe(v: String) = _state.update { it.copy(friendReframe = v) }

    /** Toggle a distortion tag — passing the same key again clears it. */
    fun toggleDistortion(key: String) {
        _state.update { s ->
            s.copy(distortionKey = if (s.distortionKey == key) null else key)
        }
    }

    fun save(intensity: Int? = null) {
        val s = _state.value
        if (s.thought.isBlank()) return

        viewModelScope.launch {
            repo.addThoughtRecord(
                date = LocalDate.now(),
                situation = s.situation,
                thought = s.thought,
                challenge = s.challenge,
                friendReframe = s.friendReframe,
                distortionTag = s.distortionKey,
            )
            repo.logCravingEvent(
                tool = SosToolKey.THOUGHT_CHECK,
                outcome = CravingOutcome.GOT_THROUGH,
                intensity = intensity,
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun skip(intensity: Int? = null) {
        viewModelScope.launch {
            repo.logCravingEvent(
                tool = SosToolKey.THOUGHT_CHECK,
                outcome = CravingOutcome.STILL_STRUGGLING,
                intensity = intensity,
            )
        }
    }
}

data class ThoughtCheckUiState(
    val situation: String = "",
    val thought: String = "",
    val challenge: String = "",
    val friendReframe: String = "",
    /** Stable key from [Distortion.key], or null if untagged. */
    val distortionKey: String? = null,
    val saved: Boolean = false,
)
