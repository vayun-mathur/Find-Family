import SwiftUI
import CoreLocation
import UIKit // Import UIKit for UIApplication
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
            }
        }
    }

    func recreateServiceSessionIfNeeded(launchOptions: [UIApplication.LaunchOptionsKey: Any]?) {
        if let _ = launchOptions?[.location] {
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
}

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate // Connect to AppDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    // Initial setup or check when the view appears
                    let launchOptions = UIApplication.shared.connectedScenes
                        .compactMap { $0 as? UIWindowScene }
                        .flatMap { $0.activationState == .foregroundActive ? $0.session.launchOptions : nil }
                        .first

                    LocationServiceManager.shared.recreateServiceSessionIfNeeded(launchOptions: launchOptions)
                }
                .onChange(of: scenePhase) { newPhase in
                    switch newPhase {
                    case .active:
                        print("App is active")
                        // You might want to ensure the service is running here as well
                        if LocationServiceManager.shared.service == nil {
                            LocationServiceManager.shared.startLocationUpdates()
                        }
                    case .inactive:
                        print("App is inactive")
                        // Consider if you need to do anything here
                    case .background:
                        print("App is in the background")
                        // You might want to invalidate resources if not needed in the background
                        // LocationServiceManager.shared.invalidateServiceSession()
                    @unknown default:
                        print("Unknown scene phase")
                    }
                }
        }
    }
}

struct ContentView: View {
    var body: some View {
        Text("Location Tracking App") // Replace with your actual content
            .onAppear {
                // Ensure location updates start when the content view appears
                // If not already started in .onAppear of iOSApp
                // LocationServiceManager.shared.startLocationUpdates()
            }
    }
}

// Create a basic AppDelegate to capture launch options
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Let the SwiftUI App struct handle the initial setup based on launch options
        return true
    }
}