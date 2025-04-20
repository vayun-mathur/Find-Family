import CoreLocation
import shared

var backgroundActivity: CLBackgroundActivitySession?
var service: CLServiceSession?

func startLocationUpdates() {
    Task {
        do {
            backgroundActivity = CLBackgroundActivitySession()
            service = CLServiceSession(authorization: CLServiceSession.AuthorizationRequirement.always)
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
