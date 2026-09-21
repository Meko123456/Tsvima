import Foundation
import Shared

/// Fetches an Open-Meteo forecast and hands it to the shared Kotlin core to make sense of.
///
/// Everything this screen asserts — what the response means, which hours are still ahead of you,
/// what the next few are worth and whether it is about to rain — comes out of `:shared`, and is
/// covered by the same tests that already run on this simulator's own Kotlin/Native target. Swift
/// does the one job that is genuinely the platform's: it makes the HTTPS request.
///
/// **This fetches live data.** Open-Meteo is free and key-less, so there is no secret to keep out
/// of the repo and no excuse for shipping a canned response dressed up as a forecast — a screenshot
/// of bundled JSON would claim something the app cannot do. The price of that choice is that the
/// screen is honest when it fails: no network means the error state, not a stale sample.
@MainActor
final class ForecastStore: ObservableObject {

    /// What the shared core made of one response, ready for the view to draw.
    struct Snapshot {
        let place: Place
        let forecast: Forecast
        /// Hours at or after the current hour. The full timeline starts at the location's midnight.
        let upcoming: [HourlyPoint]
        /// `GoOutScore` over the first up-to-3 upcoming hours.
        let score: Int32
        let verdict: String
        /// First upcoming hour that reads as rain, by the thresholds both apps share.
        let nextRain: HourlyPoint?
    }

    enum State {
        case loading
        case loaded(Snapshot)
        case failed(String)
    }

    /// A short fixed list rather than device location or the geocoding search the Android app has.
    ///
    /// Location would put a permission sheet between launch and the only thing this version is
    /// trying to show, and the spread here is the point: four places in four different climates and
    /// four different UTC offsets exercise both the score's whole range and the location-clock
    /// handling that `Forecast.utcOffsetSeconds` exists for.
    let places: [Place] = [
        Place(name: "Tbilisi", latitude: 41.7151, longitude: 44.8271, admin1: nil, country: "Georgia"),
        Place(name: "Batumi", latitude: 41.6168, longitude: 41.6367, admin1: "Adjara", country: "Georgia"),
        Place(name: "London", latitude: 51.5072, longitude: -0.1276, admin1: "England", country: "United Kingdom"),
        Place(name: "Reykjavík", latitude: 64.1466, longitude: -21.9426, admin1: nil, country: "Iceland"),
    ]

    @Published var placeIndex: Int = 0
    @Published private(set) var state: State = .loading

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 20
        return URLSession(configuration: config)
    }()

    func load() async {
        let place = places[placeIndex]
        state = .loading

        guard let url = forecastURL(for: place) else {
            state = .failed("Could not build the request URL.")
            return
        }
        var request = URLRequest(url: url)
        request.setValue("Tsvima/0.1", forHTTPHeaderField: "User-Agent")

        do {
            let (data, response) = try await session.data(for: request)
            // The view drives this with `.task(id:)`, so changing place cancels the load in flight
            // and starts another. Whatever this one came back with belongs to the previous place.
            guard !Task.isCancelled else { return }
            if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                state = .failed("Open-Meteo returned HTTP \(http.statusCode).")
                return
            }
            guard let body = String(data: data, encoding: .utf8) else {
                state = .failed("The response was not text.")
                return
            }
            // The one line this whole app exists to prove: a native fetch, parsed by shared Kotlin.
            guard let forecast = ForecastParser.shared.parse(body: body) else {
                state = .failed("Unexpected forecast format.")
                return
            }
            // The hour filter is the location's-clock rule, not this device's — see `Upcoming`.
            let upcoming = Upcoming.shared.hoursAheadNow(
                hourly: forecast.hourly,
                utcOffsetSeconds: forecast.utcOffsetSeconds
            )
            let score = GoOutScore.shared.score(upcoming: upcoming)
            state = .loaded(
                Snapshot(
                    place: place,
                    forecast: forecast,
                    upcoming: upcoming,
                    score: score,
                    verdict: GoOutScore.shared.verdict(score: score),
                    nextRain: Upcoming.shared.nextRain(upcoming: upcoming)
                )
            )
        } catch {
            // Same reason, on the throwing path: a cancelled request reports failure *after* the
            // new place's load has already set .loading, and would leave an error on screen that
            // nothing is coming to clear.
            guard !Task.isCancelled else { return }
            state = .failed(error.localizedDescription)
        }
    }

    private func forecastURL(for place: Place) -> URL? {
        // The same query the Android `OpenMeteoClient` sends, so both apps parse the same shape.
        var components = URLComponents(string: "https://api.open-meteo.com/v1/forecast")
        components?.queryItems = [
            URLQueryItem(name: "latitude", value: String(place.latitude)),
            URLQueryItem(name: "longitude", value: String(place.longitude)),
            URLQueryItem(
                name: "hourly",
                value: "precipitation_probability,precipitation,temperature_2m,wind_speed_10m"
            ),
            URLQueryItem(name: "forecast_days", value: "2"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        return components?.url
    }
}
