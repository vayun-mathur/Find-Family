package com.opengps.locationsharing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.database.getStringOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver


class AndroidPlatform(private val activity: ComponentActivity): Platform() {
    override val dataStore: DataStore<Preferences> = createDataStore(activity)

    @SuppressLint("Range")
    @Composable
    override fun requestPickContact(callback: (String, String?)->Unit): ()->Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if(uri == null) return@rememberLauncherForActivityResult
            val cur = activity.contentResolver.query(uri, null, null, null)!!
            if (cur.moveToFirst()) {
                val name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val photo = cur.getStringOrNull(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                callback(name, photo)
            }
            cur.close()
        }
        return {launcher.launch()}
    }

    override val database = Room.databaseBuilder(activity, AppDatabase::class.java, "database.db")
        .setDriver(AndroidSQLiteDriver()).build()

    override fun runBackgroundService() {
        activity.startForegroundService(Intent(activity, BackgroundLocationService::class.java))
    }

}

fun createDataStore(context: Context): DataStore<Preferences> =
    createDataStore(
        producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
    )

actual fun getPlatform(): Platform {
    return platformObject!!
}

var platformObject: AndroidPlatform? = null