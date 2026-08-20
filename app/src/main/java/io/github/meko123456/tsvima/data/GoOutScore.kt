package io.github.meko123456.tsvima.data

/**
 * A simple 0–100 "good time to be outside / go for a run" score for the next few hours.
 * Rain dominates; uncomfortable temperature and strong wind subtract smaller amounts.
 * Pure and deterministic so it unit-tests without a device.
 */
object GoOutScore {

    /** Scores the upcoming window (uses the next up-to-3 hours). Empty input → neutral 50. */
    fun score(upcoming: List<HourlyPoint>): Int {
        if (upcoming.isEmpty()) return 50
        val window = upcoming.take(3)

        val maxProb = window.maxOf { it.precipProbability }
        val totalMm = window.sumOf { it.precipMm }
        val rainPenalty = minOf(70.0, maxProb * 0.5 + totalMm * 20.0)

        val temp = window.first().tempC
        val tempPenalty = when {
            temp < 8 -> minOf(25.0, (8 - temp) * 2.0)
            temp > 24 -> minOf(25.0, (temp - 24) * 2.0)
            else -> 0.0
        }

        val wind = window.first().windKmh
        val windPenalty = if (wind > 20) minOf(20.0, (wind - 20) * 1.0) else 0.0

        return (100.0 - rainPenalty - tempPenalty - windPenalty).coerceIn(0.0, 100.0).toInt()
    }

    /** A short human verdict for a score. */
    fun verdict(score: Int): String = when {
        score >= 80 -> "Great time to head out"
        score >= 60 -> "Decent — you're probably fine"
        score >= 40 -> "Iffy — maybe grab a jacket"
        score >= 20 -> "Not great right now"
        else -> "Better to stay in"
    }
}
