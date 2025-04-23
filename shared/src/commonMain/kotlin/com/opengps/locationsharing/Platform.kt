package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import io.matthewnelson.kmp.tor.runtime.TorRuntime
import okio.Path.Companion.toPath

abstract class Platform {

    abstract val runtimeEnvironment: TorRuntime.Environment
    abstract val dataStore: DataStore<Preferences>
    val dataStoreUtils = DataStoreUtils { this.dataStore }
    abstract val database: AppDatabase
    @Composable
    abstract fun requestPickContact(callback: (String, String?)->Unit): ()->Unit

    abstract fun runBackgroundService()
    abstract fun createNotification(s: String, channelId: String)
    abstract fun copyToClipboard(text: String)

    abstract val batteryLevel: Float

    abstract val name: String
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE User ADD COLUMN lastLocationValue TEXT")
    }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE User ADD COLUMN encryptionKey TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE User ADD COLUMN deleteAt BIGINT")
    }
}

val migrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "dice.preferences_pb"

expect var platformInternal: Platform?

val platform: Platform
    get() = platformInternal!!