package io.github.meko123456.tsvima.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches an hourly forecast from the free, key-less Open-Meteo API and hands the
 * body to [ForecastParser]. Network / HTTP / parse failures are returned, not thrown.
 */
class OpenMeteoClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun forecast(latitude: Double, longitude: Double): Result<Forecast> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", latitude.toString())
                    .addQueryParameter("longitude", longitude.toString())
                    .addQueryParameter(
                        "hourly",
                        "precipitation_probability,precipitation,temperature_2m,wind_speed_10m",
                    )
                    .addQueryParameter("forecast_days", "2")
                    .addQueryParameter("timezone", "auto")
                    .build()
                val request = Request.Builder().url(url).header("User-Agent", "Tsvima/0.1").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body?.string() ?: throw IOException("Empty response")
                    ForecastParser.parse(body) ?: throw IOException("Unexpected forecast format")
                }
            }
        }
}
