package com.tidelet.app.ui.settings

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tidelet.app.TideletApplication
import com.tidelet.app.data.export.ExportImportManager
import com.tidelet.app.data.export.ImportReport
import com.tidelet.app.data.prefs.UserProfile
import com.tidelet.app.widget.StreakWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * VM behind the Settings screen.
 *
 * Most of its work is shuttling URIs from the Storage Access Framework
 * pickers (see [SettingsScreen]) through the [ExportImportManager]. The
 * actual codec work happens in pure Kotlin in [com.tidelet.app.data.export];
 * this class is only concerned with Android glue (content resolver I/O +
 * coroutine dispatching) and surfacing status to the UI.
 *
 * Delete-all-data also lives here because it's a Settings-only action.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TideletApplication).repository
    private val manager = ExportImportManager(repo)

    private val _actions = MutableStateFlow(SettingsUiState())

    /**
     * Settings state combines one-shot actions (export/import/wipe progress)
     * with the live profile flow, so the Drinking-baseline section always
     * reflects the current DataStore values without extra plumbing.
     */
    val state: StateFlow<SettingsUiState> = combine(_actions, repo.profile) { actions, profile ->
        actions.copy(profile = profile)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState(),
    )

    /**
     * Write the full export to [uri] on the IO dispatcher. The caller is
     * expected to have opened the destination via a CreateDocument contract
     * (which is why we take a URI rather than a File path).
     */
    fun exportTo(contentResolver: ContentResolver, uri: Uri) {
        _actions.update { it.copy(busy = true, lastAction = LastAction.Exporting, message = null) }
        viewModelScope.launch {
            val outcome = runCatching {
                val markdown = manager.buildExport()
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(markdown.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Could not open output stream")
                }
                markdown.length
            }
            _actions.update {
                it.copy(
                    busy = false,
                    lastAction = LastAction.Exported,
                    exportedByteCount = outcome.getOrNull(),
                    message = outcome.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun importFrom(contentResolver: ContentResolver, uri: Uri) {
        _actions.update { it.copy(busy = true, lastAction = LastAction.Importing, message = null) }
        viewModelScope.launch {
            val outcome = runCatching {
                val markdown = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("Could not open input stream")
                }
                manager.applyImport(markdown)
            }
            _actions.update {
                it.copy(
                    busy = false,
                    lastAction = LastAction.Imported,
                    lastImportReport = outcome.getOrNull(),
                    message = outcome.exceptionOrNull()?.localizedMessage,
                )
            }
            // Import may have restored a different start date — nudge the
            // widget so the user's home screen matches their imported data.
            StreakWidgetUpdater.refreshAll(getApplication())
        }
    }

    /**
     * In-process self-test: build an export, re-parse it, and compare the
     * round-tripped row counts against the current DB. Never touches live
     * data. See PHASE_2_SPEC.md §2.
     */
    fun verifyBackup() {
        _actions.update { it.copy(busy = true, lastAction = LastAction.Verifying, message = null) }
        viewModelScope.launch {
            val outcome = runCatching {
                val markdown = manager.buildExport()
                val parsed = com.tidelet.app.data.export.TideletMarkdown.decode(markdown)
                val live = VerifyCounts(
                    checkIns = repo.checkIns.first().size,
                    cravingEvents = repo.cravingEvents.first().size,
                    reasons = repo.reasons.first().size,
                    journalEntries = repo.journalEntries.first().size,
                    weeklyReflections = repo.weeklyReflections.first().size,
                    thoughtRecords = repo.thoughtRecords.first().size,
                    functionalAnalyses = repo.functionalAnalyses.first().size,
                    refusalPhrases = repo.refusalPhrases.first().size,
                    milestoneLetters = repo.milestoneLetters.first().size,
                    toolOpenCounts = repo.toolOpenCounts.first().size,
                    eveningReviews = repo.eveningReviews.first().size,
                )
                val roundTripped = VerifyCounts(
                    checkIns = parsed.checkIns.size,
                    cravingEvents = parsed.cravingEvents.size,
                    reasons = parsed.reasons.size,
                    journalEntries = parsed.journalEntries.size,
                    weeklyReflections = parsed.weeklyReflections.size,
                    thoughtRecords = parsed.thoughtRecords.size,
                    functionalAnalyses = parsed.functionalAnalyses.size,
                    refusalPhrases = parsed.refusalPhrases.size,
                    milestoneLetters = parsed.milestoneLetters.size,
                    toolOpenCounts = parsed.toolOpenCounts.size,
                    eveningReviews = parsed.eveningReviews.size,
                )
                VerifyReport(
                    matched = (live == roundTripped),
                    total = live.total(),
                    roundTripped = roundTripped.total(),
                )
            }
            _actions.update {
                it.copy(
                    busy = false,
                    lastAction = LastAction.Verified,
                    verifyReport = outcome.getOrNull(),
                    message = outcome.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun wipeAllData() {
        _actions.update { it.copy(busy = true, lastAction = LastAction.Wiping, message = null) }
        viewModelScope.launch {
            repo.wipeAll()
            _actions.update {
                it.copy(
                    busy = false,
                    lastAction = LastAction.Wiped,
                    exportedByteCount = null,
                    lastImportReport = null,
                )
            }
            // After wipe, the start date is gone; refresh so the widget shows
            // "Start your streak" instead of a stale day count.
            StreakWidgetUpdater.refreshAll(getApplication())
        }
    }

    fun dismissMessage() {
        _actions.update { it.copy(message = null, lastAction = LastAction.Idle) }
    }

    /**
     * Update the user's drinking baseline (typical drinks/day and price per
     * drink in cents). Clamps sane limits before writing to DataStore.
     *
     * Called from the "Drinking baseline" section in Settings. These values
     * feed the money-saved estimate on Stats.
     */
    fun updateBaseline(drinksPerDay: Int, priceCents: Int, hoursPerDrink: Int) {
        val clampedDrinks = drinksPerDay.coerceIn(1, 30)
        val clampedPrice = priceCents.coerceIn(0, 1_000_000)
        val clampedHours = hoursPerDrink.coerceIn(0, 24)
        viewModelScope.launch {
            repo.restoreProfile(
                startDateEpochDay = null,
                typicalDrinksPerDay = clampedDrinks,
                priceCentsPerDrink = clampedPrice,
                checkInReminderEnabled = null,
                checkInReminderHour = null,
                checkInReminderMinute = null,
                hoursPerDrink = clampedHours,
            )
        }
    }

    /**
     * Toggle the optional evening mini-review prompt (PHASE_2 §9A5). Stored
     * via [TideletRepository.restoreProfile] so the same import path that
     * restores the rest of the profile naturally restores this flag too.
     */
    fun setEveningReviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.restoreProfile(
                startDateEpochDay = null,
                typicalDrinksPerDay = null,
                priceCentsPerDrink = null,
                checkInReminderEnabled = null,
                checkInReminderHour = null,
                checkInReminderMinute = null,
                eveningReviewEnabled = enabled,
            )
        }
    }

    /**
     * Toggle the optional visual companion (PHASE_2 §9B1). Off by default;
     * opt-in via Settings.
     */
    fun setCompanionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.restoreProfile(
                startDateEpochDay = null,
                typicalDrinksPerDay = null,
                priceCentsPerDrink = null,
                checkInReminderEnabled = null,
                checkInReminderHour = null,
                checkInReminderMinute = null,
                companionEnabled = enabled,
            )
        }
    }
}

data class SettingsUiState(
    val busy: Boolean = false,
    val lastAction: LastAction = LastAction.Idle,
    /** Byte count of the most recent export, if any — handy for a tiny confirmation. */
    val exportedByteCount: Int? = null,
    val lastImportReport: ImportReport? = null,
    val message: String? = null,
    /** Live profile so Settings can render current baseline values. */
    val profile: UserProfile? = null,
    /** Most recent round-trip verification. Null until the user has ever tried. */
    val verifyReport: VerifyReport? = null,
)

/** Track what was just kicked off so the UI can show an appropriate confirmation. */
enum class LastAction {
    Idle, Exporting, Exported, Importing, Imported, Wiping, Wiped, Verifying, Verified
}

/** Row-count snapshot used by the self-test. */
private data class VerifyCounts(
    val checkIns: Int,
    val cravingEvents: Int,
    val reasons: Int,
    val journalEntries: Int,
    val weeklyReflections: Int,
    val thoughtRecords: Int,
    val functionalAnalyses: Int,
    val refusalPhrases: Int,
    val milestoneLetters: Int = 0,
    val toolOpenCounts: Int = 0,
    val eveningReviews: Int = 0,
) {
    fun total(): Int = checkIns + cravingEvents + reasons + journalEntries +
        weeklyReflections + thoughtRecords + functionalAnalyses + refusalPhrases +
        milestoneLetters + toolOpenCounts + eveningReviews
}

/** Public report surfaced by [SettingsViewModel.verifyBackup]. */
data class VerifyReport(val matched: Boolean, val total: Int, val roundTripped: Int)
