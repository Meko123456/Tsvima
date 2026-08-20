package io.github.meko123456.tsvima.data

import java.time.LocalDateTime

/** Pure helpers for turning a full forecast into "from now on" views. Testable on the JVM. */
object Upcoming {

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
