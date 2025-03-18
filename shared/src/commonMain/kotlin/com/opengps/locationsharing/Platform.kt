package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.Locator
import dev.jordond.compass.geolocation.currentLocationOrNull
import dev.jordond.compass.geolocation.mobile.mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath

abstract class Platform {
    abstract val dataStore: DataStore<Preferences>
    abstract val database: AppDatabase
    @Composable
    abstract fun requestPickContact(callback: (String, String?)->Unit): ()->Unit

    private val geolocator = Geolocator(Locator.mobile())

    suspend fun getLocation(): LocationValue? {
        val location = geolocator.currentLocationOrNull() ?: return null
        return LocationValue(Networking.userid!!, Coord(location.coordinates.latitude, location.coordinates.longitude), location.accuracy.toFloat(), Clock.System.now().toEpochMilliseconds())
    }

    abstract fun runBackgroundService()
}

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "dice.preferences_pb"

expect fun getPlatform(): Platform