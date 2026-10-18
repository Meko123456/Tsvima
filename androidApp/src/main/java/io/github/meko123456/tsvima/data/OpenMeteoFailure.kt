package io.github.meko123456.tsvima.data

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException

/** Open-Meteo answered, but not with a success. */
class HttpStatusException(val code: Int) : IOException("HTTP $code")

/** Something came back, and it isn't the shape the parser reads. */
class UnreadableResponseException : IOException("Unexpected response format")

/**
 * Why the forecast or a city search failed, in words someone can act on, rather than the
 * exception's own message (offline, the resolver's "Unable to resolve host …").
 */
object OpenMeteoFailure {
    /** The words are shared with iOS ([ForecastFailure]); only reading the exception is Android's. */
    fun forecast(error: Throwable): String = when {
        error.isOffline() -> ForecastFailure.Offline
        error is HttpStatusException -> ForecastFailure.HttpStatus(error.code)
        error is UnreadableResponseException -> ForecastFailure.Unreadable
        else -> ForecastFailure.Other
    }.describe()

    fun search(error: Throwable): String = when {
        error.isOffline() -> "Couldn't reach the city search. Check the phone is online."
        error is HttpStatusException -> "The city search answered HTTP ${error.code}. Try again in a while."
        else -> "Couldn't search for that city."
    }

    /** No route, no DNS, or the call timed out: OkHttp's timeouts are InterruptedIOExceptions. */
    private fun Throwable.isOffline() =
        this is UnknownHostException || this is ConnectException || this is InterruptedIOException
}
