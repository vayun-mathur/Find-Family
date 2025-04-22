import SwiftUI
import CoreLocation
import UIKit
import shared

class LocationServiceManager: NSObject, ObservableObject {
    static let shared = LocationServiceManager() // Singleton for easy access

    var backgroundActivity: CLBackgroundActivitySession?
    var service: CLServiceSession?

    private override init() {} // Make it a true singleton

    func startLocationUpdates() {
        Task {
            do {
                backgroundActivity = CLBackgroundActivitySession()
                service = CLServiceSession(authorization: .always)
                let updates = CLLocationUpdate.liveUpdates()
                for try await update in updates {
                    if let location = update.location {
                        BackgroundServiceKt.onLocationUpdate(arg: location)
                    }
                }
            } catch {
                debugPrint("Error in location updates: \(error)")
                // Consider more robust error handling
                // You might want to attempt to restart the session here in case of an error
            }
        }
    }

    func recreateServiceSessionIfNeeded(isLaunchedFromLocationEvent: Bool) {
        if isLaunchedFromLocationEvent {
            print("App launched in the background due to a location event. Recreating CLServiceSession.")
            // Invalidate the old session if it exists
            service?.invalidate()
            service = nil
            startLocationUpdates() // Re-initiate the location updates
        } else if service == nil {
            print("App launched in the foreground or service is nil. Starting location updates.")
            startLocationUpdates()
        }
    }

    func invalidateServiceSession() {
        print("Invalidating CLServiceSession.")
        service?.invalidate()
        service = nil
        backgroundActivity = nil
    }
    func ensureServiceSessionIsRunning() {
        if service == nil {
            print("Service is nil. Starting location updates.")
            startLocationUpdates()
        }
    }
}

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate // Connect to AppDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { newPhase in
                    switch newPhase {
                    case .active:
                        print("App is active")
                        LocationServiceManager.shared.ensureServiceSessionIsRunning()
                    case .inactive:
                        print("App is inactive")
                    case .background:
                        print("App is in the background")
                        LocationServiceManager.shared.ensureServiceSessionIsRunning()
                    @unknown default:
                        print("Unknown scene phase")
                    }
                }
        }
    }
}

struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        startLocationUpdates()
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
        LocationServiceManager.shared.recreateServiceSessionIfNeeded(isLaunchedFromLocationEvent: isLaunchedFromLocationEvent)
        return true
    }
}