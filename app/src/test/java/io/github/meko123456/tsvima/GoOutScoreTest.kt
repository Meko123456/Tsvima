package io.github.meko123456.tsvima

import io.github.meko123456.tsvima.data.GoOutScore
import io.github.meko123456.tsvima.data.HourlyPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoOutScoreTest {

    private fun hour(prob: Int, mm: Double, temp: Double, wind: Double) =
        HourlyPoint(time = "t", precipProbability = prob, precipMm = mm, tempC = temp, windKmh = wind)

    @Test
    fun clearMildWeatherScoresHigh() {
        val s = GoOutScore.score(List(3) { hour(0, 0.0, 20.0, 6.0) })
        assertEquals(100, s)
    }

    @Test
    fun heavyRainScoresLow() {
        val s = GoOutScore.score(List(3) { hour(90, 2.0, 15.0, 8.0) })
        assertTrue("expected low score, got $s", s < 40)
    }

    @Test
    fun coldAndWindyReducesScore() {
        val cold = GoOutScore.score(List(3) { hour(0, 0.0, 2.0, 30.0) })
        assertTrue("expected penalty, got $cold", cold < 100)
        assertTrue(cold in 40..80)
    }

    @Test
    fun emptyInputIsNeutral() {
        assertEquals(50, GoOutScore.score(emptyList()))
    }

    @Test
    fun verdictTracksScore() {
        assertEquals("Great time to head out", GoOutScore.verdict(95))
        assertEquals("Better to stay in", GoOutScore.verdict(5))
    }
}
