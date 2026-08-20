package io.github.meko123456.tsvima.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.tsvima.data.GoOutScore
import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.OpenMeteoClient
import io.github.meko123456.tsvima.data.Upcoming
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HourRow(val label: String, val prob: Int, val mm: Double, val tempC: Double)

sealed interface HomeUi {
    data object Loading : HomeUi
    data class Error(val message: String) : HomeUi
    data class Ready(
        val place: String,
        val score: Int,
        val verdict: String,
        val nextRain: String,
        val hours: List<HourRow>,
    ) : HomeUi
}

/** Loads a forecast for a lat/lon and derives the score, next-rain line, and hourly rows. */
class HomeViewModel(
    private val client: OpenMeteoClient = OpenMeteoClient(),
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUi>(HomeUi.Loading)
    val state: StateFlow<HomeUi> = _state.asStateFlow()

    /** True only during a pull-to-refresh (keeps the current content on screen meanwhile). */
    var refreshing by mutableStateOf(false)
        private set

    private var last: Triple<Double, Double, String>? = null
    private val hourFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun load(latitude: Double, longitude: Double, place: String) {
        last = Triple(latitude, longitude, place)
        _state.value = HomeUi.Loading
        fetch(latitude, longitude, place)
    }

    /** Re-fetches the last place (pull-to-refresh / retry) without a full-screen spinner. */
    fun refresh() {
        val (lat, lon, place) = last ?: return
        refreshing = true
        fetch(lat, lon, place) { refreshing = false }
    }

    private fun fetch(latitude: Double, longitude: Double, place: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            client.forecast(latitude, longitude)
                .onSuccess { forecast ->
                    val upcoming = Upcoming.fromNow(forecast.hourly, LocalDateTime.now())
                    val window = upcoming.take(12)
                    val score = GoOutScore.score(upcoming)
                    _state.value = HomeUi.Ready(
                        place = place,
                        score = score,
                        verdict = GoOutScore.verdict(score),
                        nextRain = nextRainLine(upcoming),
                        hours = window.map {
                            HourRow(label = label(it.time), prob = it.precipProbability, mm = it.precipMm, tempC = it.tempC)
                        },
                    )
                }
                .onFailure { _state.value = HomeUi.Error(it.message ?: "Couldn't load the forecast") }
            onDone()
        }
    }

    private fun nextRainLine(upcoming: List<HourlyPoint>): String {
        val rain = Upcoming.nextRain(upcoming) ?: return "No rain expected in the next 12h ☀️"
        return "Rain likely around ${label(rain.time)} (~${rain.precipProbability}%)"
    }

    private fun label(iso: String): String =
        runCatching { LocalDateTime.parse(iso).format(hourFmt) }.getOrDefault(iso)
}
