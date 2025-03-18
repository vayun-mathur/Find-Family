//
//  location.swift
//  iosApp
//
//  Created by Madhulika Sachdeva on 23/3/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import CoreLocation
import shared

private var backgroundActivity: CLBackgroundActivitySession?

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
