package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

interface Platform {
    val name: String
    val dataStore: DataStore<Preferences>
    val database: AppDatabase
    @Composable
    fun requestPickContact(callback: (String, String?)->Unit): ()->Unit
    fun getLocation(): LocationValue?

    companion object {
        var current: Platform? = null
    }
}

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "dice.preferences_pb"