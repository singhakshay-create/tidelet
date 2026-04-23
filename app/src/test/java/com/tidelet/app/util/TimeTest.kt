package com.tidelet.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimeTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    // ---- streakFromStartDate ----

    @Test
    fun `streakFromStartDate returns zero when now equals startDate midnight`() {
        val start = LocalDate.of(2026, 4, 18)
        val now = start.atStartOfDay(utc).toInstant()

        val result = streakFromStartDate(start, now = now, zone = utc)

        assertThat(result).isEqualTo(StreakDuration(0, 0, 0))
        assertThat(result.isSameDay).isTrue()
    }

    @Test
    fun `streakFromStartDate counts whole days across DST boundary`() {
        // America/Los_Angeles sprang forward on 2026-03-08 at 02:00 local.
        val la = ZoneId.of("America/Los_Angeles")
        val start = LocalDate.of(2026, 3, 7)
        // Midnight local seven days later, across the DST boundary.
        val now = LocalDate.of(2026, 3, 14).atStartOfDay(la).toInstant()

        val result = streakFromStartDate(start, now = now, zone = la)

        assertThat(result.days).isEqualTo(7L)
    }

    @Test
    fun `streakFromStartDate never returns negative when now is before startDate`() {
        val start = LocalDate.of(2026, 4, 18)
        val now = start.atStartOfDay(utc).minus(Duration.ofHours(5)).toInstant()

        val result = streakFromStartDate(start, now = now, zone = utc)

        assertThat(result.days).isEqualTo(0L)
        assertThat(result.hours).isEqualTo(0L)
        assertThat(result.minutes).isEqualTo(0L)
    }

    @Test
    fun `streakFromStartDate splits hours and minutes modulo 24 and 60`() {
        val start = LocalDate.of(2026, 4, 18)
        // 2 days, 5 hours, 37 minutes after midnight UTC.
        val now = start.atStartOfDay(utc)
            .plusDays(2).plusHours(5).plusMinutes(37)
            .toInstant()

        val result = streakFromStartDate(start, now = now, zone = utc)

        assertThat(result.days).isEqualTo(2L)
        assertThat(result.hours).isEqualTo(5L)
        assertThat(result.minutes).isEqualTo(37L)
    }

    // ---- nextMilestoneFor ----

    @Test
    fun `nextMilestoneFor returns day-1 when current is zero`() {
        val next = nextMilestoneFor(currentDays = 0L)

        assertThat(next).isEqualTo(NextMilestone(days = 1, daysRemaining = 1))
    }

    @Test
    fun `nextMilestoneFor returns 365 when current is 364`() {
        val next = nextMilestoneFor(currentDays = 364L)

        assertThat(next).isEqualTo(NextMilestone(days = 365, daysRemaining = 1))
    }

    @Test
    fun `nextMilestoneFor returns null past 365 days`() {
        val next = nextMilestoneFor(currentDays = 365L)

        assertThat(next).isNull()
    }

    @Test
    fun `nextMilestoneFor picks the next milestone strictly greater than current`() {
        // At exactly 7 days, the next milestone is 14 — not 7 itself.
        val next = nextMilestoneFor(currentDays = 7L)

        assertThat(next?.days).isEqualTo(14)
        assertThat(next?.daysRemaining).isEqualTo(7)
    }

    // ---- milestoneLabel ----

    @Test
    fun `milestoneLabel produces human strings for every canonical milestone`() {
        val labels = MILESTONES_DAYS.associateWith(::milestoneLabel)

        assertThat(labels[1]).isEqualTo("24 hours")
        assertThat(labels[3]).isEqualTo("3 days")
        assertThat(labels[7]).isEqualTo("1 week")
        assertThat(labels[14]).isEqualTo("2 weeks")
        assertThat(labels[30]).isEqualTo("30 days")
        assertThat(labels[60]).isEqualTo("60 days")
        assertThat(labels[90]).isEqualTo("90 days")
        assertThat(labels[180]).isEqualTo("6 months")
        assertThat(labels[365]).isEqualTo("1 year")
    }

    @Test
    fun `milestoneLabel falls back to 'N days' for a non-canonical day count`() {
        assertThat(milestoneLabel(days = 42)).isEqualTo("42 days")
    }

    // ---- mondayOf ----

    @Test
    fun `mondayOf returns same date when input is a Monday`() {
        val monday = LocalDate.of(2026, 4, 13)
        assertThat(monday.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)

        assertThat(mondayOf(monday)).isEqualTo(monday)
    }

    @Test
    fun `mondayOf returns previous Monday for any weekday`() {
        // 2026-04-18 is a Saturday; the Monday of its week is 2026-04-13.
        val saturday = LocalDate.of(2026, 4, 18)

        assertThat(mondayOf(saturday)).isEqualTo(LocalDate.of(2026, 4, 13))
    }

    @Test
    fun `mondayOf returns previous Monday when input is a Sunday`() {
        val sunday = LocalDate.of(2026, 4, 19)
        assertThat(sunday.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)

        assertThat(mondayOf(sunday)).isEqualTo(LocalDate.of(2026, 4, 13))
    }

    @Suppress("unused")
    private val unusedInstantSeed: Instant = Instant.parse("2026-04-18T00:00:00Z")
}
