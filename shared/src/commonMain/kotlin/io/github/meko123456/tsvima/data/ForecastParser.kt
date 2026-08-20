package io.github.meko123456.tsvima.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure parser for the Open-Meteo forecast response. Kept free of Android types so it
 * unit-tests on the JVM. Malformed input returns null rather than throwing.
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
        val hourly = (0 until n).map { i ->
            HourlyPoint(
                time = times[i],
                precipProbability = h.precipProbability?.getOrNull(i)?.coerceIn(0, 100) ?: 0,
                precipMm = h.precipitation?.getOrNull(i) ?: 0.0,
                tempC = h.temperature?.getOrNull(i) ?: 0.0,
                windKmh = h.windSpeed?.getOrNull(i) ?: 0.0,
            )
        }
        Forecast(latitude = dto.latitude, longitude = dto.longitude, hourly = hourly)
    }.getOrNull()

    @Serializable
    private data class ForecastDto(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val hourly: HourlyDto? = null,
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
