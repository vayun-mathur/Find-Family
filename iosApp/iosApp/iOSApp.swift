import SwiftUI
import CoreLocation
import UIKit
import shared
import OSLog

// Shared state that manages the `CLLocationManager` and `CLBackgroundActivitySession`.
@MainActor class LocationsHandler: ObservableObject {

	static let shared = LocationsHandler()  // Create a single, shared instance of the object.
	private let manager: CLLocationManager
	private var background: CLBackgroundActivitySession?

	@Published
	var updatesStarted: Bool = UserDefaults.standard.bool(forKey: "liveUpdatesStarted") {
		didSet { UserDefaults.standard.set(updatesStarted, forKey: "liveUpdatesStarted") }
	}

	@Published
	var backgroundActivity: Bool = UserDefaults.standard.bool(forKey: "BGActivitySessionStarted") {
		didSet {
			backgroundActivity ? self.background = CLBackgroundActivitySession() : self.background?.invalidate()
			UserDefaults.standard.set(backgroundActivity, forKey: "BGActivitySessionStarted")
		}
	}

	private init() {
		self.manager = CLLocationManager()  // Creating a location manager instance is safe to call here in `MainActor`.
		self.manager.allowsBackgroundLocationUpdates = true
		locationsArray = [CLLocation]()
	}

	func startLocationUpdates() {
		if self.manager.authorizationStatus == .notDetermined {
			self.manager.requestWhenInUseAuthorization()
		}
		Logger.services.info("📍 [App] Starting location updates")
		Task {
			do {
				self.updatesStarted = true
				let updates = CLLocationUpdate.liveUpdates()
				for try await update in updates {
					if !self.updatesStarted { break }
					if let loc = update.location {
					    print("new location")
                        BackgroundServiceKt.onLocationUpdate(arg: loc)
					}
				}
			} catch {
				Logger.services.error("💥 [App] Could not start location updates: \(error.localizedDescription, privacy: .public)")
			}
			return
		}
	}

	func stopLocationUpdates() {
		Logger.services.info("🛑 [App] Stopping location updates")
		self.updatesStarted = false
	}

	static let DefaultLocation = CLLocationCoordinate2D(latitude: 37.3346, longitude: -122.0090)
	static var currentLocation: CLLocationCoordinate2D {
		guard let location = shared.manager.location else {
			return DefaultLocation
		}
		return location.coordinate
	}

	static var satsInView: Int {
		var sats = 0
		if let newLocation = shared.locationsArray.last {
			sats = 1
			if newLocation.verticalAccuracy > 0 {
				sats = 4
				if 0...5 ~= newLocation.horizontalAccuracy {
					sats = 12
				} else if 6...15 ~= newLocation.horizontalAccuracy {
					sats = 10
				} else if 16...30 ~= newLocation.horizontalAccuracy {
					sats = 9
				} else if 31...45 ~= newLocation.horizontalAccuracy {
					sats = 7
				} else if 46...60 ~= newLocation.horizontalAccuracy {
					sats = 5
				}
			} else if newLocation.verticalAccuracy < 0 && 60...300 ~= newLocation.horizontalAccuracy {
				sats = 3
			} else if newLocation.verticalAccuracy < 0 && newLocation.horizontalAccuracy > 300 {
				sats = 2
			}
		}
		return sats
	}

}

struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Platform_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    var body: some View {
        ComposeViewController()
    }
}

// Create a basic AppDelegate to capture launch options
class AppDelegate: NSObject, UIApplicationDelegate {
    var isLaunchedFromLocationEvent = false
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Check if the app was launched due to a location event
        isLaunchedFromLocationEvent = launchOptions?[.location] != nil
        print("App launched due to \(isLaunchedFromLocationEvent)")
        LocationsHandler.shared.startLocationUpdates()
        if LocationsHandler.shared.backgroundActivity {
            LocationsHandler.shared.backgroundActivity = true
        }
        return true
    }
}