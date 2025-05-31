package com.opengps.locationsharing

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.AutoMigration
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
    abstract fun createNotification(title: String, body: String, channelId: String)
    abstract fun copyToClipboard(text: String)
    abstract fun startScanBluetoothDevices(setRSSI: (String, Int) -> Unit): ()->Unit

    var nearBluetoothDevices = mutableListOf<BluetoothDevice>()

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
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE BluetoothDevice DROP COLUMN address")
    }
}
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE LocationValue ADD COLUMN sleep INTEGER NOT NULL DEFAULT 0")
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE User DROP COLUMN receive")
    }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE User ADD COLUMN requestStatus TEXT NOT NULL DEFAULT \"MUTUAL_CONNECTION\"")
    }
}

val migrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

const val dataStoreFileName = "dice.preferences_pb"

expect var platformInternal: Platform?

val platform: Platform
    get() = platformInternal!!