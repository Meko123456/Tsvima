package io.github.meko123456.tsvima.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure parser for the Open-Meteo geocoding search response. Malformed input or no
 * matches yields an empty list. Kept free of platform types for the shared module.
 *
 * ```
 * { "results": [ { "name": "London", "latitude": 51.5, "longitude": -0.12,
 *                  "admin1": "England", "country": "United Kingdom" }, ... ] }
 * ```
 */
object GeocodingParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): List<Place> = runCatching {
        json.decodeFromString<GeoDto>(body).results.orEmpty().map {
            Place(
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                admin1 = it.admin1,
                country = it.country,
            )
        }
    }.getOrDefault(emptyList())

    @Serializable
    private data class GeoDto(val results: List<GeoResult>? = null)

    @Serializable
    private data class GeoResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val admin1: String? = null,
        val country: String? = null,
    )
}
