package com.opengps.locationsharing

import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.random.nextULong

class Networking {
    companion object {
        private const val url = "api.findfamily.cc"

        private val client = HttpClient() {
            install(ContentNegotiation) {
                json()
            }
        }
        private val crypto = CryptographyProvider.Default.get(AES.CTR)
        private var key: AES.CTR.Key? = null
        var userid: ULong? = null
            private set

        private suspend fun getPrivateKey() {
            val platform = getPlatform()
            val privateKeyKey = byteArrayPreferencesKey("privateKey")
            val useridKey = longPreferencesKey("userid")
            platform.dataStore.edit {
                key = it[privateKeyKey]?.let { it1 ->
                    crypto.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, it1)
                }
                userid = it[useridKey]?.toULong()
            }
            if (key == null) {
                key = crypto.keyGenerator(AES.Key.Size.B256).generateKey()
                userid = Random.nextULong()
                platform.dataStore.edit {
                    it[privateKeyKey] = key!!.encodeToByteArray(AES.Key.Format.RAW)
                    it[useridKey] = userid!!.toLong()
                }
                register()
            }
        }

        suspend fun init() {
            if(key == null)
                getPrivateKey()
        }

        private suspend fun register() {
            @Serializable
            data class Register(val userid: ULong, val key: String)
            client.post("https://$url/register") {
                contentType(ContentType.Application.Json)
                setBody(Register(userid!!, key!!.encodeToByteArray(AES.Key.Format.RAW).encodeBase64()))
            }
        }

        suspend fun ensureUserExists() {
            if(getKey(userid!!) == null) {
                register()
            }
        }

        private suspend fun getKey(userid: ULong): AES.CTR.Key? {
            try {
                val response = client.post("https://$url/getkey") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"userid\": $userid}")
                }
                if(response.status != HttpStatusCode.OK) {
                    return null
                }
                return crypto.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, response.bodyAsText().decodeBase64Bytes())
            }  catch (e: ServerResponseException) {
                return null
            }
        }

        suspend fun publishLocation(location: LocationValue, user: User): Boolean {
            try {
                val key = getKey(user.id) ?: return false
                client.post("https://$url/location/publish") {
                    contentType(ContentType.Application.Json)
                    setBody(encryptLocation(location, user.id, key))
                }
                return true
            } catch(e: SocketTimeoutException) {
                return false
            }
        }

        suspend fun receiveLocations(): List<LocationValue>? {
            try {
                val response = client.post("https://$url/location/receive") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"userid\": $userid}")
                }
                if(response.status != HttpStatusCode.OK) return null
                println(response.bodyAsText())
                val locationsEncrypted = response.body<List<String>>()
                val locations = locationsEncrypted.map { decryptLocation(it) }
                return locations
            } catch(e: SocketTimeoutException) {
                return null
            }
        }

        private suspend fun encryptLocation(location: LocationValue, recipientUserID: ULong, key: AES.CTR.Key): LocationSharingData {
            val cipher = key.cipher()
            val str = Json.encodeToString(location)
            val encryptedData = cipher.encrypt(str.encodeToByteArray()).encodeBase64()
            return LocationSharingData(recipientUserID, encryptedData)
        }

        private suspend fun decryptLocation(encryptedLocation: String): LocationValue {
            val cipher = key!!.cipher()
            val decryptedData = cipher.decrypt(encryptedLocation.decodeBase64Bytes()).decodeToString()
            return Json.decodeFromString(decryptedData)
        }

        @Serializable
        private data class LocationSharingData(val recipientUserID: ULong, val encryptedLocation: String)
    }
}