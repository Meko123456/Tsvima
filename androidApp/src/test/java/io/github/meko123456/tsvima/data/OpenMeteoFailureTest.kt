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
}
