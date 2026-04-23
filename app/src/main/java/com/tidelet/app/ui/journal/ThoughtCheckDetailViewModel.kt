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
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ThoughtCheckDetailUiState(
    val loaded: Boolean = false,
    val record: ThoughtRecord? = null,
) {
    /** True when the record wasn't found in the observed list — e.g. deleted elsewhere. */
    val notFound: Boolean get() = loaded && record == null
}

/**
 * Read-only detail for a single ThoughtRecord. Derives the record by id from
 * the full [TideletRepository.thoughtRecords] flow (the list is small and
 * already in memory on the list screen). No new DAO query required.
 */
class ThoughtCheckDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    repoOverride: TideletRepository? = null,
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    private val recordId: Long =
        savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L

    val state: StateFlow<ThoughtCheckDetailUiState> = repo.thoughtRecords
        .map { records ->
            ThoughtCheckDetailUiState(
                loaded = true,
                record = records.firstOrNull { it.id == recordId },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThoughtCheckDetailUiState(loaded = false),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = this.createSavedStateHandle()
                ThoughtCheckDetailViewModel(app, handle)
            }
        }
    }
}
