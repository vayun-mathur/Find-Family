package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.statement.HttpResponse
import okio.Path.Companion.toPath

abstract class Platform {

    abstract val dataStore: DataStore<Preferences>
    val dataStoreUtils = DataStoreUtils { this.dataStore }
    abstract val database: AppDatabase
    @Composable
    abstract fun requestPickContact(callback: (String, String?)->Unit): ()->Unit

    abstract fun runBackgroundService()
    abstract fun createNotification(s: String, channelId: String)

    abstract suspend fun torDNSChecker(inner: suspend ()-> HttpResponse): HttpResponse?

    abstract val batteryLevel: Float
}

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "dice.preferences_pb"

expect fun getPlatform(): Platform