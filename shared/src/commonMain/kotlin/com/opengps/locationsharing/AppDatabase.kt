package com.opengps.locationsharing

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
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

@Entity
@Serializable
data class User(
    @PrimaryKey(autoGenerate = true) val id: ULong = 0uL,
    val name: String,
    val photo: String?,
    var locationName: String,
    var receive: Boolean,
    var send: Boolean,
    var lastBatteryLevel: Float? = null,
    var lastCoord: Coord? = null,
    var lastLocationChangeTime: Instant = Clock.System.now()
)

class TC {
    @TypeConverter
    fun toCoord(s: String): Coord {
        val parts = s.split(",")
        return Coord(parts[0].toDouble(), parts[1].toDouble())
    }
    @TypeConverter
    fun fromCoord(coord: Coord): String {
        return "${coord.lat},${coord.lon}"
    }

    @TypeConverter
    fun fromULong(value: ULong): Long {
        return value.toLong()
    }

    @TypeConverter
    fun toULong(value: Long): ULong {
        return value.toULong()
    }

    @TypeConverter
    fun fromULongList(value: MutableList<ULong>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toULongList(value: String): MutableList<ULong> {
        if(value == "") return mutableListOf()
        return value.split(",").map { it.toULong() }.toMutableList()
    }

    @TypeConverter
    fun fromInstant(value: Instant): Long {
        return value.toEpochMilliseconds()
    }

    @TypeConverter
    fun toInstant(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }
}

@Entity
@Serializable
data class Waypoint(
    @PrimaryKey(autoGenerate = true) val id: ULong = 0uL,
    val name: String,
    val range: Double,
    val coord: Coord,
    val usersInactive: MutableList<ULong>
)

@Dao
interface WaypointDao {
    @Query("SELECT * FROM Waypoint")
    suspend fun getAll(): List<Waypoint>

    @Query("SELECT * FROM Waypoint WHERE id = :id")
    suspend fun getById(id: Long): Waypoint?

    @Upsert
    suspend fun upsert(wp: Waypoint)

    @Query("DELETE FROM Waypoint WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM Waypoint")
    suspend fun deleteAll()
}

@Dao
interface UsersDao {
    @Query("SELECT * FROM User")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getById(id: ULong): User?

    @Upsert
    suspend fun upsert(user: User)

    @Query("DELETE FROM User WHERE id = :id")
    suspend fun delete(id: ULong)

    @Query("DELETE FROM User")
    suspend fun deleteAll()
}

@Database(entities = [Waypoint::class, User::class], version = 1)
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
