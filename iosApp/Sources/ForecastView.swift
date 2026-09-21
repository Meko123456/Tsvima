import SwiftUI
import Shared

/// The one question the app answers — is now a good time to head out — and the hours behind it.
struct ForecastView: View {
    @EnvironmentObject private var store: ForecastStore

    var body: some View {
        NavigationStack {
            List {
                placeSection
                switch store.state {
                case .loading:
                    Section { ProgressView("Fetching the forecast…") }
                case .failed(let message):
                    failureSection(message)
                case .loaded(let snapshot):
                    scoreSection(snapshot)
                    timelineSection(snapshot)
                    sourceSection(snapshot)
                }
            }
            .navigationTitle("Tsvima 🌧")
            .refreshable { await store.load() }
            .task(id: store.placeIndex) { await store.load() }
        }
    }

    private var placeSection: some View {
        Section {
            Picker("Place", selection: $store.placeIndex) {
                ForEach(Array(store.places.enumerated()), id: \.offset) { index, place in
                    Text(place.label).tag(index)
                }
            }
            .pickerStyle(.menu)
        }
    }

    private func failureSection(_ message: String) -> some View {
        Section("Could not load") {
            Text(message)
            // Said plainly, because the alternative — showing an old or invented forecast — would
            // let a screenshot imply data the app never actually had.
            Text("Tsvima fetches live from Open-Meteo and ships no sample data, so there is nothing to show until the request succeeds.")
                .font(.caption)
                .foregroundStyle(.secondary)
            Button("Try again") { Task { await store.load() } }
        }
    }

    private func scoreSection(_ snapshot: ForecastStore.Snapshot) -> some View {
        Section("Go-out score") {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text("\(snapshot.score)")
                    .font(.system(size: 56, weight: .bold, design: .rounded))
                    .foregroundStyle(colour(for: snapshot.score))
                VStack(alignment: .leading, spacing: 4) {
                    Text(snapshot.verdict).font(.headline)
                    Text(windowLabel(snapshot))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.vertical, 4)

            Label(rainLabel(snapshot), systemImage: snapshot.nextRain == nil ? "sun.max" : "cloud.rain")
                .font(.subheadline)
                .foregroundStyle(snapshot.nextRain == nil ? Color.secondary : .blue)
                .accessibilityIdentifier("nextRain")
        }
    }

    /// The line this app could not show until `Upcoming` moved into `:shared`.
    ///
    /// What counts as rain is `Upcoming.RAIN_PROBABILITY_PERCENT` / `RAIN_MILLIMETRES`, decided in
    /// Kotlin. Swift only picks the words, which is the one part that genuinely differs per app.
    private func rainLabel(_ snapshot: ForecastStore.Snapshot) -> String {
        guard let rain = snapshot.nextRain else {
            return "No rain in the rest of the forecast"
        }
        // The forecast runs two days, so the first wet hour is often tomorrow's. "around 00:00"
        // read at lunchtime looks like an hour already gone; the timeline rows say "tomorrow" for
        // the same reason, and this line has no column to say it in.
        let sameDay = snapshot.upcoming.first.map { $0.time.prefix(10) == rain.time.prefix(10) } ?? true
        let when = sameDay ? hourLabel(rain.time) : "tomorrow \(hourLabel(rain.time))"
        return "Rain likely around \(when) (~\(rain.precipProbability)%)"
    }

    private func timelineSection(_ snapshot: ForecastStore.Snapshot) -> some View {
        Section("Next hours") {
            if snapshot.upcoming.isEmpty {
                Text("The forecast has no hours left from now.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(Array(snapshot.upcoming.prefix(12).enumerated()), id: \.offset) { _, hour in
                    HourRow(hour: hour, firstDay: String(snapshot.upcoming[0].time.prefix(10)))
                }
            }
        }
    }

    private func sourceSection(_ snapshot: ForecastStore.Snapshot) -> some View {
        Section("Source") {
            LabeledContent(
                "Grid point",
                value: String(format: "%.4f, %.4f", snapshot.forecast.latitude, snapshot.forecast.longitude)
            )
            LabeledContent("Hours returned", value: "\(snapshot.forecast.hourly.count)")
            LabeledContent("Location offset", value: offsetLabel(snapshot.forecast.utcOffsetSeconds))
            Text("Live from api.open-meteo.com, parsed by the shared Kotlin ForecastParser, filtered to the location's own clock by Upcoming and scored by GoOutScore — the same code the Android app runs.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    /// Names the hours the score actually covers. `GoOutScore` reads the first three, and a number
    /// with no stated window is a number nobody can check against the timeline below it.
    private func windowLabel(_ snapshot: ForecastStore.Snapshot) -> String {
        let window = snapshot.upcoming.prefix(3)
        guard let first = window.first, let last = window.last else { return "No upcoming hours" }
        return "Next \(window.count)h · \(hourLabel(first.time))–\(hourLabel(last.time)) local to \(snapshot.place.name)"
    }

    private func offsetLabel(_ seconds: KotlinInt?) -> String {
        guard let seconds = seconds?.intValue else { return "unknown — using this device's zone" }
        let sign = seconds < 0 ? "-" : "+"
        let total = abs(seconds)
        return String(format: "UTC%@%02d:%02d", sign, total / 3600, (total % 3600) / 60)
    }

    /// Colour backs up the verdict, it never carries the meaning on its own.
    private func colour(for score: Int32) -> Color {
        switch score {
        case 80...: return .green
        case 60..<80: return .mint
        case 40..<60: return .orange
        default: return .red
        }
    }
}

/// One forecast hour. Rain first, because rain is the question.
private struct HourRow: View {
    let hour: HourlyPoint
    /// The date of the first hour shown, so later rows can say when they cross midnight.
    let firstDay: String

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(hourLabel(hour.time)).font(.body.monospacedDigit())
                if String(hour.time.prefix(10)) != firstDay {
                    Text("tomorrow").font(.caption2).foregroundStyle(.secondary)
                }
            }
            .frame(width: 76, alignment: .leading)

            Text("\(hour.precipProbability)%")
                .font(.body.monospacedDigit())
                .frame(width: 50, alignment: .trailing)
                // The shared threshold, not a 50 typed here: the highlight and the "next rain"
                // line above have to agree about which hours are the wet ones.
                .foregroundStyle(hour.precipProbability >= Upcoming.shared.RAIN_PROBABILITY_PERCENT ? Color.blue : .secondary)

            Text(hour.precipMm > 0 ? String(format: "%.1f mm", hour.precipMm) : "—")
                .font(.caption.monospacedDigit())
                .frame(width: 64, alignment: .trailing)
                .foregroundStyle(.secondary)

            Spacer()

            Text(String(format: "%.0f°", hour.tempC)).font(.body.monospacedDigit())
            Text(String(format: "%.0f km/h", hour.windKmh))
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(width: 66, alignment: .trailing)
        }
    }
}

/// "2026-09-20T14:00" → "14:00". The stamps are fixed-width, so the tail is the clock.
private func hourLabel(_ isoLocal: String) -> String { String(isoLocal.suffix(5)) }
