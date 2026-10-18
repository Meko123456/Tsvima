package io.github.meko123456.tsvima.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// A file that won't parse is replaced with empty preferences rather than thrown on every read. The
// CorruptionException from `data` crashed the app at launch, every launch, until its storage was
// cleared by hand.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tsvima",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Persists the last successful forecast so the app can show something offline. */
class ForecastCache(private val context: Context) {

    private val key = stringPreferencesKey("last_forecast")

    suspend fun save(cached: CachedForecast) {
        context.dataStore.edit { prefs -> prefs[key] = ForecastCacheCodec.encode(cached) }
    }

    suspend fun read(): CachedForecast? {
        val text = context.dataStore.data.first()[key] ?: return null
        return ForecastCacheCodec.decode(text)
    }
}
