package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class LocationValue(val userid: ULong, val coord: Coord, val acc: Float, val timestamp: Long)

var locations by mutableStateOf(mutableMapOf<ULong, List<LocationValue>>())
var latestLocations by mutableStateOf(mutableMapOf<ULong, LocationValue>())
var location by mutableStateOf<LocationValue?>(null)

private suspend fun locationBackend(waypoints: List<Waypoint>, locationValue: LocationValue) {
    users.filter{ it.value.send }.values.forEach { Networking.publishLocation(locationValue, it) }
    location = locationValue
    locations = (
            Networking.receiveLocations()
            ).groupBy { it.userid }.filterKeys { users[it]?.receive?:false }.toMutableMap()
    println(locations.mapValues { it.value.maxBy { it.timestamp } })
    latestLocations = locations.mapValues { it.value.maxBy { it.timestamp } }.toMutableMap()
}

suspend fun backgroundTask(updateNotification: (LocationValue) -> Unit) {
    val platform = getPlatform()
    val waypoints = platform.database.waypointDao().getAll()
    while(Networking.userid == null) {
        delay(1000)
    }
    val locationValue = platform.getLocation()
    if(locationValue != null) {
        updateNotification(locationValue)
        locationBackend(waypoints, locationValue)
    }
}