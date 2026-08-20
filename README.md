# Tsvima 🌧

**წვიმა** (*tsvima* — Georgian for "rain") — a focused, one-glance **rain nowcast**
for Android.

Not another do-everything weather app. Tsvima answers one question well: **is it
about to rain, and is right now a good time to head out?** Minute-by-minute
precipitation for the next few hours, plus a simple "good to go out" score.

## Planned features

- 🌦️ **Rain next 3h** — hourly precipitation probability + amount for your location,
  from **[Open-Meteo](https://open-meteo.com/)** (free, no API key).
- 🏃 **Go-out score** — a simple 0–100 "good time to be outside / run" score derived
  from rain, temperature, and wind.
- 📍 **Location** — device location (coarse) with a manual city fallback; last result
  cached for offline glances.
- 🔄 **Home-screen widget** — the next-rain answer at a glance (Glance) *(stretch)*.
- 🎨 **Material 3** — dynamic color, light/dark, edge-to-edge.

## Tech

Single-module **Jetpack Compose** app on the shared toolchain: Material 3,
**OkHttp** for the Open-Meteo API, a small pure parser (unit-tested), Kotlin coroutines.

- Gradle 9.3.1 · AGP 9.1.1 · Kotlin 2.3.21 · Compose BOM 2026.06.01
- compileSdk 36 · minSdk 26

## Structure

```
data/  Open-Meteo client + response parsing + go-out score (pure, testable)
ui/    Compose home (rain timeline + score), theme
```

## Status

🚧 Day 1 — README-first. See [issues](../../issues) for the roadmap.

## License

[MIT](LICENSE)
