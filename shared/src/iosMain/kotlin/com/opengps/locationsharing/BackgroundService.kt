package com.opengps.locationsharing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject

private val locationManager = CLLocationManager()
private var last_time: Long = 0

fun BackgroundService() {
    locationManager.delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            println("location found")
            CoroutineScope(Dispatchers.Main).launch {
                if(Clock.System.now().toEpochMilliseconds() - last_time > 30000) {
                    backgroundTask {  }
                    last_time = Clock.System.now().toEpochMilliseconds()
                }
            }
        }
    }
}