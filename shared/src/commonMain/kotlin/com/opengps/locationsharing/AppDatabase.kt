package com.opengps.locationsharing

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
@Database(entities = [Waypoint::class, User::class], version = 4)
@TypeConverters(TC::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun usersDao(): UsersDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
