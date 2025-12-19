package com.opengps.locationsharing

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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

@Serializable
enum class RequestStatus {
    MUTUAL_CONNECTION,
    AWAITING_REQUEST,
    AWAITING_RESPONSE
}

@OptIn(ExperimentalTime::class)
class InstantSerializer() : KSerializer<Instant> {
    override fun deserialize(decoder: Decoder) = Instant.fromEpochMilliseconds(decoder.decodeLong())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilliseconds())
}

@Entity
@Serializable
@OptIn(ExperimentalTime::class)
data class User(
    @PrimaryKey(autoGenerate = true) override val id: ULong = 0uL,
    override val name: String,
    val photo: String?,
    var locationName: String,
    var send: Boolean,
    var requestStatus: RequestStatus,
    var lastBatteryLevel: Float?,
    @Serializable(with = InstantSerializer::class)
    var lastLocationChangeTime: Instant = Clock.System.now(),
    @Serializable(with = InstantSerializer::class)
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
    @Query("SELECT * FROM Waypoint")
    fun getAllFlow(): Flow<List<Waypoint>>
    @Upsert
    suspend fun upsert(wp: Waypoint)
    @Delete
    suspend fun delete(waypoint: Waypoint)
    @Query("DELETE FROM Waypoint")
    suspend fun clear()
    @Insert
    suspend fun insertAll(waypoints: List<Waypoint>)
    @Transaction
    suspend fun setAll(waypoints: List<Waypoint>) {
        clear()
        insertAll(waypoints)
    }
}

@OptIn(ExperimentalTime::class)
@Dao
interface UsersDao {
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>
    @Query("SELECT * FROM User")
    fun getAllFlow(): Flow<List<User>>
    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getByID(id: ULong): User?
    @Upsert
    suspend fun upsert(user: User)
    @Delete
    suspend fun delete(user: User)
    @Query("DELETE FROM User")
    suspend fun clear()
    @Insert
    suspend fun insertAll(users: List<User>)

    @Query("DELETE FROM User WHERE deleteAt IS NOT NULL AND deleteAt < :nowThreshold")
    suspend fun deleteExpiredUsers(nowThreshold: Instant)

    @Transaction
    suspend fun setAll(users: List<User>) {
        clear()
        insertAll(users)
    }

    @Transaction
    suspend fun update(id: ULong, update: (User) -> User) {
        val user = getByID(id) ?: return
        upsert(update(user))
    }
}

@Dao
interface LocationValueDao {
    @Query("SELECT * FROM LocationValue WHERE userid = :id")
    fun getForID(id: ULong): Flow<List<LocationValue>>
    @Query("SELECT * FROM LocationValue WHERE userid = :id ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(id: ULong): Flow<LocationValue?>
    @Query("SELECT * FROM LocationValue WHERE timestamp IN (SELECT MAX(timestamp) FROM LocationValue GROUP BY userid)")
    fun getAllLatestLocationsFlow(): Flow<List<LocationValue>>
    @Query("SELECT * FROM LocationValue WHERE timestamp IN (SELECT MAX(timestamp) FROM LocationValue GROUP BY userid)")
    suspend fun getAllLatestLocations(): List<LocationValue>
    @Upsert
    suspend fun upsertAll(locationValue: List<LocationValue>)
}

@Dao
interface BluetoothDeviceDao {
    @Query("SELECT * FROM BluetoothDevice")
    suspend fun getAll(): List<BluetoothDevice>
    @Query("SELECT * FROM BluetoothDevice")
    fun getAllFlow(): Flow<List<BluetoothDevice>>
    @Upsert
    suspend fun upsert(bluetoothDevice: BluetoothDevice)
    @Query("SELECT * FROM BluetoothDevice WHERE name = :name")
    suspend fun getFromName(name: String): BluetoothDevice?
    @Delete
    suspend fun delete(bluetoothDevice: BluetoothDevice)
}

@OptIn(ExperimentalTime::class)
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
@Database(entities = [Waypoint::class, User::class, LocationValue::class, BluetoothDevice::class], version = 10)
@TypeConverters(TC::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun usersDao(): UsersDao
    abstract fun locationValueDao(): LocationValueDao
    abstract fun bluetoothDeviceDao(): BluetoothDeviceDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
