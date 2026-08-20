package io.github.meko123456.tsvima.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager

/**
 * Reads the device's last known coarse location via the framework [LocationManager]
 * (no Google Play Services dependency). Callers must already hold a location permission.
 */
class DeviceLocation(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun lastKnown(): Pair<Double, Double>? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
        for (provider in providers) {
            if (!lm.isProviderEnabled(provider)) continue
            val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
            return loc.latitude to loc.longitude
        }
        return null
    }
}
