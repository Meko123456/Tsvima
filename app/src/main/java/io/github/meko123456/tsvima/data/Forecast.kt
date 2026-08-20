package io.github.meko123456.tsvima.data

/** One hour of forecast, in the location's local time. */
data class HourlyPoint(
    val time: String,            // ISO local, e.g. "2026-08-20T14:00"
    val precipProbability: Int,  // percent, 0..100
    val precipMm: Double,        // millimetres in that hour
    val tempC: Double,
    val windKmh: Double,
)

/** A parsed forecast for one place: its coordinates and an hourly timeline. */
data class Forecast(
    val latitude: Double,
    val longitude: Double,
    val hourly: List<HourlyPoint>,
)
