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

private var latestLocations by mutableStateOf(mapOf<ULong, LocationValue>())

const val SHARE_INTERVAL = 3000L

private var counter = 100

@OptIn(ExperimentalTime::class)
suspend fun checkSharingRequests() {
    // retrieve requests
    Networking.retrieveRequestsOfMe().map {
        User(it.toULong(), it.toULong().encodeBase26(), null, "", false, RequestStatus.AWAITING_REQUEST, null)
    }.forEach {
        platform.database.usersDao().upsert(it)
        platform.createNotification("Your Location Requested", "by ${it.id.encodeBase26()}", channelId = "SHARING_REQUEST")
    }
}

@OptIn(ExperimentalTime::class)
private suspend fun locationBackend(locationValue: LocationValue) {
    val usersDao = platform.database.usersDao()

    if(latestLocations.isEmpty()) {
        latestLocations = platform.database.locationValueDao().getAllLatestLocations().associateBy { it.userid }
        println("got locations")
    }

    checkSharingRequests()

    val geocoder = Geocoder()
    val waypoints = platform.database.waypointDao().getAll()

    platform.database.usersDao().deleteExpiredUsers(Clock.System.now())

    if(counter++ == 100) {
        Networking.ensureUserExists()
        counter = 0
    }

    val users1 = usersDao.getAll()

    users1.filter{it.send}.forEach {
        Networking.publishLocation(locationValue, it)
    }

    val receivedLocations = Networking.receiveLocations() ?: listOf()

    val newLocations = receivedLocations.groupBy { it.userid }.filterKeys { id -> usersDao.getByID(id) != null }.mapValues { it.value.sortedBy { it.timestamp } }
    latestLocations = latestLocations + newLocations.mapValues { it.value.last() }
    platform.database.locationValueDao().upsertAll(newLocations.values.flatten())

    for (user in users1) {
        val latest = latestLocations[user.id] ?: continue
        if(user.requestStatus == RequestStatus.AWAITING_RESPONSE) {
            usersDao.update(user.id) { it.copy(requestStatus = RequestStatus.MUTUAL_CONNECTION) }
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
            usersDao.update(user.id) { it.copy(lastBatteryLevel = latest.battery) }
        }

        val waypointsSubset = waypoints.filter { !it.usersInactive.contains(user.id) }
        // enter or exit waypoints
        val wpIn = waypointsSubset.find { havershine(it.coord, latest.coord) < it.range }

        val geocoderResult = geocoder.placeOrNull(Coordinates(latest.coord.lat, latest.coord.lon))?.let {
            it.name ?: "${it.subThoroughfare} ${it.thoroughfare}"
        }

        val locationName = wpIn?.name ?: geocoderResult ?: "Unnamed Location"

        if(locationName != user.locationName) {
            usersDao.update(user.id) { it.copy(locationName = locationName, lastLocationChangeTime = Clock.System.now()) }
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
suspend fun backgroundTask(location: Coord, speed: Float, accuracy: Float, sleep: Boolean = false) {
    if(Networking.userid == null) {
        Networking.init()
    }
    if(Clock.System.now() - lastCalled < SHARE_INTERVAL.milliseconds*0.8) return
    lastCalled = Clock.System.now()
    val locationValue = LocationValue(Random.nextULong(), Networking.userid!!, Coord(location.lat, location.lon), speed, accuracy, Clock.System.now().toEpochMilliseconds(), platform.batteryLevel, sleep)
    locationBackend(locationValue)
}