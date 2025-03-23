package com.opengps.locationsharing

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.database.getStringOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlin.random.Random


class AndroidPlatform(private val context: Context): Platform() {
    override val dataStore: DataStore<Preferences> = createDataStore(context)

    @SuppressLint("Range")
    @Composable
    override fun requestPickContact(callback: (String, String?)->Unit): ()->Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if(uri == null) return@rememberLauncherForActivityResult
            val cur = context.contentResolver.query(uri, null, null, null)!!
            if (cur.moveToFirst()) {
                val name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val photo = cur.getStringOrNull(cur.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                callback(name, photo)
            }
            cur.close()
        }
        return {launcher.launch()}
    }

    override val database = Room.databaseBuilder(context, AppDatabase::class.java, "database.db")
        .setDriver(AndroidSQLiteDriver()).build()

    override fun runBackgroundService() {
        context.startForegroundService(Intent(context, BackgroundLocationService::class.java))
    }

    override fun createNotification(s: String, channelId: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Location Sharing")
            .setContentText(s)
            .setSmallIcon(R.drawable.baseline_notifications_24) // Replace with your icon
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
        val notificationManager = getSystemService(context, NotificationManager::class.java)!!
        notificationManager.notify(Random.nextInt(), notification)
    }

}

fun createDataStore(context: Context): DataStore<Preferences> =
    createDataStore(
        producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
    )

actual fun getPlatform(): Platform {
    return platformObject!!
}

@SuppressLint("StaticFieldLeak")
var platformObject: AndroidPlatform? = null