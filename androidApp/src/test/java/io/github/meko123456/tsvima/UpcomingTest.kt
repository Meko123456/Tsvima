package io.github.meko123456.tsvima

import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.Upcoming
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpcomingTest {

    private fun hour(time: String, prob: Int = 0, mm: Double = 0.0) =
        HourlyPoint(time = time, precipProbability = prob, precipMm = mm, tempC = 18.0, windKmh = 5.0)

    private val hours = listOf(
        hour("2026-08-20T10:00"),
        hour("2026-08-20T11:00"),
        hour("2026-08-20T12:00", prob = 60),
        hour("2026-08-20T13:00", mm = 0.5),
    )

    @Test
    fun fromNowDropsPastHoursAndKeepsCurrentHour() {
        val now = LocalDateTime.parse("2026-08-20T11:20")
        val upcoming = Upcoming.fromNow(hours, now)
        assertEquals(3, upcoming.size) // 11:00 (current hour) onward
        assertEquals("2026-08-20T11:00", upcoming.first().time)
    }

    @Test
    fun nextRainFindsFirstWetHour() {
        val now = LocalDateTime.parse("2026-08-20T10:00")
        val next = Upcoming.nextRain(Upcoming.fromNow(hours, now))
        assertEquals("2026-08-20T12:00", next!!.time)
    }

    @Test
    fun aLondonForecastReadFromTbilisiKeepsItsEarlyHours() {
        // The bug this guards. Open-Meteo stamps hours in the *location's* local time, so a London
        // forecast at 09:00 London is being read on a phone that already says 13:00. Filtering with
        // the device clock threw away the first three real hours and scored the rest over a window
        // that had not started. 09:00 London is the current hour and must survive.
        val londonHours = listOf(
            hour("2026-08-20T09:00"),
            hour("2026-08-20T10:00"),
            hour("2026-08-20T11:00"),
            hour("2026-08-20T12:00"),
            hour("2026-08-20T13:00"),
        )
        val instant = Instant.parse("2026-08-20T08:30:00Z") // 09:30 London, 12:30 Tbilisi
        val tbilisi = ZoneId.of("Asia/Tbilisi")

        val withDeviceClock = Upcoming.fromNow(londonHours, LocalDateTime.ofInstant(instant, tbilisi))
        assertEquals(2, withDeviceClock.size) // what used to happen: 09:00–11:00 silently gone
        assertEquals("2026-08-20T12:00", withDeviceClock.first().time)

        val withLocationClock =
            Upcoming.fromNow(londonHours, Upcoming.localNow(3600, instant, tbilisi))
        assertEquals(5, withLocationClock.size)
        assertEquals("2026-08-20T09:00", withLocationClock.first().time)
    }

    @Test
    fun localNowUsesTheLocationsOffset() {
        val instant = Instant.parse("2026-08-20T08:30:00Z")
        assertEquals(
            LocalDateTime.parse("2026-08-20T09:30"),
            Upcoming.localNow(3600, instant, ZoneId.of("Asia/Tbilisi")),
        )
    }

    @Test
    fun localNowFallsBackToTheDeviceZoneWhenTheOffsetIsUnknown() {
        // A forecast cached by a build that predates the offset field. The device's own zone is the
        // old behaviour and stays correct for the common case: looking up where you already are.
        val instant = Instant.parse("2026-08-20T08:30:00Z")
        assertEquals(
            LocalDateTime.parse("2026-08-20T12:30"),
            Upcoming.localNow(null, instant, ZoneId.of("Asia/Tbilisi")),
        )
    }

    @Test
    fun zeroOffsetIsAnOffsetNotAMissingValue() {
        val instant = Instant.parse("2026-08-20T08:30:00Z")
        assertEquals(
            LocalDateTime.parse("2026-08-20T08:30"),
            Upcoming.localNow(0, instant, ZoneId.of("Asia/Tbilisi")),
        )
        assertEquals(ZoneOffset.UTC.totalSeconds, 0)
    }

    @Test
    fun nextRainNullWhenDry() {
        val dry = listOf(hour("2026-08-20T10:00"), hour("2026-08-20T11:00"))
        assertNull(Upcoming.nextRain(dry))
    }
}
