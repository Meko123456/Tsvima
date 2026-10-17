package io.github.meko123456.tsvima.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Searches place names via the Open-Meteo geocoding API (key-less). */
class GeocodingClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun search(query: String): Result<List<Place>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
                .addQueryParameter("name", query)
                .addQueryParameter("count", "5")
                .addQueryParameter("language", "en")
                .addQueryParameter("format", "json")
                .build()
            val request = Request.Builder().url(url).header("User-Agent", "Tsvima/0.1").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw HttpStatusException(response.code)
                val body = response.body?.string() ?: throw UnreadableResponseException()
                GeocodingParser.parse(body)
            }
        }
    }
}
