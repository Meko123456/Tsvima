package io.github.meko123456.tsvima.data

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Turning a full forecast into the part that is still ahead of you.
 *
 * Shared rather than Android-only, and that move is the point. This holds two rules — which hours
 * count as upcoming, and what counts as rain — and both were written against `java.time` in the
 * Android app. The iOS app could therefore either go without a next-rain line or restate the
 * thresholds in Swift, and a rule stated twice is a rule that will eventually disagree with itself.
 *
 * The public surface deliberately takes and returns no date type. Both apps want the same thing —
 * "the hours I could still step outside in" — and neither wants a `LocalDateTime` to get it, so the
 * calendar arithmetic stays inside and nothing about it has to be expressed in Swift or in
 * `java.time`.
 */
object Upcoming {

    /** Probability at or above which an hour reads as rain. */
    const val RAIN_PROBABILITY_PERCENT: Int = 50

    /** Millimetres at or above which an hour reads as rain, whatever the probability says. */
    const val RAIN_MILLIMETRES: Double = 0.2

    /**
     * Hours at or after the current hour **at the forecast location**, read at [nowEpochSeconds].
     *
     * Open-Meteo is asked for `timezone=auto`, so every hour it returns is stamped in the local time
     * of the place being forecast. Comparing those against the phone's clock is only correct while
     * the two places share an offset — and this app has a city search, so they routinely do not.
     * From Tbilisi, asking about London used to silently drop the first three or four hours of the
     * forecast and then score the remainder over a window that had not begun.
     *
     * [utcOffsetSeconds] null means a cached forecast written before that field existed; the
     * device's own offset is the right fallback, being both the old behaviour and correct for the
     * common case of looking up where you already are. Both offsets are plain second counts rather
     * than platform zone objects, so nothing platform-shaped has to cross the boundary.
     *
     * Every argument is passed in, which is what makes the timezone cases above testable at all;
     * [hoursAheadNow] is the same call against the real clock.
     */
    fun hoursAhead(
        hourly: List<HourlyPoint>,
        nowEpochSeconds: Long,
        utcOffsetSeconds: Int?,
        deviceOffsetSeconds: Int,
    ): List<HourlyPoint> = fromNow(hourly, localNow(utcOffsetSeconds, nowEpochSeconds, deviceOffsetSeconds))

    /**
     * [hoursAhead] against the system clock and this device's current offset.
     *
     * The one impure entry point, and it is shared too: reading the clock is the same job on both
     * platforms, so neither app needs its own `java.time` or `DateFormatter` code to do it.
     */
    fun hoursAheadNow(hourly: List<HourlyPoint>, utcOffsetSeconds: Int?): List<HourlyPoint> {
        val now = Clock.System.now()
        return hoursAhead(
            hourly = hourly,
            nowEpochSeconds = now.epochSeconds,
            utcOffsetSeconds = utcOffsetSeconds,
            deviceOffsetSeconds = TimeZone.currentSystemDefault().offsetAt(now).totalSeconds,
        )
    }

    /** First upcoming hour that reads as rain, or null if none does. */
    fun nextRain(upcoming: List<HourlyPoint>): HourlyPoint? = upcoming.firstOrNull {
        it.precipProbability >= RAIN_PROBABILITY_PERCENT || it.precipMm >= RAIN_MILLIMETRES
    }

    /** The wall-clock time at the forecast location. Internal: see the note on the class. */
    internal fun localNow(utcOffsetSeconds: Int?, nowEpochSeconds: Long, deviceOffsetSeconds: Int): LocalDateTime =
        Instant.fromEpochSeconds(nowEpochSeconds)
            .toLocalDateTime(UtcOffset(seconds = utcOffsetSeconds ?: deviceOffsetSeconds).asTimeZone())

    /** Hours at or after [now]'s hour. Open-Meteo times are the location's local time. */
    internal fun fromNow(hourly: List<HourlyPoint>, now: LocalDateTime): List<HourlyPoint> {
        // The hour you are *in* still counts: it is the one you would step outside during.
        val cutoff = LocalDateTime(now.year, now.month, now.day, now.hour, 0)
        return hourly.filter { point ->
            val at = runCatching { LocalDateTime.parse(point.time) }.getOrNull()
            at != null && at >= cutoff
        }
    }
}
