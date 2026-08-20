package io.github.meko123456.tsvima

import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.Upcoming
import java.time.LocalDateTime
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
    fun nextRainNullWhenDry() {
        val dry = listOf(hour("2026-08-20T10:00"), hour("2026-08-20T11:00"))
        assertNull(Upcoming.nextRain(dry))
    }
}
