package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class LocationValue(val userid: ULong, val coord: Coord, val acc: Float, val timestamp: Long)

var locations by mutableStateOf(mutableMapOf<ULong, List<LocationValue>>())
var latestLocations by mutableStateOf(mutableMapOf<ULong, LocationValue>())
var location by mutableStateOf<LocationValue?>(null)

const val SHARE_INTERVAL = 3000L

private suspend fun locationBackend(waypoints: List<Waypoint>, locationValue: LocationValue) {
    val users = getPlatform().database.usersDao().getAll()
    users.filter{ it.send }.forEach { Networking.publishLocation(locationValue, it) }
    location = locationValue
    locations = (
            Networking.receiveLocations()
            ).groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id}?.receive?:false }.toMutableMap()
    latestLocations = locations.mapValues { it.value.maxBy { it.timestamp } }.toMutableMap()
    println(latestLocations)
}

suspend fun backgroundTask(location: Coord, updateNotification: (LocationValue) -> Unit) {
    val platform = getPlatform()
    val waypoints = platform.database.waypointDao().getAll()
    while(Networking.userid == null) {
        delay(1000)
    }
    val locationValue = LocationValue(Networking.userid!!, Coord(location.lat, location.lon), 1.0f, Clock.System.now().toEpochMilliseconds())
    updateNotification(locationValue)
    locationBackend(waypoints, locationValue)
}