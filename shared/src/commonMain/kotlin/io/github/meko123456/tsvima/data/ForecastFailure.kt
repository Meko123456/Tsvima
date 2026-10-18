package io.github.meko123456.tsvima.data

/**
 * Why a forecast didn't load, as each platform's networking reports it: Android reads it from
 * OkHttp's exceptions, iOS from URLSession's errors and the response. Both say it in the same
 * words, [describe], rather than each in its own.
 */
sealed interface ForecastFailure {
    /** No connection, no DNS, or the request timed out. */
    data object Offline : ForecastFailure

    /** The service answered, with something other than success. */
    data class HttpStatus(val code: Int) : ForecastFailure

    /** Something came back that the parser can't read. */
    data object Unreadable : ForecastFailure

    /** Anything else. */
    data object Other : ForecastFailure

    /** The sentence the screen shows. */
    fun describe(): String = when (this) {
        Offline -> "Couldn't reach the forecast. Check the phone is online, then retry."
        is HttpStatus -> "The forecast service answered HTTP $code. Try again in a while."
        Unreadable -> "The forecast came back in a form Tsvima can't read."
        Other -> "Couldn't load the forecast."
    }
}
