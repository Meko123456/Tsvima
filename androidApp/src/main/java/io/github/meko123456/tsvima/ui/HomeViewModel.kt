package io.github.meko123456.tsvima.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.tsvima.data.CachedForecast
import io.github.meko123456.tsvima.data.Forecast
import io.github.meko123456.tsvima.data.ForecastCache
import io.github.meko123456.tsvima.data.GeocodingClient
import io.github.meko123456.tsvima.data.GoOutScore
import io.github.meko123456.tsvima.data.HourlyPoint
import io.github.meko123456.tsvima.data.OpenMeteoClient
import io.github.meko123456.tsvima.data.Place
import io.github.meko123456.tsvima.data.Upcoming
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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
        val stale: Boolean = false,
        val asOf: String? = null,
    ) : HomeUi
}

/** Loads a forecast for a lat/lon, derives the score, and caches the last result for offline use. */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val client = OpenMeteoClient()
    private val geocoder = GeocodingClient()
    private val cache = ForecastCache(app)

    private val _state = MutableStateFlow<HomeUi>(HomeUi.Loading)
    val state: StateFlow<HomeUi> = _state.asStateFlow()

    var refreshing by mutableStateOf(false)
        private set

    var searching by mutableStateOf(false)
        private set
    var searchResults by mutableStateOf<List<Place>>(emptyList())
        private set

    private var last: Triple<Double, Double, String>? = null
    private val hourFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun load(latitude: Double, longitude: Double, place: String) {
        last = Triple(latitude, longitude, place)
        _state.value = HomeUi.Loading
        fetch(latitude, longitude, place)
    }

    fun refresh() {
        val (lat, lon, place) = last ?: return
        refreshing = true
        fetch(lat, lon, place) { refreshing = false }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searching = true
            searchResults = geocoder.search(query).getOrDefault(emptyList())
            searching = false
        }
    }

    fun clearSearch() { searchResults = emptyList() }

    fun pickPlace(place: Place) {
        clearSearch()
        load(place.latitude, place.longitude, place.label)
    }

    private fun fetch(latitude: Double, longitude: Double, place: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            client.forecast(latitude, longitude)
                .onSuccess { forecast ->
                    _state.value = ready(place, forecast, stale = false, asOf = null)
                    cache.save(CachedForecast(place, latitude, longitude, System.currentTimeMillis(), forecast))
                }
                .onFailure { error ->
                    val cached = cache.read()
                    _state.value = if (cached != null) {
                        ready(cached.place, cached.forecast, stale = true, asOf = clock(cached.savedAtEpochMs))
                    } else {
                        HomeUi.Error(error.message ?: "Couldn't load the forecast")
                    }
                }
            onDone()
        }
    }

    private fun ready(place: String, forecast: Forecast, stale: Boolean, asOf: String?): HomeUi.Ready {
        val upcoming = Upcoming.fromNow(forecast.hourly, LocalDateTime.now())
        val score = GoOutScore.score(upcoming)
        return HomeUi.Ready(
            place = place,
            score = score,
            verdict = GoOutScore.verdict(score),
            nextRain = nextRainLine(upcoming),
            hours = upcoming.take(12).map {
                HourRow(label = label(it.time), prob = it.precipProbability, mm = it.precipMm, tempC = it.tempC)
            },
            stale = stale,
            asOf = asOf,
        )
    }

    private fun nextRainLine(upcoming: List<HourlyPoint>): String {
        val rain = Upcoming.nextRain(upcoming) ?: return "No rain expected in the next 12h ☀️"
        return "Rain likely around ${label(rain.time)} (~${rain.precipProbability}%)"
    }

    private fun label(iso: String): String =
        runCatching { LocalDateTime.parse(iso).format(hourFmt) }.getOrDefault(iso)

    private fun clock(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime().format(hourFmt)
}
