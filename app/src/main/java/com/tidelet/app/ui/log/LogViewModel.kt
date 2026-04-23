package com.tidelet.app.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CheckIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * UI model for a single row in the Log history list.
 *
 * We translate the DB [CheckIn] into a small presentation-friendly shape so the
 * screen doesn't have to know about DB column names or parse ISO dates inline.
 */
data class LogRow(
    val date: LocalDate,
    val didDrink: Boolean,
    val drinkCount: Int?,
    val mood: Int?,
    val note: String?,
)

/**
 * Convenience wrapper the Log screen consumes in one shot. The "today" row is
 * split out so the screen can render it as its own prominent card — the rest
 * is the reverse-chron history.
 */
data class LogUiState(
    val today: LogRow? = null,
    val history: List<LogRow> = emptyList(),
)

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    val state: StateFlow<LogUiState> = repo.checkIns
        .mapToUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LogUiState(),
        )

    private fun Flow<List<CheckIn>>.mapToUiState(): Flow<LogUiState> = map { all ->
        val today = LocalDate.now()
        val rows = all.mapNotNull { it.toRowOrNull() }
        val todayRow = rows.firstOrNull { it.date == today }
        val history = rows.filter { it.date != today }
        LogUiState(today = todayRow, history = history)
    }

    private fun CheckIn.toRowOrNull(): LogRow? {
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        return LogRow(
            date = parsed,
            didDrink = didDrink,
            drinkCount = drinkCount,
            mood = mood,
            note = note?.takeIf { it.isNotBlank() },
        )
    }
}
