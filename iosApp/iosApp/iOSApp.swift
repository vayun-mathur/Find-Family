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
		Task {
			do {
				self.background = CLBackgroundActivitySession()
				self.session = CLServiceSession()
				let updates = CLLocationUpdate.liveUpdates()
				for try await update in updates {
					if let loc = update.location {
					    print("new location")
                        BackgroundServiceKt.onLocationUpdate(arg: loc)
					}
				}
			} catch {
				print("💥 [App] Could not start location updates")
			}
			return
		}
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