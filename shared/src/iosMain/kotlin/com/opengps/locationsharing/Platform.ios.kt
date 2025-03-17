package com.opengps.locationsharing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val dataStore: DataStore<Preferences>
        get() = TODO("Not yet implemented")
    override val database: AppDatabase
        get() = TODO("Not yet implemented")

    override fun requestPickContact(callback: (String, String?) -> Unit): () -> Unit {
        TODO("Not yet implemented")
    }

    override fun getLocation(): LocationValue? {
        TODO("Not yet implemented")
    }
}

actual fun getPlatform(): Platform = IOSPlatform()