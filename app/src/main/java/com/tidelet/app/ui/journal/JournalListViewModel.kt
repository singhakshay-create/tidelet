package com.tidelet.app.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class JournalListUiState(
    val loaded: Boolean = false,
    val query: String = "",
    val all: List<JournalEntry> = emptyList(),
    val filtered: List<JournalEntry> = emptyList(),
) {
    val isEmpty: Boolean get() = loaded && all.isEmpty()
    val isSearching: Boolean get() = query.isNotBlank()
    val hasNoMatches: Boolean
        get() = loaded && isSearching && filtered.isEmpty() && all.isNotEmpty()
}

/**
 * Read-side list of journal entries. Reverse-chronological (from DAO), with
 * a client-side case-insensitive substring filter applied to [JournalEntry.text].
 */
class JournalListViewModel @JvmOverloads constructor(
    application: Application,
    repoOverride: TideletRepository? = null,
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    private val query = MutableStateFlow("")

    val state: StateFlow<JournalListUiState> = combine(
        repo.journalEntries,
        query,
    ) { entries, q ->
        val trimmed = q.trim()
        val filtered = if (trimmed.isEmpty()) {
            entries
        } else {
            entries.filter { it.text.contains(trimmed, ignoreCase = true) }
        }
        JournalListUiState(
            loaded = true,
            query = q,
            all = entries,
            filtered = filtered,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = JournalListUiState(loaded = false),
    )

    fun setQuery(q: String) {
        query.value = q
    }

    fun clearQuery() {
        query.value = ""
    }
}
