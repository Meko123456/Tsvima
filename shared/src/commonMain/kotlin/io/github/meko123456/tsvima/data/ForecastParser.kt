package io.github.meko123456.tsvima.data

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure parser for the Open-Meteo forecast response. Kept free of platform types so it
 * unit-tests on an Android host JVM and on an iOS simulator alike. Malformed input returns
 * null rather than throwing.
 *
 * Expected shape (hourly arrays are parallel and indexed by `time`):
 * ```
 * { "latitude": 41.7, "longitude": 44.8,
 *   "hourly": { "time": [...], "precipitation_probability": [...],
 *               "precipitation": [...], "temperature_2m": [...], "wind_speed_10m": [...] } }
 * ```
 */
object ForecastParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): Forecast? = runCatching {
        val dto = json.decodeFromString<ForecastDto>(body)
        val h = dto.hourly ?: return null
        val times = h.time ?: return null
        val n = times.size
        val offset = dto.utcOffsetSeconds?.let { UtcOffset(seconds = it) }
        val zone = dto.timezone?.let { runCatching { TimeZone.of(it) }.getOrNull() }
        val hourly = (0 until n).map { i ->
            // Open-Meteo stamps every hour with the one offset in force when it answered, even past
            // a clock change inside the forecast: asked on a Saturday in Berlin, Sunday afternoon
            // still comes back at summer time, an hour ahead of the clocks. The offset turns each
            // stamp back into an instant, and the place's own zone says how its clocks will read it.
            val starts = offset?.let { o -> runCatching { LocalDateTime.parse(times[i]).toInstant(o) }.getOrNull() }
            HourlyPoint(
                time = if (starts != null && zone != null) starts.toLocalDateTime(zone).toString() else times[i],
                precipProbability = h.precipProbability?.getOrNull(i)?.coerceIn(0, 100) ?: 0,
                precipMm = h.precipitation?.getOrNull(i) ?: 0.0,
                tempC = h.temperature?.getOrNull(i) ?: 0.0,
                windKmh = h.windSpeed?.getOrNull(i) ?: 0.0,
                epochSeconds = starts?.epochSeconds,
            )
        }
        Forecast(
            latitude = dto.latitude,
            longitude = dto.longitude,
            hourly = hourly,
            utcOffsetSeconds = dto.utcOffsetSeconds,
        )
    }.getOrNull()

    @Serializable
    private data class ForecastDto(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val hourly: HourlyDto? = null,
        @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
        val timezone: String? = null,
    )

    @Serializable
    private data class HourlyDto(
        val time: List<String>? = null,
        @SerialName("precipitation_probability") val precipProbability: List<Int>? = null,
        val precipitation: List<Double>? = null,
        @SerialName("temperature_2m") val temperature: List<Double>? = null,
        @SerialName("wind_speed_10m") val windSpeed: List<Double>? = null,
    )
}
