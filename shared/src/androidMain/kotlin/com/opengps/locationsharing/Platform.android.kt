package com.opengps.locationsharing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.database.getStringOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase


class AndroidPlatform(private val activity: ComponentActivity): Platform {
    override val name: String = "Android"
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

    private val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override fun getLocation(): LocationValue? {
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: return null
        return LocationValue(Networking.userid!!, Coord(location.latitude, location.longitude), location.accuracy, System.currentTimeMillis())
    }

    override val database = Room.databaseBuilder(activity, AppDatabase::class.java, "database.db").build()
}

fun createDataStore(context: Context): DataStore<Preferences> =
    createDataStore(
        producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
    )