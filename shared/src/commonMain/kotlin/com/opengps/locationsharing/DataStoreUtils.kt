package com.opengps.locationsharing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.delay

class DataStoreUtils(private val dataStore: () -> DataStore<Preferences>) {

    private var stateMap = mapOf<Preferences.Key<*>, Any>()

    init {
        SuspendScope {
            while(dataStore() == null) {
                delay(100)
            }
            dataStore().data.collect({
                stateMap = it.asMap()
            })
        }
    }

    fun getString(name: String): String? {
        return stateMap[stringPreferencesKey(name)] as String?
    }

    fun getStringOrDefault(name: String, default: String): String {
        return stateMap[stringPreferencesKey(name)] as String? ?: run {
            SuspendScope { setString(name, default) }
            default
        }
    }

    suspend fun setString(name: String, value: String) {
        dataStore().edit {
            it[stringPreferencesKey(name)] = value
        }
    }

    fun getByteArray(name: String): ByteArray? {
        return stateMap[stringPreferencesKey(name)] as ByteArray?
    }

    fun getByteArrayOrDefault(name: String, default: ByteArray): ByteArray {
        return stateMap[byteArrayPreferencesKey(name)] as ByteArray? ?: run {
            SuspendScope { setByteArray(name, default) }
            default
        }
    }

    suspend fun setByteArray(name: String, value: ByteArray) {
        dataStore().edit {
            it[byteArrayPreferencesKey(name)] = value
        }
    }

    fun getBoolean(name: String): Boolean? {
        return stateMap[booleanPreferencesKey(name)] as Boolean?
    }

    fun getBooleanOrDefault(name: String, default: Boolean): Boolean {
        return stateMap[booleanPreferencesKey(name)] as Boolean? ?: run {
            SuspendScope { setBoolean(name, default) }
            default
        }
    }

    suspend fun setBoolean(name: String, value: Boolean) {
        dataStore().edit {
            it[booleanPreferencesKey(name)] = value
        }
    }

    fun getLong(name: String): Long? {
        return stateMap[longPreferencesKey(name)] as Long?
    }

    fun getLongOrDefault(name: String, default: Long): Long {
        return stateMap[longPreferencesKey(name)] as Long? ?: run {
            SuspendScope { setLong(name, default) }
            default
        }
    }

    suspend fun setLong(s: String, userid: Long) {
        dataStore().edit {
            it[longPreferencesKey(s)] = userid
        }
    }
}