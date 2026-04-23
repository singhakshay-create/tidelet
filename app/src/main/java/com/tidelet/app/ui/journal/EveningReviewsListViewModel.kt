package com.tidelet.app.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.EveningReview
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Read-only list of past evening mini-reviews (PHASE_2 §9A5).
 *
 * Each row carries everything the user wrote — both fields are short by
 * design — so we don't need a separate detail screen.
 */
data class EveningReviewsListUiState(
    val loaded: Boolean = false,
    val entries: List<EveningReview> = emptyList(),
) {
    val isEmpty: Boolean get() = loaded && entries.isEmpty()
}

class EveningReviewsListViewModel @JvmOverloads constructor(
    application: Application,
    repoOverride: TideletRepository? = null,
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    val state: StateFlow<EveningReviewsListUiState> = repo.eveningReviews
        // DAO already orders by dateIso DESC — pass through.
        .map { rows -> EveningReviewsListUiState(loaded = true, entries = rows) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = EveningReviewsListUiState(loaded = false),
        )
}
