package io.github.meko123456.tsvima.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Pure helpers for turning a full forecast into "from now on" views. Testable on the JVM. */
object Upcoming {

    /**
     * The wall-clock time **at the forecast location** right now.
     *
     * Open-Meteo is asked for `timezone=auto`, so every hour it returns is stamped in the local time
     * of the place being forecast. Comparing those against the phone's clock is only correct while
     * the two places share an offset — and this app has a city search, so they routinely do not.
     * From Tbilisi, asking about London used to silently drop the first three or four hours of the
     * forecast and then score the remainder over a window that had not begun.
     *
     * [utcOffsetSeconds] null means a cached forecast from before that field existed; the device's
     * own zone is the right fallback, being both the old behaviour and correct for the common case
     * of looking up where you already are.
     */
    fun localNow(utcOffsetSeconds: Int?, now: Instant, deviceZone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(
            now,
            utcOffsetSeconds?.let { ZoneOffset.ofTotalSeconds(it) } ?: deviceZone,
        )

    /** Hours at or after the current hour (Open-Meteo times are the location's local time). */
    fun fromNow(hourly: List<HourlyPoint>, now: LocalDateTime): List<HourlyPoint> {
        val cutoff = now.withMinute(0).withSecond(0).withNano(0)
        return hourly.filter { point ->
            val t = runCatching { LocalDateTime.parse(point.time) }.getOrNull()
            t != null && !t.isBefore(cutoff)
        }
    }

    /** First upcoming hour that reads as "rain": probability ≥ 50% or ≥ 0.2 mm. */
    fun nextRain(upcoming: List<HourlyPoint>): HourlyPoint? =
        upcoming.firstOrNull { it.precipProbability >= 50 || it.precipMm >= 0.2 }
}
