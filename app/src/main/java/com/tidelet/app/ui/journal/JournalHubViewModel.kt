package com.tidelet.app.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class JournalHubUiState(
    val loaded: Boolean = false,
    val thoughtChecksCount: Int = 0,
    val journalEntriesCount: Int = 0,
    /** PHASE_2 §9A5 — count of past evening mini-reviews. */
    val eveningReviewsCount: Int = 0,
)

/**
 * Powers the read-side Journal hub tab. Just two integer counts derived from
 * the repository — kept as its own tiny VM so that the hub screen can render
 * without observing the full list flows it doesn't need.
 */
class JournalHubViewModel @JvmOverloads constructor(
    application: Application,
    repoOverride: TideletRepository? = null,
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    val state: StateFlow<JournalHubUiState> = combine(
        repo.thoughtRecords,
        repo.journalEntries,
        repo.eveningReviews,
    ) { thoughts, entries, eveningReviews ->
        JournalHubUiState(
            loaded = true,
            thoughtChecksCount = thoughts.size,
            journalEntriesCount = entries.size,
            eveningReviewsCount = eveningReviews.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = JournalHubUiState(loaded = false),
    )
}
