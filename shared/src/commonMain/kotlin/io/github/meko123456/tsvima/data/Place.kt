package io.github.meko123456.tsvima.data

/** A geocoded place the user can pick as their forecast location. */
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null,
    val country: String? = null,
) {
    /** A human label like "London, England, United Kingdom". */
    val label: String
        get() = listOfNotNull(name, admin1, country).distinct().joinToString(", ")
}
