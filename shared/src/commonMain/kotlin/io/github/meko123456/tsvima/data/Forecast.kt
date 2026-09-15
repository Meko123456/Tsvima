package io.github.meko123456.tsvima.data

import kotlinx.serialization.Serializable

/** One hour of forecast, in the location's local time. */
@Serializable
data class HourlyPoint(
    val time: String,            // ISO local, e.g. "2026-08-20T14:00"
    val precipProbability: Int,  // percent, 0..100
    val precipMm: Double,        // millimetres in that hour
    val tempC: Double,
    val windKmh: Double,
)

/** A parsed forecast for one place: its coordinates and an hourly timeline. */
@Serializable
data class Forecast(
    val latitude: Double,
    val longitude: Double,
    val hourly: List<HourlyPoint>,
    /**
     * Seconds the forecast location is offset from UTC, as Open-Meteo reports it for `timezone=auto`.
     *
     * Needed because [HourlyPoint.time] is the *location's* local time. Filtering those hours against
     * the phone's clock silently drops the wrong ones the moment the two places differ — searching
     * for London from Tbilisi lost the first few hours of the forecast and scored the rest over a
     * window that had not started yet.
     *
     * Nullable rather than defaulting to zero, because zero is a real offset (London in winter) and
     * a cached response written by an older build genuinely has no value here. Null means "fall back
     * to the device's clock", which is what this app did before and is right whenever the place being
     * looked at is the one you are standing in.
     */
    val utcOffsetSeconds: Int? = null,
)
