package io.github.meko123456.tsvima.ui

import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.Upcoming
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the app *says* about rain. Which hour it is talking about is `Upcoming`'s job, proved in
 * `:shared` on both platforms; this covers the part that is only Android's.
 */
class RainLineTest {

    private fun hour(time: String, prob: Int = 0, mm: Double = 0.0) =
        HourlyPoint(time = time, precipProbability = prob, precipMm = mm, tempC = 18.0, windKmh = 5.0)

    @Test
    fun namesTheFirstWetHourAndItsOdds() {
        val line = nextRainLine(
            listOf(hour("2026-09-21T12:00"), hour("2026-09-21T13:00"), hour("2026-09-21T14:00", prob = 70)),
            is24Hour = true,
        )
        assertEquals("Rain likely around 14:00 (~70%)", line)
    }

    @Test
    fun anHourPastMidnightIsMarkedAsTomorrow() {
        // The case that made this worth a function: read at lunchtime, a bare "around 00:00" is
        // indistinguishable from an hour that has already been and gone.
        val line = nextRainLine(listOf(hour("2026-09-21T12:00"), hour("2026-09-22T00:00", prob = 58)), is24Hour = true)
        assertEquals("Rain likely around tomorrow 00:00 (~58%)", line)
    }

    @Test
    fun anHourWetOnlyByItsAmountIsPossibleAndSaysHowMuch() {
        // Seen on 5 October 2026 for Tbilisi: 0.5 mm forecast at a 0% chance. The amount alone
        // makes the hour wet, and the line used to call that "Rain likely ... (~0%)".
        val line = nextRainLine(listOf(hour("2026-10-05T12:00"), hour("2026-10-06T15:00", prob = 0, mm = 0.5)), is24Hour = true)
        assertEquals("Rain possible around tomorrow 15:00 (0.5 mm)", line)
    }

    @Test
    fun aLikelyHourIsCalledLikelyWhateverItsAmount() {
        val line = nextRainLine(listOf(hour("2026-10-05T14:00", prob = 80, mm = 3.0)), is24Hour = true)
        assertEquals("Rain likely around 14:00 (~80%)", line)
    }

    @Test
    fun aDryForecastClaimsOnlyTheWindowItChecked() {
        // Not "the next 12h" — that was the timeline's length. nextRain reads every upcoming hour.
        val line = nextRainLine(listOf(hour("2026-09-21T12:00"), hour("2026-09-21T13:00")), is24Hour = true)
        assertEquals("No rain in the rest of the forecast ☀️", line)
        assertTrue("12h" !in line)
    }

    @Test
    fun theWetThresholdIsTheSharedOneRatherThanAnythingRestatedHere() {
        val justUnder = listOf(hour("2026-09-21T12:00", prob = Upcoming.RAIN_PROBABILITY_PERCENT - 1, mm = 0.19))
        val justOver = listOf(hour("2026-09-21T12:00", prob = Upcoming.RAIN_PROBABILITY_PERCENT))
        assertTrue(nextRainLine(justUnder, is24Hour = true).startsWith("No rain"))
        assertTrue(nextRainLine(justOver, is24Hour = true).startsWith("Rain likely"))
        val wetByAmount = listOf(hour("2026-09-21T12:00", prob = 0, mm = Upcoming.RAIN_MILLIMETRES))
        assertTrue(nextRainLine(wetByAmount, is24Hour = true).startsWith("Rain possible"))
    }

    @Test
    fun anEmptyForecastReadsAsDryRatherThanCrashing() {
        assertEquals("No rain in the rest of the forecast ☀️", nextRainLine(emptyList(), is24Hour = true))
    }

    @Test
    fun anUnparseableStampIsShownRawRatherThanSwallowed() {
        // Better a visibly odd label than a confident wrong time.
        assertEquals("not-a-time", hourLabel("not-a-time", is24Hour = true))
        assertEquals("14:00", hourLabel("2026-09-21T14:00", is24Hour = true))
    }

    @Test
    fun aPhoneOnATwelveHourClockReadsTwelveHourTimes() {
        // Its status bar said 2:00 PM while the forecast said 14:00.
        val before = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("2:00 PM", hourLabel("2026-09-21T14:00", is24Hour = false))
            assertEquals("12:00 AM", hourLabel("2026-09-22T00:00", is24Hour = false))
            assertEquals(
                "Rain likely around tomorrow 12:00 AM (~58%)",
                nextRainLine(listOf(hour("2026-09-21T12:00"), hour("2026-09-22T00:00", prob = 58)), is24Hour = false),
            )
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun theWidgetSaysWhenRainIsTomorrows() {
        // It used to print the stamp's last five characters: "Rain ~00:00" at lunchtime.
        val upcoming = listOf(hour("2026-09-21T12:00"), hour("2026-09-22T00:00", prob = 58))
        assertEquals("Rain tomorrow ~00:00", widgetRainLine(upcoming, is24Hour = true))
        assertEquals("Rain ~14:00", widgetRainLine(listOf(hour("2026-09-21T14:00", prob = 70)), is24Hour = true))
        assertEquals("No rain soon ☀️", widgetRainLine(listOf(hour("2026-09-21T14:00")), is24Hour = true))
    }

    @Test
    fun theWidgetFollowsATwelveHourClockToo() {
        val before = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            assertEquals("Rain ~2:00 PM", widgetRainLine(listOf(hour("2026-09-21T14:00", prob = 70)), is24Hour = false))
        } finally {
            Locale.setDefault(before)
        }
    }
}
