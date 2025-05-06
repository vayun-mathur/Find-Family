package com.opengps.locationsharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.random.nextULong

var locations by mutableStateOf(mutableMapOf<ULong, List<LocationValue>>())
var latestLocations by mutableStateOf(mapOf<ULong, LocationValue>())

private val confirmCount by mutableStateOf(mutableMapOf<ULong, UInt>())
private val confirmType by mutableStateOf(mutableMapOf<ULong, String>())

const val SHARE_INTERVAL = 30000L
private const val CONFIRMATIONS_REQUIRED = 10u

private var counter = 100

private suspend fun locationBackend(locationValue: LocationValue) {
    println("updated location")
    if(locations.isEmpty()) {
        locations = platform.database.locationValueDao().getAll().groupBy { it.userid }.toMutableMap()
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
    val recievedLocations = Networking.receiveLocations() ?: return
    val newLocations = recievedLocations.groupBy { it.userid }.filterKeys { id -> users.firstOrNull{it.id == id}?.receive?:false }.mapValues { it.value.sortedBy { it.timestamp } }
    println(newLocations)
    for ((key, value) in newLocations) {
        // If the key already exists, add the new list values to the existing list
        locations[key] = (locations[key] ?: mutableListOf()) + value
        platform.database.locationValueDao().upsertAll(value)
    }
    latestLocations = locations.mapValues { it.value.maxByOrNull { it.timestamp }!! }
    for (user in users) {
        val latest = latestLocations[user.id] ?: continue
        var newUser = user.copy(lastLocationValue = latest)
        println(timestring(latest.timestamp))

        // battery level
        if(latest.battery <= 15f && (user.lastBatteryLevel?:100f) > 15f) {
            if(user.id != Networking.userid)
                platform.createNotification(
                    "${user.name} is running low on battery",
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
            val wasInEarlier = havershine(user.lastCoord?:Coord(0.0,0.0), wpIn.coord) < wpIn.range
            if(!wasInEarlier) {
                // entered waypoint
                if(confirmType.getOrPut(user.id){"enter"} != "enter") {
                    confirmType[user.id] = "enter"
                    confirmCount[user.id] = 0u
                }
                if(confirmCount.getOrPut(user.id){0u} == CONFIRMATIONS_REQUIRED) {
                    if(user.id != Networking.userid)
                        platform.createNotification(
                            "${user.name} entered ${wpIn.name}",
                            "WAYPOINT_ENTER_EXIT"
                        )
                    confirmCount[user.id] = 0u
                    newUser = newUser.copy(lastCoord = latest.coord, locationName = wpIn.name, lastLocationChangeTime = Clock.System.now())
                } else {
                    confirmCount[user.id] = confirmCount[user.id]!! + 1u
                    println("WAYPOINT_ENTER: confirmations: " + confirmCount[user.id])
                }
            } else {
                newUser = newUser.copy(lastCoord = latest.coord)
            }
        } else {
            val wasInEarlier = waypointsSubset.find { havershine(it.coord, user.lastCoord?:Coord(0.0,0.0)) < it.range }
            if(wasInEarlier != null) {
                // exited waypoints
                if(confirmType.getOrPut(user.id){"exit"} != "exit") {
                    confirmType[user.id] = "exit"
                    confirmCount[user.id] = 0u
                }
                if(confirmCount.getOrPut(user.id){0u} == CONFIRMATIONS_REQUIRED) {
                    if(user.id != Networking.userid)
                        platform.createNotification(
                            "${user.name} left ${wasInEarlier.name}",
                            "WAYPOINT_ENTER_EXIT"
                        )
                    confirmCount[user.id] = 0u
                    newUser = newUser.copy(lastCoord = latest.coord, locationName = "Unnamed Location", lastLocationChangeTime = Clock.System.now())
                } else {
                    confirmCount[user.id] = confirmCount[user.id]!! + 1u
                    println("WAYPOINT_EXIT: confirmations: " + confirmCount[user.id])
                }
            } else {
                newUser = newUser.copy(lastCoord = latest.coord)
            }
        }
        usersDao.upsert(newUser)
    }


    // TODO: update bluetooth device locations
    if(bluetoothscancounter % 10 == 0) {
        val newBluetoothLocations: MutableMap<BluetoothDevice, LocationValue> = mutableMapOf()
        val stopScan = platform.startScanBluetoothDevices({ mac, rssi ->
            SuspendScope {
                val device =
                    platform.database.bluetoothDeviceDao().getFromMac(mac) ?: return@SuspendScope
                newBluetoothLocations[device] = locationValue.copy(userid = device.id)
            }
        })
        delay(500)
        stopScan()
        // TODO: update bluetooth device locations
        for ((device, newLocation) in newBluetoothLocations) {
            println(device)
            platform.database.locationValueDao().upsert(newLocation)
            locations[device.id] = (locations[device.id] ?: mutableListOf()) + newLocation
            platform.database.bluetoothDeviceDao()
                .upsert(device.copy(lastLocationValue = newLocation))
        }
    }
    bluetoothscancounter++
}

var bluetoothscancounter = 0

// will be called every SHARE_INTERVAL
suspend fun backgroundTask(location: Coord, speed: Float) {
    if(Networking.userid == null) return
    val locationValue = LocationValue(Random.nextULong(), Networking.userid!!, Coord(location.lat, location.lon), speed, 1.0f, Clock.System.now().toEpochMilliseconds(), platform.batteryLevel)
    locationBackend(locationValue)
}