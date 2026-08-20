package io.github.meko123456.tsvima.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A saved forecast snapshot for offline glances: which place, when, and the data. */
@Serializable
data class CachedForecast(
    val place: String,
    val latitude: Double,
    val longitude: Double,
    val savedAtEpochMs: Long,
    val forecast: Forecast,
)

/** Pure JSON encode/decode for [CachedForecast], so the persistence layer stays platform-only. */
object ForecastCacheCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(cached: CachedForecast): String = json.encodeToString(cached)

    fun decode(text: String): CachedForecast? =
        runCatching { json.decodeFromString<CachedForecast>(text) }.getOrNull()
}
