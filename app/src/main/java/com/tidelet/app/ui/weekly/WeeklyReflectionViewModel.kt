package com.tidelet.app.ui.weekly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.util.mondayOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * VM for the Weekly Reflection form.
 *
 * Design intent (see Reframe_Benchmarking.md, Tier 2):
 *   - Sunday-evening, low-pressure prompt.
 *   - Rate the week 1–5 and optionally write a sentence.
 *   - Keyed by the ISO Monday of the week so the row is upserted — writing
 *     again on Sunday replaces, not duplicates.
 *
 * We preload any existing reflection for the current week so the screen
 * doubles as an editor for "oh I want to change that rating."
 */
class WeeklyReflectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    /** ISO Monday of the week whose slot this screen writes to. */
    private val weekStart: LocalDate = mondayOf(LocalDate.now())

    private val _state = MutableStateFlow(WeeklyReflectionUiState(weekStart = weekStart))
    val state: StateFlow<WeeklyReflectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repo.findWeeklyReflection(weekStart)
            _state.update {
                it.copy(
                    loaded = true,
                    rating = existing?.rating,
                    note = existing?.note.orEmpty(),
                )
            }
        }
    }

    fun setRating(rating: Int) {
        _state.update { it.copy(rating = rating.coerceIn(1, 5)) }
    }

    fun setNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun save() {
        val s = _state.value
        val rating = s.rating ?: return  // Save button is gated on this in the UI
        viewModelScope.launch {
            repo.upsertWeeklyReflection(
                weekStart = s.weekStart,
                rating = rating,
                note = s.note.trim().takeIf { it.isNotEmpty() },
            )
            _state.update { it.copy(saved = true) }
        }
    }
}

data class WeeklyReflectionUiState(
    val weekStart: LocalDate,
    val loaded: Boolean = false,
    val rating: Int? = null,
    val note: String = "",
    val saved: Boolean = false,
) {
    /** Save button is live only when a rating is picked. */
    val canSave: Boolean get() = rating != null
}
