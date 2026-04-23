package com.tidelet.app.ui.log

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
import java.time.Clock
import java.time.LocalDate

/**
 * State for the Daily Check-in form.
 *
 * `date` is kept as a [LocalDate] (not a string) inside the VM so that the screen
 * never has to know about the ISO format. We only stringify when we hit the DB.
 *
 * `drinkCount` is nullable because "Yes, I drank" with no count set is a valid
 * state while the user is still tapping the stepper.
 */
data class CheckInUiState(
    val date: LocalDate = LocalDate.now(),
    val loaded: Boolean = false,
    val didDrink: Boolean? = null,
    val drinkCount: Int = 1,
    val mood: Int? = null,
    /**
     * Optional tag for what was happening that led to drinking. One of
     * [com.tidelet.app.data.db.CheckInTrigger]'s keys, or null.
     * Only meaningful when `didDrink = true`.
     */
    val trigger: String? = null,
    val note: String = "",
    val saved: Boolean = false,
) {
    /** The user has made enough choices that Save should be live. */
    val canSave: Boolean get() = didDrink != null
}

/**
 * ViewModel for the Daily Check-in form.
 *
 * Reads the {date} nav argument out of the SavedStateHandle, then loads any
 * existing CheckIn row for that date so the user can edit retroactively.
 *
 * We don't use the default `AndroidViewModel(application)` one-arg constructor
 * because we also want a [SavedStateHandle] — Compose's `viewModel()` can
 * inject SavedStateHandle when we provide a factory. See [Factory] below.
 */
class CheckInViewModel @JvmOverloads constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository

    /** From the nav route `log/checkin/{date}` — ISO-format ("YYYY-MM-DD"). */
    private val date: LocalDate = parseDateOrToday(savedStateHandle.get<String>("date"))

    private val _state = MutableStateFlow(CheckInUiState(date = date))
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = repo.findCheckIn(date)
            _state.update {
                if (existing != null) {
                    it.copy(
                        loaded = true,
                        didDrink = existing.didDrink,
                        drinkCount = existing.drinkCount ?: 1,
                        mood = existing.mood,
                        trigger = existing.trigger,
                        note = existing.note.orEmpty(),
                    )
                } else {
                    it.copy(loaded = true)
                }
            }
        }
    }

    fun setDidDrink(didDrink: Boolean) {
        _state.update { it.copy(didDrink = didDrink) }
    }

    fun decrementDrinkCount() {
        _state.update { it.copy(drinkCount = (it.drinkCount - 1).coerceAtLeast(1)) }
    }

    fun incrementDrinkCount() {
        // 30 is a generous ceiling — past that we're beyond "standard drinks" as a
        // useful concept anyway.
        _state.update { it.copy(drinkCount = (it.drinkCount + 1).coerceAtMost(30)) }
    }

    fun setMood(mood: Int) {
        _state.update { it.copy(mood = mood) }
    }

    /** Tap a chip to set the trigger; tap the same chip again to clear. */
    fun toggleTrigger(trigger: String) {
        _state.update { it.copy(trigger = if (it.trigger == trigger) null else trigger) }
    }

    fun setNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun save() {
        val s = _state.value
        val didDrink = s.didDrink ?: return  // guarded by canSave in the UI
        viewModelScope.launch {
            repo.upsertCheckIn(
                date = s.date,
                didDrink = didDrink,
                drinkCount = if (didDrink) s.drinkCount else null,
                mood = s.mood,
                // trigger only makes sense when the user drank; drop it otherwise
                // so the DB doesn't carry a stale value from a user who toggled
                // Yes → No after picking a trigger.
                trigger = if (didDrink) s.trigger else null,
                note = s.note.trim().takeIf { it.isNotEmpty() },
            )
            _state.update { it.copy(saved = true) }
        }
    }

    private fun parseDateOrToday(raw: String?): LocalDate =
        try {
            if (raw.isNullOrBlank()) LocalDate.now(clock) else LocalDate.parse(raw)
        } catch (_: Exception) {
            LocalDate.now(clock)
        }

    companion object {
        /**
         * Custom factory so we can receive both the Application and the
         * SavedStateHandle (which `viewModel()` will supply from CreationExtras).
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val handle = this.createSavedStateHandle()
                CheckInViewModel(app, handle)
            }
        }
    }
}
