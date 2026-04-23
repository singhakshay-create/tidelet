package com.tidelet.app.ui.home

import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.ThoughtRecord
import java.time.LocalDate
import java.time.Period

/**
 * A past entry Home can surface as an "on this day" anniversary card.
 *
 * The card shows when a [ThoughtRecord] or [JournalEntry] exists whose
 * `dateIso` falls on the same day-of-month as today and at least one calendar
 * month in the past.
 *
 * Sealed because the card renders differently for the two types (different
 * labels, different detail route) and we want the Home layer to pattern-match
 * rather than sniff types.
 */
sealed class OnThisDayItem {
    abstract val date: LocalDate
    abstract val preview: String
    /** Whole months between [date] and the observing "today". */
    abstract val monthsAgo: Int

    data class Thought(
        val record: ThoughtRecord,
        override val date: LocalDate,
        override val monthsAgo: Int,
    ) : OnThisDayItem() {
        override val preview: String
            get() = record.thought.ifBlank { record.situation }
    }

    data class Journal(
        val entry: JournalEntry,
        override val date: LocalDate,
        override val monthsAgo: Int,
    ) : OnThisDayItem() {
        override val preview: String
            get() = entry.text.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                ?: entry.text
    }
}

/**
 * Pick the oldest eligible past entry to surface on Home as "on this day."
 *
 * Eligibility:
 *   * Same `dayOfMonth` as [today]
 *   * Strictly in a past calendar month (i.e. `entryDate < today.withDayOfMonth(1)`)
 *   * `dateIso` parseable as an ISO `LocalDate`
 *
 * Among eligible entries, we pick the *oldest* — the anniversary that has
 * compounded the most. Ties are broken by `createdAtEpochMillis` ascending,
 * then by a stable type preference (Thought before Journal) so tests are
 * deterministic.
 */
fun computeOnThisDay(
    thoughts: List<ThoughtRecord>,
    entries: List<JournalEntry>,
    today: LocalDate,
): OnThisDayItem? {
    val firstOfThisMonth = today.withDayOfMonth(1)

    val candidates = buildList<OnThisDayItem> {
        thoughts.forEach { r ->
            val d = parseOrNull(r.dateIso) ?: return@forEach
            if (d.dayOfMonth == today.dayOfMonth && d.isBefore(firstOfThisMonth)) {
                add(OnThisDayItem.Thought(r, d, monthsBetween(d, today)))
            }
        }
        entries.forEach { e ->
            val d = parseOrNull(e.dateIso) ?: return@forEach
            if (d.dayOfMonth == today.dayOfMonth && d.isBefore(firstOfThisMonth)) {
                add(OnThisDayItem.Journal(e, d, monthsBetween(d, today)))
            }
        }
    }

    return candidates.minWithOrNull(
        compareBy<OnThisDayItem> { it.date }
            .thenBy { createdAtOf(it) }
            .thenBy { if (it is OnThisDayItem.Thought) 0 else 1 }
    )
}

private fun createdAtOf(item: OnThisDayItem): Long = when (item) {
    is OnThisDayItem.Thought -> item.record.createdAtEpochMillis
    is OnThisDayItem.Journal -> item.entry.createdAtEpochMillis
}

private fun parseOrNull(iso: String): LocalDate? = try {
    LocalDate.parse(iso)
} catch (_: Exception) {
    null
}

private fun monthsBetween(earlier: LocalDate, later: LocalDate): Int {
    val period = Period.between(earlier, later)
    return period.years * 12 + period.months
}
