package io.github.meko123456.tsvima.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForecastParserTest {

    private val sample = """
        {
          "latitude": 41.7,
          "longitude": 44.8,
          "generationtime_ms": 0.12,
          "hourly": {
            "time": ["2026-08-20T00:00", "2026-08-20T01:00", "2026-08-20T02:00"],
            "precipitation_probability": [10, 55, 80],
            "precipitation": [0.0, 0.3, 1.2],
            "temperature_2m": [19.5, 19.0, 18.6],
            "wind_speed_10m": [8.0, 9.2, 11.0]
          }
        }
    """.trimIndent()

    @Test
    fun parsesCoordinatesAndHourlyPoints() {
        val f = ForecastParser.parse(sample)!!
        assertEquals(41.7, f.latitude, 0.001)
        assertEquals(44.8, f.longitude, 0.001)
        assertEquals(3, f.hourly.size)

        val second = f.hourly[1]
        assertEquals("2026-08-20T01:00", second.time)
        assertEquals(55, second.precipProbability)
        assertEquals(0.3, second.precipMm, 0.001)
        assertEquals(19.0, second.tempC, 0.001)
        assertEquals(9.2, second.windKmh, 0.001)
    }

    @Test
    fun clampsProbabilityIntoRange() {
        val weird = """
            {"latitude":0,"longitude":0,"hourly":{"time":["t"],"precipitation_probability":[150]}}
        """.trimIndent()
        assertEquals(100, ForecastParser.parse(weird)!!.hourly[0].precipProbability)
    }

    @Test
    fun missingHourlyFieldsDefaultToZero() {
        val minimal = """{"latitude":1,"longitude":2,"hourly":{"time":["2026-08-20T00:00"]}}"""
        val p = ForecastParser.parse(minimal)!!.hourly[0]
        assertEquals(0, p.precipProbability)
        assertEquals(0.0, p.precipMm, 0.001)
    }

    @Test
    fun malformedReturnsNull() {
        assertNull(ForecastParser.parse("not json"))
        assertNull(ForecastParser.parse("{}"))
        assertTrue(ForecastParser.parse("""{"latitude":1,"longitude":2}""") == null)
    }
}
