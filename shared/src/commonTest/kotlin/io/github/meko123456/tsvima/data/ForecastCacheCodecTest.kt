package io.github.meko123456.tsvima.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The cache is the widget's only source of data — it never fetches — so whatever this codec loses,
 * the widget is permanently wrong about. That makes the round trip worth asserting rather than
 * assuming.
 */
class ForecastCacheCodecTest {

    private val hour = HourlyPoint(
        time = "2026-08-20T09:00",
        precipProbability = 60,
        precipMm = 0.4,
        tempC = 17.5,
        windKmh = 12.0,
    )

    private fun cached(offset: Int?) = CachedForecast(
        place = "London",
        latitude = 51.5,
        longitude = -0.1,
        savedAtEpochMs = 1_755_000_000_000,
        forecast = Forecast(51.5, -0.1, listOf(hour), utcOffsetSeconds = offset),
    )

    @Test
    fun roundTripsEverythingTheUiReads() {
        val restored = ForecastCacheCodec.decode(ForecastCacheCodec.encode(cached(3600)))!!
        assertEquals(cached(3600), restored)
    }

    @Test
    fun keepsTheOffsetSoTheWidgetUsesTheLocationsClock() {
        val text = ForecastCacheCodec.encode(cached(3600))
        assertTrue(text.contains("utcOffsetSeconds"), "offset must survive the cache: $text")
        assertEquals(3600, ForecastCacheCodec.decode(text)!!.forecast.utcOffsetSeconds)
    }

    @Test
    fun readsABlobWrittenBeforeTheOffsetExisted() {
        // Exactly what is sitting in DataStore on an installed phone right now. It must still load
        // — with an unknown offset, which the caller answers with the device's own zone.
        val old = """
            {"place":"Tbilisi","latitude":41.7,"longitude":44.8,"savedAtEpochMs":1755000000000,
             "forecast":{"latitude":41.7,"longitude":44.8,"hourly":[
               {"time":"2026-08-20T09:00","precipProbability":60,"precipMm":0.4,
                "tempC":17.5,"windKmh":12.0}]}}
        """.trimIndent()
        val restored = ForecastCacheCodec.decode(old)!!
        assertNull(restored.forecast.utcOffsetSeconds)
        assertEquals("Tbilisi", restored.place)
        assertEquals(1, restored.forecast.hourly.size)
    }

    @Test
    fun aZeroOffsetSurvivesAsZero() {
        // London in winter. If this ever came back null the widget would fall back to the device
        // clock for a location that genuinely is at UTC.
        assertEquals(0, ForecastCacheCodec.decode(ForecastCacheCodec.encode(cached(0)))!!.forecast.utcOffsetSeconds)
    }

    @Test
    fun corruptOrEmptyTextIsNullRatherThanACrash() {
        assertNull(ForecastCacheCodec.decode("not json"))
        assertNull(ForecastCacheCodec.decode("{}"))
        assertNull(ForecastCacheCodec.decode(""))
    }
}
