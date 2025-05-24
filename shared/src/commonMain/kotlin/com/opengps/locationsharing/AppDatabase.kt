package com.opengps.locationsharing

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface ObjectParent {
    val id: ULong
    val name: String
}

@Entity
@Serializable
data class LocationValue(
    @PrimaryKey(autoGenerate = true) val id: ULong = 0uL,
    val userid: ULong,
    val coord: Coord,
    val speed: Float,
    val acc: Float,
    val timestamp: Long,
    val battery: Float,
    val sleep: Boolean = false)

@Entity
@Serializable
data class BluetoothDevice(
    @PrimaryKey
    override val id: ULong = 0uL,
    override val name: String,
    val lastLocationValue: LocationValue? = null,
): ObjectParent

@Entity
@Serializable
data class User(
    @PrimaryKey(autoGenerate = true) override val id: ULong = 0uL,
    override val name: String,
    val photo: String?,
    var locationName: String,
    var receive: Boolean,
    var send: Boolean,
    var lastBatteryLevel: Float?,
    var lastCoord: Coord?,
    var lastLocationChangeTime: Instant = Clock.System.now(),
    var lastLocationValue: LocationValue? = null,
    var deleteAt: Instant? = null,
    var encryptionKey: String? = null,
): ObjectParent

@Entity
@Serializable
data class Waypoint(
    @PrimaryKey(autoGenerate = true) override val id: ULong = 0uL,
    override val name: String,
    val range: Double,
    val coord: Coord,
    val usersInactive: MutableList<ULong>
): ObjectParent

@Dao
interface WaypointDao {
    @Query("SELECT * FROM Waypoint")
    suspend fun getAll(): List<Waypoint>
    @Upsert
    suspend fun upsert(wp: Waypoint)
    @Delete
    suspend fun delete(waypoint: Waypoint)
}

@Dao
interface UsersDao {
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>
    @Upsert
    suspend fun upsert(user: User)
    @Delete
    suspend fun delete(user: User)
}

@Dao
interface LocationValueDao {
    @Query("SELECT * FROM LocationValue")
    suspend fun getAll(): List<LocationValue>
    @Query("SELECT * FROM LocationValue WHERE timestamp > :timestamp")
    suspend fun getSince(timestamp: Long): List<LocationValue>
    @Query("DELETE FROM LocationValue WHERE timestamp < :timestamp")
    suspend fun clearBefore(timestamp: Long)
    @Upsert
    suspend fun upsert(locationValue: LocationValue)
    @Upsert
    suspend fun upsertAll(locationValue: List<LocationValue>)
    @Delete
    suspend fun delete(locationValue: LocationValue)
}

@Dao
interface BluetoothDeviceDao {
    @Query("SELECT * FROM BluetoothDevice")
    suspend fun getAll(): List<BluetoothDevice>
    @Upsert
    suspend fun upsert(bluetoothDevice: BluetoothDevice)
    @Query("SELECT * FROM BluetoothDevice WHERE name = :name")
    suspend fun getFromName(name: String): BluetoothDevice?
    @Delete
    suspend fun delete(bluetoothDevice: BluetoothDevice)
}

class TC {
    @TypeConverter fun fromULong(value: ULong) = value.toLong()
    @TypeConverter fun toULong(value: Long) = value.toULong()
    @TypeConverter fun fromInstant(value: Instant) = value.toEpochMilliseconds()
    @TypeConverter fun toInstant(value: Long) = Instant.fromEpochMilliseconds(value)
    @TypeConverter fun fromLocationValue(value: LocationValue?) = Json.encodeToString(value)
    @TypeConverter fun toLocationValue(value: String) = Json.decodeFromString<LocationValue?>(value)
    @TypeConverter fun fromUlonglist(value: MutableList<ULong>?) = Json.encodeToString(value)
    @TypeConverter fun toUlonglist(value: String) = Json.decodeFromString<MutableList<ULong>?>(value)
    @TypeConverter fun fromCoord(value: Coord?) = Json.encodeToString(value)
    @TypeConverter fun toCoord(value: String) = Json.decodeFromString<Coord?>(value)
}
@Database(entities = [Waypoint::class, User::class, LocationValue::class, BluetoothDevice::class], version = 7)
@TypeConverters(TC::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun usersDao(): UsersDao
    abstract fun locationValueDao(): LocationValueDao
    abstract fun bluetoothDeviceDao(): BluetoothDeviceDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
