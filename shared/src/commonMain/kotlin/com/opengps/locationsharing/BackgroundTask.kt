package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class LocationValue(val userid: ULong, val coord: Coord, val acc: Float, val timestamp: Long, val battery: Float)

var locations by mutableStateOf(mutableMapOf<ULong, MutableList<LocationValue>>())
var latestLocations by mutableStateOf(mutableMapOf<ULong, LocationValue>())
var location by mutableStateOf<LocationValue?>(null)

val confirmCount by mutableStateOf(mutableMapOf<ULong, UInt>())
val confirmType by mutableStateOf(mutableMapOf<ULong, String>())

const val SHARE_INTERVAL = 3000L
const val CONFIRMATIONS_REQUIRED = 10u

private suspend fun locationBackend(locationValue: LocationValue) {
    val platform = getPlatform()
    val users = platform.database.usersDao().getAll()
    val waypoints = platform.database.waypointDao().getAll()

    Networking.ensureUserExists()

    users.filter{ it.send }.forEach { Networking.publishLocation(locationValue, it) }
    location = locationValue
    val recievedLocations = Networking.receiveLocations() ?: return
    val newLocations = recievedLocations.groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id}?.receive?:false }.mapValues { it.value.sortedBy { it.timestamp } }.toMutableMap()
    for ((key, value) in newLocations) {
        // If the key already exists, add the new list values to the existing list
        locations.getOrPut(key) { mutableListOf() } += value
    }
    latestLocations = locations.mapValues { it.value.last() }.toMutableMap()
    println(recievedLocations)
    println(latestLocations)
    for((userid, locationHistory) in locations) {
        if(userid == Networking.userid) continue
        val user = users.first{it.id == userid}
        val latest = locationHistory.last()

        // battery level
        if(latest.battery <= 15f && (user.lastBatteryLevel?:100f) > 15f) {
            platform.createNotification(
                "${user.name} is running low on battery",
                "BATTERY_LOW"
            )
        }
        if(user.lastBatteryLevel != latest.battery) {
            user.lastBatteryLevel = latest.battery
            platform.database.usersDao().upsert(user)
        }

        val waypointsSubset = waypoints.filter { !it.usersInactive.contains(userid) }
        // enter or exit waypoints
        val wpIn = waypointsSubset.find { havershine(it.coord, latest.coord) < it.range }
        if(wpIn != null) {
            val wasInEarlier = havershine(user.lastCoord?:Coord(0.0,0.0), wpIn.coord) < wpIn.range
            if(!wasInEarlier) {
                // entered waypoint
                if(confirmType.getOrPut(userid){"enter"} != "enter") {
                    confirmType[userid] = "enter"
                    confirmCount[userid] = 0u
                }
                if(confirmCount.getOrPut(userid){0u} == CONFIRMATIONS_REQUIRED) {
                    platform.createNotification(
                        "${user.name} entered ${wpIn.name}",
                        "WAYPOINT_ENTER_EXIT"
                    )
                    confirmCount[userid] = 0u
                    user.lastCoord = latest.coord
                    platform.database.usersDao().upsert(user)
                } else {
                    confirmCount[userid] = confirmCount[userid]!! + 1u
                    println("WAYPOINT_ENTER: confirmations: " + confirmCount[userid])
                }
            } else {
                user.lastCoord = latest.coord
                platform.database.usersDao().upsert(user)
            }
        } else {
            val wasInEarlier = waypointsSubset.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
            if(wasInEarlier != null) {
                // exited waypoints
                if(confirmType.getOrPut(userid){"exit"} != "exit") {
                    confirmType[userid] = "exit"
                    confirmCount[userid] = 0u
                }
                if(confirmCount.getOrPut(userid){0u} == CONFIRMATIONS_REQUIRED) {
                    platform.createNotification(
                        "${user.name} left ${wasInEarlier.name}",
                        "WAYPOINT_ENTER_EXIT"
                    )
                    confirmCount[userid] = 0u
                    user.lastCoord = latest.coord
                    platform.database.usersDao().upsert(user)
                } else {
                    confirmCount[userid] = confirmCount[userid]!! + 1u
                    println("WAYPOINT_EXIT: confirmations: " + confirmCount[userid])
                }
            } else {
                user.lastCoord = latest.coord
                platform.database.usersDao().upsert(user)
            }
        }
    }
}

// will be called every SHARE_INTERVAL
suspend fun backgroundTask(location: Coord) {
    if(Networking.userid == null) return
    val locationValue = LocationValue(Networking.userid!!, Coord(location.lat, location.lon), 1.0f, Clock.System.now().toEpochMilliseconds(), getPlatform().batteryLevel)
    locationBackend(locationValue)
}