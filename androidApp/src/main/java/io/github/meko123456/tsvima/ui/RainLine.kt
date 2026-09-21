package io.github.meko123456.tsvima.ui

import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.Upcoming
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The wording around the shared rain rule.
 *
 * [Upcoming] decides *which* hour is the next wet one, identically on both platforms. What to call
 * it is the part that is genuinely per-app, so it lives here — out of [HomeViewModel], which needs
 * an `Application` to construct and would drag Robolectric in behind a string.
 */
internal val hourFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "2026-09-21T14:00" → "14:00", or the raw stamp if it will not parse. */
internal fun hourLabel(iso: String): String =
    runCatching { LocalDateTime.parse(iso).format(hourFmt) }.getOrDefault(iso)

internal fun nextRainLine(upcoming: List<HourlyPoint>): String {
    // "in the next 12h" was the timeline's length, not this line's: nextRain scans every upcoming
    // hour, which is up to two days. Claim the window actually checked.
    val rain = Upcoming.nextRain(upcoming) ?: return "No rain in the rest of the forecast ☀️"
    // And say which day. The first wet hour is often tomorrow's, and "around 00:00" read at
    // lunchtime looks like an hour already gone — the timeline rows below have a column to mark
    // the crossing, this line does not.
    val sameDay = upcoming.firstOrNull()?.time?.take(10) == rain.time.take(10)
    val at = if (sameDay) hourLabel(rain.time) else "tomorrow ${hourLabel(rain.time)}"
    return "Rain likely around $at (~${rain.precipProbability}%)"
}
