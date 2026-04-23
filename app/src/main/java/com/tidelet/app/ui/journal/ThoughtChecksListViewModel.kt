package com.tidelet.app.ui.journal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.ThoughtRecord
import com.tidelet.app.data.repo.TideletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * One distortion group in the Thought Checks list.
 *
 * [tagKey] is the stable [com.tidelet.app.ui.cbt.Distortion.key] value (null
 * for the "Untagged" pseudo-group), [entries] are the records in that group
 * ordered newest-first.
 */
data class ThoughtChecksGroup(
    val tagKey: String?,
    val entries: List<ThoughtRecord>,
) {
    val count: Int get() = entries.size
}

data class ThoughtChecksListUiState(
    val loaded: Boolean = false,
    val total: Int = 0,
    /**
     * Flat, newest-first list of every record. Mirrors exactly what the DAO
     * emits (ORDER BY createdAtEpochMillis DESC) — the Chronological view mode
     * renders this list directly.
     */
    val allEntries: List<ThoughtRecord> = emptyList(),
    val groups: List<ThoughtChecksGroup> = emptyList(),
) {
    val isEmpty: Boolean get() = loaded && total == 0
}

/**
 * ViewModel for the read-only Thought Checks list screen.
 *
 * Groups records by distortion tag, preserving newest-first order within each
 * group. Untagged records live in a trailing pseudo-group. Group order: by
 * descending count, matching the intuition that the most common pattern
 * surfaces first. Untagged always sits last.
 */
class ThoughtChecksListViewModel @JvmOverloads constructor(
    application: Application,
    repoOverride: TideletRepository? = null,
) : AndroidViewModel(application) {

    private val repo: TideletRepository = repoOverride
        ?: (application as TideletApplication).repository

    val state: StateFlow<ThoughtChecksListUiState> = repo.thoughtRecords
        .map { records -> toUiState(records) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThoughtChecksListUiState(loaded = false),
        )

    private fun toUiState(records: List<ThoughtRecord>): ThoughtChecksListUiState {
        if (records.isEmpty()) {
            return ThoughtChecksListUiState(
                loaded = true,
                total = 0,
                allEntries = emptyList(),
                groups = emptyList(),
            )
        }

        // Group preserving the record order (already newest-first from the DAO).
        val grouped: Map<String?, List<ThoughtRecord>> =
            records.groupBy { it.distortionTag?.takeIf { k -> k.isNotBlank() } }

        // Tagged groups first, ordered by descending count; ties broken alphabetically
        // on the tag key for a deterministic order.
        val tagged = grouped
            .filterKeys { it != null }
            .map { (tag, list) -> ThoughtChecksGroup(tagKey = tag, entries = list) }
            .sortedWith(
                compareByDescending<ThoughtChecksGroup> { it.count }
                    .thenBy { it.tagKey ?: "" }
            )

        val untagged = grouped[null]?.let { list ->
            listOf(ThoughtChecksGroup(tagKey = null, entries = list))
        } ?: emptyList()

        return ThoughtChecksListUiState(
            loaded = true,
            total = records.size,
            // Pass the DAO's newest-first stream through unchanged — the
            // Chronological view renders it as a single flat list.
            allEntries = records,
            groups = tagged + untagged,
        )
    }
}
