package io.github.meko123456.tsvima.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertTrue(s < 40, "expected low score, got $s")
    }

    @Test
    fun coldAndWindyReducesScore() {
        val cold = GoOutScore.score(List(3) { hour(0, 0.0, 2.0, 30.0) })
        assertTrue(cold < 100, "expected penalty, got $cold")
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
