package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jordond.compass.Coordinates
import dev.jordond.compass.geocoder.Geocoder
import dev.jordond.compass.geocoder.placeOrNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.random.Random
import kotlin.random.nextULong
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

var locations by mutableStateOf(mutableMapOf<ULong, List<LocationValue>>())
var latestLocations by mutableStateOf(mapOf<ULong, LocationValue>())

const val SHARE_INTERVAL = 3000L

private var counter = 100

@OptIn(ExperimentalTime::class)
suspend fun checkSharingRequests() {
    // retrieve requests
    Networking.retrieveRequestsOfMe().map {
        User(it.decodeBase26(), it, null, "", false, RequestStatus.AWAITING_REQUEST, null, null)
    }.forEach {
        platform.database.usersDao().upsert(it)
        platform.createNotification("Your Location Requested", "by ${it.id.encodeBase26()}", channelId = "SHARING_REQUEST")
    }
}

@OptIn(ExperimentalTime::class)
private suspend fun locationBackend(locationValue: LocationValue) {
    println("updated location")
    if(locations.isEmpty()) {
        locations = platform.database.locationValueDao().getSince((Clock.System.now() - 2.days).toEpochMilliseconds()).groupBy { it.userid }.toMutableMap()
    }

    checkSharingRequests()

    val usersDao = platform.database.usersDao()
    val geocoder = Geocoder()
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

    val newLocations = recievedLocations.groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id} != null }.mapValues { it.value.sortedBy { it.timestamp } }
    for ((key, value) in newLocations) {
        // If the key already exists, add the new list values to the existing list
        locations[key] = (locations[key] ?: mutableListOf()) + value
        platform.database.locationValueDao().upsertAll(value)
    }
    users = usersDao.getAll()
    latestLocations = locations.mapValues { it.value.maxByOrNull { it.timestamp }!! }
    for (user in users) {
        val latest = latestLocations[user.id] ?: continue
        var newUser = user.copy(lastLocationValue = latest)
        if(newUser.requestStatus == RequestStatus.AWAITING_RESPONSE) {
            newUser = newUser.copy(requestStatus = RequestStatus.MUTUAL_CONNECTION)
        }

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

        val geocoderResult = geocoder.placeOrNull(Coordinates(latest.coord.lat, latest.coord.lon))?.let {
            it.name ?: "${it.subThoroughfare} ${it.thoroughfare}"
        }

        val locationName = wpIn?.name ?: geocoderResult ?: "Unnamed Location"

        if(locationName != newUser.locationName) {
            newUser = newUser.copy(locationName = locationName, lastLocationChangeTime = Clock.System.now(), lastCoord = latest.coord)
            if(wpIn != null && user.id != Networking.userid)
                platform.createNotification(
                    user.name,
                    "${user.name} has entered ${wpIn.name}",
                    "WAYPOINT_ENTER_EXIT"
                )

        }

//        if(wpIn != null) {
//            val wasInEarlier = waypointsSubset.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
//            if(newUser.locationName != wpIn.name) {
//                newUser = newUser.copy(locationName = wpIn.name, lastLocationChangeTime = Clock.System.now(), lastCoord = latest.coord)
//                if(wasInEarlier != wpIn) {
//                    if(user.id != Networking.userid)
//                        platform.createNotification(
//                            user.name,
//                            "${user.name} has entered ${wpIn.name}",
//                            "WAYPOINT_ENTER_EXIT"
//                        )
//                }
//            }
//        } else {
//            val wasInEarlier = waypointsSubset.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
//            if(newUser.locationName != "Unnamed Location") {
//                newUser = newUser.copy(locationName = "Unnamed Location", lastLocationChangeTime = Clock.System.now(), lastCoord = latest.coord)
//                if(wasInEarlier != null) {
//                    if(user.id != Networking.userid)
//                        platform.createNotification(
//                            user.name,
//                            "${user.name} has left ${user.locationName}",
//                            "WAYPOINT_ENTER_EXIT"
//                        )
//                }
//            }
//        }
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

@OptIn(ExperimentalTime::class)
private var lastCalled = Instant.fromEpochMilliseconds(0)

// will be called every SHARE_INTERVAL
@OptIn(ExperimentalTime::class)
suspend fun backgroundTask(location: Coord, speed: Float, sleep: Boolean = false) {
    if(Networking.userid == null) return
    if(Clock.System.now() - lastCalled < SHARE_INTERVAL.milliseconds*0.8) return
    lastCalled = Clock.System.now()
    val locationValue = LocationValue(Random.nextULong(), Networking.userid!!, Coord(location.lat, location.lon), speed, 1.0f, Clock.System.now().toEpochMilliseconds(), platform.batteryLevel, sleep)
    locationBackend(locationValue)
}