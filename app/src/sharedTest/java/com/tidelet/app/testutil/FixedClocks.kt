package com.tidelet.app.testutil

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Factories for deterministic clocks. Avoids scattering `Clock.fixed(...)`
 * incantations across every test.
 */
object FixedClocks {
    val UTC: ZoneId = ZoneId.of("UTC")

    fun at(instant: Instant, zone: ZoneId = UTC): Clock = Clock.fixed(instant, zone)

    fun atIso(iso: String, zone: ZoneId = UTC): Clock = Clock.fixed(Instant.parse(iso), zone)

    /** Clock whose "today" in [zone] is [date] at local midnight. */
    fun atLocalMidnight(date: LocalDate, zone: ZoneId = UTC): Clock =
        Clock.fixed(date.atStartOfDay(zone).toInstant(), zone)
}
