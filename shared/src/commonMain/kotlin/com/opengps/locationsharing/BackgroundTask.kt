package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class LocationValue(val userid: ULong, val coord: Coord, val acc: Float, val timestamp: Long)

var locations by mutableStateOf(mutableMapOf<ULong, List<LocationValue>>())
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
    locations = (
            Networking.receiveLocations()
            ).groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id}?.receive?:false }.mapValues { it.value.sortedBy { it.timestamp } }.toMutableMap()
    latestLocations = locations.mapValues { it.value.last() }.toMutableMap()
    println(latestLocations)
    for((userid, locationHistory) in locations) {
        val user = users.first{it.id == userid}
        val latest = locationHistory.last()
        val wpIn = waypoints.find { havershine(it.coord, latest.coord) < it.range }
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
                        "${user.name} entered waypoint ${wpIn.name}",
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
            val wasInEarlier = waypoints.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
            if(wasInEarlier != null) {
                // exited waypoints
                if(confirmType.getOrPut(userid){"exit"} != "exit") {
                    confirmType[userid] = "exit"
                    confirmCount[userid] = 0u
                }
                if(confirmCount.getOrPut(userid){0u} == CONFIRMATIONS_REQUIRED) {
                    platform.createNotification(
                        "Exited waypoint ${wasInEarlier.name}",
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
    val locationValue = LocationValue(Networking.userid!!, Coord(location.lat, location.lon), 1.0f, Clock.System.now().toEpochMilliseconds())
    locationBackend(locationValue)
}