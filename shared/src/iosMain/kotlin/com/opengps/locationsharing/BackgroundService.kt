package com.opengps.locationsharing

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.CoreLocation.CLLocation

private var last_time: Long = 0

fun BackgroundService() {
}

@OptIn(ExperimentalForeignApi::class)
fun onLocationUpdate(arg: CLLocation) {
    println("location found" + Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    val coords = arg.coordinate.useContents {
        Coord(this.latitude, this.longitude)
    }
    CoroutineScope(Dispatchers.Main).launch {
        if(Clock.System.now().toEpochMilliseconds() - last_time > SHARE_INTERVAL) {
            last_time = Clock.System.now().toEpochMilliseconds()
            backgroundTask(coords) {  }
        }
    }
}