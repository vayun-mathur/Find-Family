import CoreLocation
import shared

var backgroundActivity: CLBackgroundActivitySession?

func startLocationUpdates() {
    Task {
        do {
            backgroundActivity = CLBackgroundActivitySession()
            let updates = CLLocationUpdate.liveUpdates()
            for try await update in updates {
                if(update.location != nil) {
                    BackgroundServiceKt.onLocationUpdate(arg: update.location!);
                }
            }
        } catch {
            debugPrint("error")
        }
    }
}
