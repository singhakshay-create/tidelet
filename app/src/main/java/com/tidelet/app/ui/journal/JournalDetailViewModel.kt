package com.tidelet.app.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class JournalDetailUiState(
    val loaded: Boolean = false,
    val entry: JournalEntry? = null,
) {
    val notFound: Boolean get() = loaded && entry == null
}

/**
 * Read-only detail screen for a single journal entry. Derives from the full
 * journal-entries flow by id — same rationale as [ThoughtCheckDetailViewModel].
 */
class JournalDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    repoOverride: TideletRepository? = null,
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    private val entryId: Long =
        savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L

    val state: StateFlow<JournalDetailUiState> = repo.journalEntries
        .map { entries ->
            JournalDetailUiState(
                loaded = true,
                entry = entries.firstOrNull { it.id == entryId },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = JournalDetailUiState(loaded = false),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = this.createSavedStateHandle()
                JournalDetailViewModel(app, handle)
            }
        }
    }
}
