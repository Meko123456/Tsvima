import SwiftUI

@main
struct TsvimaApp: App {
    @StateObject private var store = ForecastStore()

    var body: some Scene {
        WindowGroup {
            ForecastView()
                .environmentObject(store)
        }
    }
}
