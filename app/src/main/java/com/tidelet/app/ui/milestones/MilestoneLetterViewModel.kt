package com.tidelet.app.ui.milestones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tidelet.app.TideletApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for writing a milestone letter to the user's future self.
 *
 * Reads the `milestoneDays` nav argument, loads any existing letter for that
 * milestone (so editing is supported), and persists via
 * [com.tidelet.app.data.repo.TideletRepository.saveMilestoneLetter].
 */
data class MilestoneLetterUiState(
    val milestoneDays: Int = 0,
    val text: String = "",
    val loaded: Boolean = false,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = text.trim().isNotEmpty()
}

class MilestoneLetterViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    private val milestoneDays: Int =
        savedStateHandle.get<String>("days")?.toIntOrNull() ?: 0

    private val _state = MutableStateFlow(MilestoneLetterUiState(milestoneDays = milestoneDays))
    val state: StateFlow<MilestoneLetterUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repo.findMilestoneLetter(milestoneDays)
            _state.update {
                it.copy(text = existing?.text.orEmpty(), loaded = true)
            }
        }
    }

    fun setText(text: String) {
        _state.update { it.copy(text = text) }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repo.saveMilestoneLetter(s.milestoneDays, s.text)
            _state.update { it.copy(saved = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = this.createSavedStateHandle()
                MilestoneLetterViewModel(app, handle)
            }
        }
    }
}
