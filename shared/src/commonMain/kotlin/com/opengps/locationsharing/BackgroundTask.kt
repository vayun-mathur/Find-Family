package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.time.Duration.Companion.days

var locations by mutableStateOf(mutableMapOf<ULong, List<LocationValue>>())
var latestLocations by mutableStateOf(mapOf<ULong, LocationValue>())

private val confirmCount by mutableStateOf(mutableMapOf<ULong, UInt>())
private val confirmType by mutableStateOf(mutableMapOf<ULong, String>())

const val SHARE_INTERVAL = 3000L
private const val CONFIRMATIONS_REQUIRED = 10u

private var counter = 100

private suspend fun locationBackend(locationValue: LocationValue) {
    println("updated location")
    if(locations.isEmpty()) {
        platform.database.locationValueDao().clearBefore((Clock.System.now() - 4.days).toEpochMilliseconds())
        locations = platform.database.locationValueDao().getSince((Clock.System.now() - 2.days).toEpochMilliseconds()).groupBy { it.userid }.toMutableMap()
    }

    val usersDao = platform.database.usersDao()
    var users = usersDao.getAll()
    val waypoints = platform.database.waypointDao().getAll()

    // remove recipients who were temporary and are no longer valid
    users = users.filter { user ->
        if(user.deleteAt != null && user.deleteAt!! < Clock.System.now()) {
            usersDao.delete(user)
            false
        } else true
    }

    if(counter++ == 100) {
        Networking.ensureUserExists()
        counter = 0
    }

    users.filter{ it.send }.forEach { Networking.publishLocation(locationValue, it) }
    val recievedLocations = Networking.receiveLocations() ?: listOf()
    val newLocations = recievedLocations.groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id}?.receive?:false }.mapValues { it.value.sortedBy { it.timestamp } }
    for ((key, value) in newLocations) {
        // If the key already exists, add the new list values to the existing list
        locations[key] = (locations[key] ?: mutableListOf()) + value
        platform.database.locationValueDao().upsertAll(value)
    }
    //println(recievedLocations)
    latestLocations = locations.mapValues { it.value.maxByOrNull { it.timestamp }!! }
    for (user in users) {
        val latest = latestLocations[user.id] ?: continue
        var newUser = user.copy(lastLocationValue = latest)

        // battery level
        if(latest.battery <= 15f && (user.lastBatteryLevel?:100f) > 15f) {
            if(user.id != Networking.userid)
                platform.createNotification(
                    user.name,
                    "${user.name} has low battery",
                    "BATTERY_LOW"
                )
        }
        if(user.lastBatteryLevel != latest.battery) {
            newUser = newUser.copy(lastBatteryLevel = latest.battery)
        }

        val waypointsSubset = waypoints.filter { !it.usersInactive.contains(user.id) }
        // enter or exit waypoints
        val wpIn = waypointsSubset.find { havershine(it.coord, latest.coord) < it.range }
        if(wpIn != null) {
            val wasInEarlier = waypointsSubset.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
            if(newUser.locationName != wpIn.name) {
                newUser = newUser.copy(locationName = wpIn.name, lastLocationChangeTime = Clock.System.now())
                if(wasInEarlier != wpIn) {
                    if(user.id != Networking.userid)
                        platform.createNotification(
                            user.name,
                            "${user.name} has entered ${wpIn.name}",
                            "WAYPOINT_ENTER_EXIT"
                        )
                }
            }
        } else {
            val wasInEarlier = waypointsSubset.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
            if(newUser.locationName != "Unnamed Location") {
                newUser = newUser.copy(locationName = "Unnamed Location", lastLocationChangeTime = Clock.System.now())
                if(wasInEarlier != null) {
                    if(user.id != Networking.userid)
                        platform.createNotification(
                            user.name,
                            "${user.name} has left ${user.locationName}",
                            "WAYPOINT_ENTER_EXIT"
                        )
                }
            }
        }
        usersDao.upsert(newUser)
    }

//    val newBluetoothLocations: MutableMap<BluetoothDevice, LocationValue> = mutableMapOf()
//    val stopScan = platform.startScanBluetoothDevices({ name, rssi ->
//        SuspendScope {
//            val device =
//                platform.database.bluetoothDeviceDao().getFromName(name) ?: return@SuspendScope
//            newBluetoothLocations[device] = locationValue.copy(userid = device.id)
//        }
//    })
//    delay(2000)
//    stopScan()
//    for ((device, newLocation) in newBluetoothLocations) {
//        println(device)
//        platform.database.locationValueDao().upsert(newLocation)
//        locations[device.id] = (locations[device.id] ?: mutableListOf()) + newLocation
//        platform.database.bluetoothDeviceDao()
//            .upsert(device.copy(lastLocationValue = newLocation))
//    }
}

// will be called every SHARE_INTERVAL
suspend fun backgroundTask(location: Coord, speed: Float, sleep: Boolean = false) {
    if(Networking.userid == null) return
    val locationValue = LocationValue(Random.nextULong(), Networking.userid!!, Coord(location.lat, location.lon), speed, 1.0f, Clock.System.now().toEpochMilliseconds(), platform.batteryLevel, sleep)
    locationBackend(locationValue)
}