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

    // Berlin asked on a Saturday. Open-Meteo stamps Sunday at summer time too (+2), though the clocks
    // go back to +1 at 03:00 that night.
    private val berlinAcrossTheChange = """
        {"latitude":52.52,"longitude":13.41,"utc_offset_seconds":7200,"timezone":"Europe/Berlin",
         "hourly":{"time":["2026-10-25T01:00","2026-10-25T02:00","2026-10-25T03:00","2026-10-25T04:00","2026-10-25T14:00"]}}
    """.trimIndent()

    @Test
    fun hoursPastAClockChangeReadTheWayThePlacesClocksWill() {
        val hours = ForecastParser.parse(berlinAcrossTheChange)!!.hourly
        assertEquals(
            listOf("2026-10-25T01:00", "2026-10-25T02:00", "2026-10-25T02:00", "2026-10-25T03:00", "2026-10-25T13:00"),
            hours.map { it.time },
        )
    }

    @Test
    fun everyHourKeepsTheInstantItStarts() {
        val hours = ForecastParser.parse(berlinAcrossTheChange)!!.hourly
        // 2026-10-24T23:00Z, then an hour apart however the labels repeat; 14:00 sent is 12:00Z.
        assertEquals(
            listOf(1_792_882_800L, 1_792_886_400L, 1_792_890_000L, 1_792_893_600L, 1_792_929_600L),
            hours.map { it.epochSeconds },
        )
    }

    @Test
    fun withoutAZoneNameTheStampsStayAsSent() {
        val noZone = """{"latitude":0,"longitude":0,"utc_offset_seconds":7200,"hourly":{"time":["2026-10-25T14:00"]}}"""
        val p = ForecastParser.parse(noZone)!!.hourly.single()
        assertEquals("2026-10-25T14:00", p.time)
        assertEquals(1_792_929_600L, p.epochSeconds)
    }

    @Test
    fun anUnknownZoneKeepsTheStampsAsSent() {
        val badZone = """
            {"latitude":0,"longitude":0,"utc_offset_seconds":7200,"timezone":"Mars/Olympus_Mons","hourly":{"time":["2026-10-25T14:00"]}}
        """.trimIndent()
        assertEquals("2026-10-25T14:00", ForecastParser.parse(badZone)!!.hourly.single().time)
    }

    @Test
    fun withoutAnOffsetAnHourHasNoInstant() {
        val p = ForecastParser.parse(sample)!!.hourly[1]
        assertEquals("2026-08-20T01:00", p.time)
        assertNull(p.epochSeconds)
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
    fun keepsTheLocationsUtcOffset() {
        // The request asks for timezone=auto, so hourly times come back in the *location's* local
        // time. This offset is the only thing in the response that says which clock that is.
        val london = """
            {"latitude":51.5,"longitude":-0.1,"utc_offset_seconds":3600,
             "hourly":{"time":["2026-08-20T00:00"]}}
        """.trimIndent()
        assertEquals(3600, ForecastParser.parse(london)!!.utcOffsetSeconds)
    }

    @Test
    fun absentOffsetStaysNullRatherThanBecomingZero() {
        // Zero is a real offset (London in winter), so a response without the field must not be
        // read as UTC. Null means "unknown", which the caller answers with the device's own zone.
        val minimal = """{"latitude":1,"longitude":2,"hourly":{"time":["2026-08-20T00:00"]}}"""
        assertNull(ForecastParser.parse(minimal)!!.utcOffsetSeconds)
    }

    @Test
    fun malformedReturnsNull() {
        assertNull(ForecastParser.parse("not json"))
        assertNull(ForecastParser.parse("{}"))
        assertTrue(ForecastParser.parse("""{"latitude":1,"longitude":2}""") == null)
    }
}
