package com.opengps.locationsharing

import androidx.datastore.preferences.core.edit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class Backup(val users: List<User>, val waypoints: List<Waypoint>, val privateKey: ByteArray, val publicKey: ByteArray, val userID: Long) {


    companion object {
        suspend fun downloadBackupFile(file: PlatformFile) {
            val users = platform.database.usersDao().getAll();
            val waypoints = platform.database.waypointDao().getAll();
            val privateKey = platform.dataStoreUtils.getByteArray("privateKey")!!;
            val publicKey = platform.dataStoreUtils.getByteArray("publicKey")!!
            val userid = platform.dataStoreUtils.getLong("userid")!!
            val string = Json.encodeToString(Backup(users, waypoints, privateKey, publicKey, userid))
            file.writeString(string);
        }

        suspend fun restoreBackupFile(file: PlatformFile) {
            val backup = Json.decodeFromString<Backup>(file.readString())
            platform.database.usersDao().setAll(backup.users)
            platform.database.waypointDao().setAll(backup.waypoints)
            platform.dataStoreUtils.setByteArray("privateKey", backup.privateKey)
            platform.dataStoreUtils.setByteArray("publicKey", backup.publicKey)
            platform.dataStoreUtils.setLong("userid", backup.userID)
        }
    }
}