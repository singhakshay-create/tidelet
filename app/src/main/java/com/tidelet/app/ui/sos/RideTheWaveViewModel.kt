package com.tidelet.app.ui.sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.db.CravingOutcome
import com.tidelet.app.data.db.SosToolKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

enum class WavePhase { Running, Done }

data class WaveUiState(
    val phase: WavePhase = WavePhase.Running,
    val totalSeconds: Int = 15 * 60,
    val remainingSeconds: Int = 15 * 60,
    val soundscape: Soundscape = Soundscape.Silent,
    /**
     * After a non-DRANK outcome we flip this to true so the screen can
     * offer the in-line "map what happened?" panel. DRANK does not trigger
     * this — that path goes to CompassionateReset where the analysis lives
     * as part of the reset flow.
     */
    val showAnalysisPanel: Boolean = false,
    val analysisSaved: Boolean = false,
    /** Craving-event id for the outcome just logged — used to link the analysis. */
    val lastCravingEventId: Long? = null,
)

class RideTheWaveViewModel @JvmOverloads constructor(
    application: Application,
    private val clock: Clock = Clock.systemDefaultZone(),
) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository
    private val soundscapePlayer = SoundscapePlayer(application.applicationContext)

    private val _state = MutableStateFlow(WaveUiState())
    val state: StateFlow<WaveUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch { repo.recordToolOpen(SosToolKey.RIDE_THE_WAVE) }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.remainingSeconds > 0) {
                delay(1_000L)
                _state.value = _state.value.copy(
                    remainingSeconds = (_state.value.remainingSeconds - 1).coerceAtLeast(0),
                )
            }
            // Timer finished: advance phase and drop audio — the outcome
            // question should be quiet.
            soundscapePlayer.play(Soundscape.Silent)
            _state.value = _state.value.copy(
                phase = WavePhase.Done,
                soundscape = Soundscape.Silent,
            )
        }
    }

    fun endEarly() {
        timerJob?.cancel()
        soundscapePlayer.play(Soundscape.Silent)
        _state.value = _state.value.copy(
            phase = WavePhase.Done,
            soundscape = Soundscape.Silent,
        )
    }

    fun selectSoundscape(soundscape: Soundscape) {
        _state.value = _state.value.copy(soundscape = soundscape)
        soundscapePlayer.play(soundscape)
    }

    /**
     * Log an outcome. For non-DRANK outcomes we also flip
     * [WaveUiState.showAnalysisPanel] so the screen can offer the optional
     * in-line analysis. DRANK does NOT open the panel — the analysis lives
     * on the Compassionate Reset screen instead (see class doc).
     */
    fun logOutcome(outcome: String) {
        viewModelScope.launch {
            val id = repo.logCravingEventReturningId(
                tool = SosToolKey.RIDE_THE_WAVE,
                outcome = outcome,
            )
            _state.update {
                it.copy(
                    lastCravingEventId = id,
                    showAnalysisPanel = outcome != CravingOutcome.DRANK,
                )
            }
        }
    }

    fun logGotThrough() = logOutcome(CravingOutcome.GOT_THROUGH)
    fun logStillStruggling() = logOutcome(CravingOutcome.STILL_STRUGGLING)
    fun logDrank() = logOutcome(CravingOutcome.DRANK)

    /** Save the analysis (if at least one field is filled) and dismiss the panel. */
    fun saveAnalysis(antecedent: String?, thought: String?, followingAction: String?) {
        val cravingId = _state.value.lastCravingEventId
        viewModelScope.launch {
            repo.addFunctionalAnalysis(
                date = LocalDate.now(clock),
                cravingEventId = cravingId,
                antecedent = antecedent,
                thoughtAtMoment = thought,
                followingAction = followingAction,
            )
            _state.update { it.copy(showAnalysisPanel = false, analysisSaved = true) }
        }
    }

    fun dismissAnalysis() {
        _state.update { it.copy(showAnalysisPanel = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundscapePlayer.release()
    }
}
