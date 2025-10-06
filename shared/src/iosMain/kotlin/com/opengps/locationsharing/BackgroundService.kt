package com.opengps.locationsharing

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlin.time.Clock
import platform.CoreLocation.CLLocation

private var last_time: Long = 0

fun BackgroundService() {
    SuspendScope {
        Networking.init()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun onLocationUpdate(arg: CLLocation, sleep: Boolean) {
    val coords = arg.coordinate.useContents {
        Coord(this.latitude, this.longitude)
    }
    if(Clock.System.now().toEpochMilliseconds() - last_time > SHARE_INTERVAL || sleep) {
        last_time = Clock.System.now().toEpochMilliseconds()
        SuspendScope { backgroundTask(coords, arg.speed.toFloat(), sleep) }
    }
}

fun problem(arg: String) {
    println(arg)
    SuspendScope {
        Networking.problem(arg)
    }
}