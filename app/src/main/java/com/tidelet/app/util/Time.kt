package com.tidelet.app.util

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Small helpers for working with streak math.
 * Kept free of Android imports so it's easy to unit-test.
 */

data class StreakDuration(
    val days: Long,
    val hours: Long,
    val minutes: Long,
) {
    val isSameDay: Boolean get() = days == 0L
    val isWithinFirstDay: Boolean get() = days < 1
}

fun streakFromStartDate(
    startDate: LocalDate,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): StreakDuration {
    // The streak begins at midnight local time on the chosen start date.
    val start = startDate.atStartOfDay(zone)
    val current = now.atZone(zone)
    if (!current.isAfter(start)) return StreakDuration(0, 0, 0)
    // Count calendar days in local time, not elapsed 24h blocks: a DST
    // spring-forward day is only 23 hours long, so Duration.toDays() would
    // under-count the streak by one until the clocks fall back.
    val days = ChronoUnit.DAYS.between(start, current)
    val remainder = Duration.between(start.plusDays(days), current)
    return StreakDuration(
        days = days,
        hours = remainder.toHours() % 24,
        minutes = remainder.toMinutes() % 60,
    )
}

/** The ordered list of milestones (in days) the app celebrates. */
val MILESTONES_DAYS: List<Int> = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)

data class NextMilestone(val days: Int, val daysRemaining: Int)

fun nextMilestoneFor(currentDays: Long): NextMilestone? {
    val next = MILESTONES_DAYS.firstOrNull { it > currentDays } ?: return null
    return NextMilestone(days = next, daysRemaining = (next - currentDays).toInt())
}

fun milestoneLabel(days: Int): String = when (days) {
    1 -> "24 hours"
    3 -> "3 days"
    7 -> "1 week"
    14 -> "2 weeks"
    30 -> "30 days"
    60 -> "60 days"
    90 -> "90 days"
    180 -> "6 months"
    365 -> "1 year"
    else -> "$days days"
}

fun todayLocal(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    LocalDateTime.now(zone).toLocalDate()

/**
 * ISO Monday of the week containing [date]. Weekly reflections key by this
 * date so any write during Mon–Sun upserts the single slot for that week,
 * regardless of what day the user happens to open the screen.
 *
 * If [date] is already a Monday, returns [date] unchanged.
 */
fun mondayOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
