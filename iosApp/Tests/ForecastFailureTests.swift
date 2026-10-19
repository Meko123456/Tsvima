import XCTest
@testable import Tsvima

/// Each way a forecast request fails, answered by a stub instead of Open-Meteo, and the sentence the
/// screen then shows. The sentences come from the shared ForecastFailure, so these are the words the
/// Android app shows too; before these tests the iOS side of that mapping had only ever run against
/// a network that worked.
@MainActor
final class ForecastFailureTests: XCTestCase {

    override func tearDown() {
        StubProtocol.answer = nil
        super.tearDown()
    }

    func testNoConnectionSaysToCheckThePhoneIsOnline() async {
        let shown = await failure(answering: .error(URLError(.notConnectedToInternet)))
        XCTAssertEqual(shown, "Couldn't reach the forecast. Check the phone is online, then retry.")
    }

    func testATimeoutAndAMissingHostAreOfflineToo() async {
        for code: URLError.Code in [.timedOut, .cannotFindHost, .networkConnectionLost] {
            let shown = await failure(answering: .error(URLError(code)))
            XCTAssertEqual(shown, "Couldn't reach the forecast. Check the phone is online, then retry.", "\(code)")
        }
    }

    func testAServerErrorSaysWhichAndToWait() async {
        let shown = await failure(answering: .status(503))
        XCTAssertEqual(shown, "The forecast service answered HTTP 503. Try again in a while.")
    }

    func testAnUnreadableBodySaysSo() async {
        let shown = await failure(answering: .body("<html>Maintenance</html>"))
        XCTAssertEqual(shown, "The forecast came back in a form Tsvima can't read.")
    }

    func testAnythingElseIsAPlainCouldNotLoad() async {
        let shown = await failure(answering: .error(URLError(.badServerResponse)))
        XCTAssertEqual(shown, "Couldn't load the forecast.")
    }

    /// Loads the first place through a session that only ever gives [answer], and returns the
    /// failure the screen would show, or nil if it didn't fail.
    private func failure(answering answer: StubProtocol.Answer) async -> String? {
        StubProtocol.answer = answer
        let config = URLSessionConfiguration.ephemeral
        config.protocolClasses = [StubProtocol.self]
        let store = ForecastStore(session: URLSession(configuration: config))
        await store.load()
        if case .failed(let shown) = store.state { return shown }
        return nil
    }
}

/// Answers every request with [answer]: an error, a bare status, or a 200 with a body.
final class StubProtocol: URLProtocol {
    enum Answer {
        case error(Error)
        case status(Int)
        case body(String)
    }

    static var answer: Answer?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        switch Self.answer {
        case .error(let error): client?.urlProtocol(self, didFailWithError: error)
        case .status(let code): respond(code, Data())
        case .body(let text): respond(200, Data(text.utf8))
        case nil: client?.urlProtocol(self, didFailWithError: URLError(.unknown))
        }
    }

    override func stopLoading() {}

    private func respond(_ code: Int, _ data: Data) {
        let response = HTTPURLResponse(url: request.url!, statusCode: code, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: data)
        client?.urlProtocolDidFinishLoading(self)
    }
}
