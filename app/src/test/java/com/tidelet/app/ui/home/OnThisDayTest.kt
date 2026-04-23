package com.tidelet.app.ui.home

import com.google.common.truth.Truth.assertThat
import com.tidelet.app.data.db.JournalEntry
import com.tidelet.app.data.db.ThoughtRecord
import org.junit.Test
import java.time.LocalDate

/**
 * Pure-JVM tests for the "on this day" picker. No coroutines, no Robolectric
 * — just the [computeOnThisDay] function against seed data.
 */
class OnThisDayTest {

    private val today = LocalDate.of(2026, 4, 19)

    private fun thought(id: Long, dateIso: String, createdAt: Long = id) = ThoughtRecord(
        id = id,
        dateIso = dateIso,
        situation = "s",
        thought = "t$id",
        challenge = "c",
        friendReframe = "f",
        createdAtEpochMillis = createdAt,
    )

    private fun journal(id: Long, dateIso: String, text: String = "j$id", createdAt: Long = id) =
        JournalEntry(
            id = id,
            dateIso = dateIso,
            text = text,
            createdAtEpochMillis = createdAt,
        )

    @Test
    fun `returns null when nothing matches the day-of-month`() {
        val result = computeOnThisDay(
            thoughts = listOf(thought(1, "2025-03-18")),
            entries = listOf(journal(2, "2024-05-20")),
            today = today,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `returns null when same day-of-month but same calendar month`() {
        // April 19th in the same year/month as today — not an anniversary.
        val result = computeOnThisDay(
            thoughts = listOf(thought(1, "2026-04-19")),
            entries = emptyList(),
            today = today,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `picks the oldest eligible thought record`() {
        val result = computeOnThisDay(
            thoughts = listOf(
                thought(1, "2026-03-19"),  // 1 month ago
                thought(2, "2025-04-19"),  // 12 months ago — oldest
                thought(3, "2025-11-19"),  // 5 months ago
            ),
            entries = emptyList(),
            today = today,
        )

        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(OnThisDayItem.Thought::class.java)
        val t = result as OnThisDayItem.Thought
        assertThat(t.record.id).isEqualTo(2L)
        assertThat(t.monthsAgo).isEqualTo(12)
    }

    @Test
    fun `picks oldest across both thought and journal pools`() {
        val result = computeOnThisDay(
            thoughts = listOf(thought(1, "2025-06-19")), // ~10 months ago
            entries = listOf(journal(2, "2024-12-19")),  // 16 months ago — oldest
            today = today,
        )

        assertThat(result).isInstanceOf(OnThisDayItem.Journal::class.java)
        val j = result as OnThisDayItem.Journal
        assertThat(j.entry.id).isEqualTo(2L)
    }

    @Test
    fun `ignores entries with unparseable dateIso`() {
        val result = computeOnThisDay(
            thoughts = listOf(
                thought(1, "not-a-date"),
                thought(2, "2025-04-19"),
            ),
            entries = emptyList(),
            today = today,
        )
        assertThat(result).isNotNull()
        assertThat((result as OnThisDayItem.Thought).record.id).isEqualTo(2L)
    }

    @Test
    fun `deterministic when dates tie - older createdAt wins, then thought over journal`() {
        val thought = thought(10, "2025-04-19", createdAt = 500L)
        val journal = journal(20, "2025-04-19", createdAt = 500L)
        val result = computeOnThisDay(
            thoughts = listOf(thought),
            entries = listOf(journal),
            today = today,
        )

        // Equal date + equal createdAt → Thought wins by type-preference tiebreak.
        assertThat(result).isInstanceOf(OnThisDayItem.Thought::class.java)
    }

    @Test
    fun `monthsAgo counts calendar months correctly`() {
        val result = computeOnThisDay(
            thoughts = listOf(thought(1, "2026-02-19")),
            entries = emptyList(),
            today = today, // 2026-04-19
        )
        assertThat((result as OnThisDayItem.Thought).monthsAgo).isEqualTo(2)
    }

    @Test
    fun `handles cross-year anniversary`() {
        val lastYearSameDay = today.minusYears(1)
        val result = computeOnThisDay(
            thoughts = listOf(thought(1, lastYearSameDay.toString())),
            entries = emptyList(),
            today = today,
        )
        assertThat((result as OnThisDayItem.Thought).monthsAgo).isEqualTo(12)
    }

    @Test
    fun `preview trims leading blank lines on journal entries`() {
        val result = computeOnThisDay(
            thoughts = emptyList(),
            entries = listOf(journal(1, "2025-04-19", text = "\n\n  First real line.\nAnd more.")),
            today = today,
        )
        val j = result as OnThisDayItem.Journal
        assertThat(j.preview).isEqualTo("First real line.")
    }
}
