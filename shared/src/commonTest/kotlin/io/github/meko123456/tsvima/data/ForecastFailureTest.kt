package io.github.meko123456.tsvima.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ForecastFailureTest {

    @Test
    fun eachFailureHasItsSentence() {
        assertEquals("Couldn't reach the forecast. Check the phone is online, then retry.", ForecastFailure.Offline.describe())
        assertEquals("The forecast service answered HTTP 503. Try again in a while.", ForecastFailure.HttpStatus(503).describe())
        assertEquals("The forecast came back in a form Tsvima can't read.", ForecastFailure.Unreadable.describe())
        assertEquals("Couldn't load the forecast.", ForecastFailure.Other.describe())
    }
}
