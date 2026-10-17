package io.github.meko123456.tsvima.data

import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoFailureTest {

    @Test
    fun noConnectionIsSaidInWordsNotAsTheResolversError() {
        val offline = "Couldn't reach the forecast. Check the phone is online, then retry."
        assertEquals(offline, OpenMeteoFailure.forecast(UnknownHostException("Unable to resolve host \"api.open-meteo.com\"")))
        assertEquals(offline, OpenMeteoFailure.forecast(ConnectException("Failed to connect")))
        assertEquals(offline, OpenMeteoFailure.forecast(InterruptedIOException("timeout")))
    }

    @Test
    fun aStatusAndAnUnreadableReplyAreToldApart() {
        assertEquals(
            "The forecast service answered HTTP 503. Try again in a while.",
            OpenMeteoFailure.forecast(HttpStatusException(503)),
        )
        assertEquals("The forecast came back in a form Tsvima can't read.", OpenMeteoFailure.forecast(UnreadableResponseException()))
        assertEquals("Couldn't load the forecast.", OpenMeteoFailure.forecast(IllegalStateException("anything else")))
    }

    @Test
    fun aFailedSearchSaysWhyInItsOwnWords() {
        assertEquals(
            "Couldn't reach the city search. Check the phone is online.",
            OpenMeteoFailure.search(UnknownHostException("Unable to resolve host \"geocoding-api.open-meteo.com\"")),
        )
        assertEquals(
            "The city search answered HTTP 429. Try again in a while.",
            OpenMeteoFailure.search(HttpStatusException(429)),
        )
        assertEquals("Couldn't search for that city.", OpenMeteoFailure.search(UnreadableResponseException()))
    }
}
