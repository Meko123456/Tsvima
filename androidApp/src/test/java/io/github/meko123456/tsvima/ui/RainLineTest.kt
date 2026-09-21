package io.github.meko123456.tsvima.ui

import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.Upcoming
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
        )
        assertEquals("Rain likely around 14:00 (~70%)", line)
    }

    @Test
    fun anHourPastMidnightIsMarkedAsTomorrow() {
        // The case that made this worth a function: read at lunchtime, a bare "around 00:00" is
        // indistinguishable from an hour that has already been and gone.
        val line = nextRainLine(listOf(hour("2026-09-21T12:00"), hour("2026-09-22T00:00", prob = 58)))
        assertEquals("Rain likely around tomorrow 00:00 (~58%)", line)
    }

    @Test
    fun aDryForecastClaimsOnlyTheWindowItChecked() {
        // Not "the next 12h" — that was the timeline's length. nextRain reads every upcoming hour.
        val line = nextRainLine(listOf(hour("2026-09-21T12:00"), hour("2026-09-21T13:00")))
        assertEquals("No rain in the rest of the forecast ☀️", line)
        assertTrue("12h" !in line)
    }

    @Test
    fun theWetThresholdIsTheSharedOneRatherThanAnythingRestatedHere() {
        val justUnder = listOf(hour("2026-09-21T12:00", prob = Upcoming.RAIN_PROBABILITY_PERCENT - 1, mm = 0.19))
        val justOver = listOf(hour("2026-09-21T12:00", prob = Upcoming.RAIN_PROBABILITY_PERCENT))
        assertTrue(nextRainLine(justUnder).startsWith("No rain"))
        assertTrue(nextRainLine(justOver).startsWith("Rain likely"))
    }

    @Test
    fun anEmptyForecastReadsAsDryRatherThanCrashing() {
        assertEquals("No rain in the rest of the forecast ☀️", nextRainLine(emptyList()))
    }

    @Test
    fun anUnparseableStampIsShownRawRatherThanSwallowed() {
        // Better a visibly odd label than a confident wrong time.
        assertEquals("not-a-time", hourLabel("not-a-time"))
        assertEquals("14:00", hourLabel("2026-09-21T14:00"))
    }
}
