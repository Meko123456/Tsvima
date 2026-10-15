import SwiftUI
import Shared

/// The one question the app answers — is now a good time to head out — and the hours behind it.
struct ForecastView: View {
    @EnvironmentObject private var store: ForecastStore
    @Environment(\.dynamicTypeSize) private var typeSize

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
            if typeSize.isAccessibilitySize {
                // A menu picker draws its value in a button of fixed height, so at these sizes a
                // place that wraps, "Tbilisi, Georgia", lost its second line. This row grows instead.
                placePicker.pickerStyle(.navigationLink)
            } else {
                placePicker.pickerStyle(.menu)
            }
        }
    }

    private var placePicker: some View {
        Picker("Place", selection: $store.placeIndex) {
            ForEach(Array(store.places.enumerated()), id: \.offset) { index, place in
                Text(place.label).tag(index)
            }
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
        // An hour can be wet by its chance of rain or by its amount, which Open-Meteo takes from
        // different models; they can disagree, as with 0.5 mm at 0%. Cite whichever said so:
        // "Rain likely" beside "~0%" contradicts itself.
        if rain.precipProbability >= Upcoming.shared.RAIN_PROBABILITY_PERCENT {
            return "Rain likely around \(when) (~\(rain.precipProbability)%)"
        }
        return "Rain possible around \(when) (\(String(format: "%.1f", rain.precipMm)) mm)"
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

    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        if typeSize.isAccessibilitySize {
            lines
        } else {
            // A 12-hour clock makes the time wider, and at the largest standard size the columns
            // no longer fit: the temperature broke into "1" over "6°". The lines take over then.
            ViewThatFits(in: .horizontal) {
                columns
                lines
            }
        }
    }

    /// The columns below have fixed widths, and at the accessibility text sizes they broke every
    /// value into pieces: "13 :0 0", "0 %", "2 3 °". Here each hour is three short lines instead,
    /// with words where the columns used to say what a number was.
    private var lines: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(isTomorrow ? "\(hourLabel(hour.time)) tomorrow" : hourLabel(hour.time))
                .font(.body.monospacedDigit())
            Text(rainLine)
                .foregroundStyle(hour.precipProbability >= Upcoming.shared.RAIN_PROBABILITY_PERCENT ? Color.blue : .secondary)
            Text(String(format: "%.0f°, wind %.0f km/h", hour.tempC, hour.windKmh))
                .foregroundStyle(.secondary)
        }
    }

    private var isTomorrow: Bool { String(hour.time.prefix(10)) != firstDay }

    private var rainLine: String {
        let chance = "\(hour.precipProbability)% chance of rain"
        return hour.precipMm > 0 ? chance + String(format: ", %.1f mm", hour.precipMm) : chance
    }

    private var columns: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                // As wide as the widest time this clock can show, and never narrower than the 76
                // points the column always had, so every row lines up. "22:00" fits in 76;
                // "10:00 PM", on a phone set to a 12-hour clock, does not.
                ZStack(alignment: .leading) {
                    Color.clear.frame(width: 76, height: 0)
                    Text(hourLabel("2026-01-01T10:00")).hidden()
                    Text(hourLabel("2026-01-01T22:00")).hidden()
                    Text(hourLabel(hour.time))
                }
                .font(.body.monospacedDigit())
                if String(hour.time.prefix(10)) != firstDay {
                    Text("tomorrow").font(.caption2).foregroundStyle(.secondary)
                }
            }
            .fixedSize(horizontal: true, vertical: false)

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

/// "2026-09-20T14:00" → "14:00", or "2:00 PM" where the phone keeps a 12-hour clock: the template's
/// "j" is the hour in whichever cycle the user chose. The stamp is the place's own wall clock, so
/// it is read and written in UTC, where no zone of this device's can move it. Formatters are made
/// per call because the setting can change while the app is open.
private func hourLabel(_ isoLocal: String) -> String {
    let utc = TimeZone(identifier: "UTC")
    let parser = DateFormatter()
    parser.locale = Locale(identifier: "en_US_POSIX")
    parser.timeZone = utc
    parser.dateFormat = "yyyy-MM-dd'T'HH:mm"
    // The stamps are fixed-width, so an unreadable one still ends in the clock.
    guard let date = parser.date(from: isoLocal) else { return String(isoLocal.suffix(5)) }
    let clock = DateFormatter()
    clock.locale = .current
    clock.timeZone = utc
    clock.setLocalizedDateFormatFromTemplate("jmm")
    return clock.string(from: date)
}
