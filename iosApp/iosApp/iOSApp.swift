import SwiftUI
import CoreLocation
import shared

// Shared state that manages the `CLLocationManager` and `CLBackgroundActivitySession`.
@MainActor class LocationsHandler: ObservableObject {

	static let shared = LocationsHandler()  // Create a single, shared instance of the object.
	private let manager: CLLocationManager
	private var background: CLBackgroundActivitySession?
	private var session: CLServiceSession?

	private init() {
		self.manager = CLLocationManager()  // Creating a location manager instance is safe to call here in `MainActor`.
		self.manager.allowsBackgroundLocationUpdates = true
	}

	func startLocationUpdates() {
		if self.manager.authorizationStatus == .notDetermined {
			self.manager.requestWhenInUseAuthorization()
		}
		print("📍 [App] Starting location updates")
        self.session = CLServiceSession(authorization: CLServiceSession.AuthorizationRequirement.always)
		Task {
			do {
				let updates = CLLocationUpdate.liveUpdates()
				for try await update in updates {
					if let loc = update.location {
                        BackgroundServiceKt.onLocationUpdate(arg: loc, sleep: (update.stationary || update.insufficientlyInUse))
					}
//                    print(update.location == nil)
//                    
//                    if update.authorizationDenied {
//                        BackgroundServiceKt.problem(arg: "Auth denied")
//                    }
//                    if update.authorizationDeniedGlobally {
//                        BackgroundServiceKt.problem(arg: "Auth denied globally")
//                    }
//                    if update.authorizationRequestInProgress {
//                        BackgroundServiceKt.problem(arg: "Auth in progress")
//                    }
//                    if update.authorizationRestricted {
//                        BackgroundServiceKt.problem(arg: "Auth restricted")
//                    }
//                    if update.insufficientlyInUse {
//                        BackgroundServiceKt.problem(arg: "Insufficient Use")
//                    }
//                    if update.locationUnavailable {
//                        BackgroundServiceKt.problem(arg: "Location Unavailable")
//                    }
//                    if update.serviceSessionRequired {
//                        BackgroundServiceKt.problem(arg: "Service Session required")
//                    }
//                    if update.stationary {
//                        BackgroundServiceKt.problem(arg: "Stationary")
//                    }
				}
			} catch {
				print("💥 [App] Could not start location updates")
			}
			return
		}
        self.background = CLBackgroundActivitySession()
	}

	func stopLocationUpdates() {
		print("🛑 [App] Stopping location updates")
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
        return true
    }
}

@main
struct iosApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(LocationsHandler.shared)
        }
    }
}
