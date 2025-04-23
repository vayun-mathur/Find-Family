package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class LocationValue(val userid: ULong, val coord: Coord, val speed: Float, val acc: Float, val timestamp: Long, val battery: Float)

var locations by mutableStateOf(mutableMapOf<ULong, MutableList<LocationValue>>())
var latestLocations by mutableStateOf(mutableMapOf<ULong, LocationValue>())
var location by mutableStateOf<LocationValue?>(null)

val confirmCount by mutableStateOf(mutableMapOf<ULong, UInt>())
val confirmType by mutableStateOf(mutableMapOf<ULong, String>())

const val SHARE_INTERVAL = 3000L
const val CONFIRMATIONS_REQUIRED = 10u

var counter = 0;

private suspend fun locationBackend(locationValue: LocationValue) {
    val platform = getPlatform()
    val usersDao = platform.database.usersDao()
    val users = usersDao.getAll()
    val waypoints = platform.database.waypointDao().getAll()

    if(counter++ == 100) {
        Networking.ensureUserExists()
    }

    users.filter{ it.send }.forEach { Networking.publishLocation(locationValue, it) }
    location = locationValue
    val recievedLocations = Networking.receiveLocations() ?: return
    val newLocations = recievedLocations.groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id}?.receive?:false }.mapValues { it.value.sortedBy { it.timestamp } }.toMutableMap()
    for ((key, value) in newLocations) {
        // If the key already exists, add the new list values to the existing list
        locations.getOrPut(key) { mutableListOf() } += value
    }
    latestLocations = locations.mapValues { it.value.last() }.toMutableMap()
    for (user in users) {
        val latestLocation = latestLocations[user.id]
        if(latestLocation != null) {
            usersDao.update(user.id) { it.copy(lastLocationValue = latestLocation) }
        }
    }
    println(latestLocations)
    for((userid, locationHistory) in locations) {
        val user = users.first{it.id == userid}
        val latest = locationHistory.last()

        // battery level
        if(latest.battery <= 15f && (user.lastBatteryLevel?:100f) > 15f) {
            if(userid != Networking.userid)
                platform.createNotification(
                    "${user.name} is running low on battery",
                    "BATTERY_LOW"
                )
        }
        if(user.lastBatteryLevel != latest.battery) {
            usersDao.update(user.id) { it.copy(lastBatteryLevel = latest.battery) }
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
                    if(userid != Networking.userid)
                        platform.createNotification(
                            "${user.name} entered ${wpIn.name}",
                            "WAYPOINT_ENTER_EXIT"
                        )
                    confirmCount[userid] = 0u
                    usersDao.update(user.id) { it.copy(lastCoord = latest.coord) }
                } else {
                    confirmCount[userid] = confirmCount[userid]!! + 1u
                    println("WAYPOINT_ENTER: confirmations: " + confirmCount[userid])
                }
            } else {
                usersDao.update(user.id) { it.copy(lastCoord = latest.coord) }
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
                    if(userid != Networking.userid)
                        platform.createNotification(
                            "${user.name} left ${wasInEarlier.name}",
                            "WAYPOINT_ENTER_EXIT"
                        )
                    confirmCount[userid] = 0u
                    usersDao.update(user.id) { it.copy(lastCoord = latest.coord) }
                } else {
                    confirmCount[userid] = confirmCount[userid]!! + 1u
                    println("WAYPOINT_EXIT: confirmations: " + confirmCount[userid])
                }
            } else {
                usersDao.update(user.id) { it.copy(lastCoord = latest.coord) }
            }
        }
    }
}

// will be called every SHARE_INTERVAL
suspend fun backgroundTask(location: Coord, speed: Float) {
    if(Networking.userid == null) return
    val locationValue = LocationValue(Networking.userid!!, Coord(location.lat, location.lon), speed, 1.0f, Clock.System.now().toEpochMilliseconds(), getPlatform().batteryLevel)
    locationBackend(locationValue)
}