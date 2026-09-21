package io.github.meko123456.tsvima.data

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * These moved here from the Android app along with the code, and running on both platforms is the
 * point of the move: the rule for which hours are still ahead of you is now proved once, on the
 * JVM and on an iOS simulator, instead of proved on Android and reimplemented in Swift.
 */
class UpcomingTest {

    private fun hour(time: String, prob: Int = 0, mm: Double = 0.0) =
        HourlyPoint(time = time, precipProbability = prob, precipMm = mm, tempC = 18.0, windKmh = 5.0)

    private val hours = listOf(
        hour("2026-08-20T10:00"),
        hour("2026-08-20T11:00"),
        hour("2026-08-20T12:00", prob = 60),
        hour("2026-08-20T13:00", mm = 0.5),
    )

    /** 2026-08-20T08:30Z, the instant every offset case below is read at. */
    private val halfPastEight = 1_787_214_600L

    private val tbilisi = 4 * 3600
    private val london = 3600

    @Test
    fun fromNowDropsPastHoursAndKeepsTheCurrentOne() {
        val upcoming = Upcoming.fromNow(hours, LocalDateTime.parse("2026-08-20T11:20"))
        assertEquals(3, upcoming.size) // 11:00 is the hour you are in, and it counts
        assertEquals("2026-08-20T11:00", upcoming.first().time)
    }

    @Test
    fun anUnparseableTimestampIsSkippedRatherThanThrowing() {
        val withJunk = hours + hour("not a time")
        assertEquals(4, Upcoming.fromNow(withJunk, LocalDateTime.parse("2026-08-20T10:00")).size)
    }

    @Test
    fun aLondonForecastReadFromTbilisiKeepsItsEarlyHours() {
        // The bug this guards. Open-Meteo stamps hours in the location's local time, so a London
        // forecast at 09:00 London is read on a phone that already says 12:30. Filtering with the
        // device clock threw away the first three real hours and scored the rest over a window
        // that had not started.
        val londonHours = (9..13).map { hour("2026-08-20T${it.toString().padStart(2, '0')}:00") }

        val withDeviceClock = Upcoming.fromNow(
            londonHours,
            Upcoming.localNow(utcOffsetSeconds = null, nowEpochSeconds = halfPastEight, deviceOffsetSeconds = tbilisi),
        )
        assertEquals(2, withDeviceClock.size) // what used to happen: 09:00–11:00 silently gone
        assertEquals("2026-08-20T12:00", withDeviceClock.first().time)

        val withLocationClock = Upcoming.fromNow(
            londonHours,
            Upcoming.localNow(utcOffsetSeconds = london, nowEpochSeconds = halfPastEight, deviceOffsetSeconds = tbilisi),
        )
        assertEquals(5, withLocationClock.size)
        assertEquals("2026-08-20T09:00", withLocationClock.first().time)
    }

    @Test
    fun localNowUsesTheLocationsOffset() {
        assertEquals(
            LocalDateTime.parse("2026-08-20T09:30"),
            Upcoming.localNow(london, halfPastEight, deviceOffsetSeconds = tbilisi),
        )
    }

    @Test
    fun localNowFallsBackToTheDeviceOffsetWhenTheLocationsIsUnknown() {
        // A forecast cached by a build that predates the offset field. The device's own offset is
        // the old behaviour and stays right for the common case: looking up where you already are.
        assertEquals(
            LocalDateTime.parse("2026-08-20T12:30"),
            Upcoming.localNow(null, halfPastEight, deviceOffsetSeconds = tbilisi),
        )
    }

    @Test
    fun zeroIsAnOffsetRatherThanAMissingValue() {
        assertEquals(
            LocalDateTime.parse("2026-08-20T08:30"),
            Upcoming.localNow(0, halfPastEight, deviceOffsetSeconds = tbilisi),
        )
    }

    @Test
    fun aWesternOffsetGoesBackwardsAcrossMidnight() {
        // Negative offsets are the case a seconds-based API makes easy to get wrong.
        assertEquals(
            LocalDateTime.parse("2026-08-19T22:30"),
            Upcoming.localNow(-10 * 3600, halfPastEight, deviceOffsetSeconds = tbilisi),
        )
    }

    @Test
    fun nextRainFindsTheFirstWetHour() {
        val next = Upcoming.nextRain(Upcoming.fromNow(hours, LocalDateTime.parse("2026-08-20T10:00")))
        assertEquals("2026-08-20T12:00", next!!.time)
    }

    @Test
    fun nextRainIsNullWhenNothingIsWet() {
        assertNull(Upcoming.nextRain(listOf(hour("2026-08-20T10:00"), hour("2026-08-20T11:00"))))
    }

    @Test
    fun eitherThresholdOnItsOwnCountsAsRain() {
        // Both halves matter: drizzle is likely and barely wet, a summer storm is unlikely and very.
        assertEquals("a", Upcoming.nextRain(listOf(hour("a", prob = Upcoming.RAIN_PROBABILITY_PERCENT)))!!.time)
        assertEquals("b", Upcoming.nextRain(listOf(hour("b", mm = Upcoming.RAIN_MILLIMETRES)))!!.time)
        assertNull(Upcoming.nextRain(listOf(hour("c", prob = 49, mm = 0.19))))
    }

    @Test
    fun hoursAheadIsTheTwoStepsTheAppsActuallyCall() {
        // The public entry point. Both apps call this and never see a LocalDateTime; if it ever
        // stops agreeing with the pair it composes, every timezone case above proves nothing.
        assertEquals(
            Upcoming.fromNow(hours, Upcoming.localNow(london, halfPastEight, tbilisi)),
            Upcoming.hoursAhead(hours, halfPastEight, utcOffsetSeconds = london, deviceOffsetSeconds = tbilisi),
        )
    }

    @Test
    fun theThresholdsAreTheOnesTheAppsShare() {
        assertTrue(Upcoming.RAIN_PROBABILITY_PERCENT == 50 && Upcoming.RAIN_MILLIMETRES == 0.2)
    }
}
