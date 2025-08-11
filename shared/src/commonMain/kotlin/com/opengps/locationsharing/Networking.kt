package com.opengps.locationsharing

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
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

class Networking {
    companion object {

        private fun getUrl() = "https://findfamily.cc" // if(platform.name == "Android") "http://d5u5c37mmg337kgce5jpjkuuqnq7e5xc44w2vsc4wcjrrqlyo3jjvbqd.onion" else "api.findfamily.cc"

        private val client = HttpClient() {
            install(ContentNegotiation) {
                json()
            }
            engine {
                //TODO: re-enable tor eventually
                //proxy = ProxyBuilder.socks("localhost", 42997)
            }
        }
        private val crypto = CryptographyProvider.Default.get(RSA.OAEP)
        private var publickey: RSA.OAEP.PublicKey? = null
        private var privatekey: RSA.OAEP.PrivateKey? = null
        private var network_is_down = false
        var userid: ULong? = null
            private set

        suspend fun init() {
            val platform = platform
            val (privateKey, publicKey) = crypto.keyPairGenerator(digest = SHA512).generateKey().let { Pair(it.privateKey, it.publicKey) }
            platform.dataStoreUtils.setByteArray("privateKey", privateKey.encodeToByteArray(RSA.PrivateKey.Format.PEM), true)
            platform.dataStoreUtils.setByteArray("publicKey", publicKey.encodeToByteArray(RSA.PublicKey.Format.PEM), true)
            platform.dataStoreUtils.setLong("userid", Random.nextLong(), true)

            publickey = crypto.publicKeyDecoder(SHA512).decodeFromByteArray(RSA.PublicKey.Format.PEM, platform.dataStoreUtils.getByteArray("publicKey")!!)
            privatekey = crypto.privateKeyDecoder(SHA512).decodeFromByteArray(RSA.PrivateKey.Format.PEM, platform.dataStoreUtils.getByteArray("privateKey")!!)
            userid = platform.dataStoreUtils.getLong("userid")!!.toULong()
        }

        private suspend fun <T> checkNetworkDown(try_connect: suspend ()->T?): T? {
            try {
                val x = try_connect()
                network_is_down = false
                return x
            } catch(e: ConnectTimeoutException) {
                if (!network_is_down) {
                    //TODO: notify user
                    println("network is down")
                }
                network_is_down = true
            } catch(e: SocketTimeoutException) {
                if (!network_is_down) {
                    //TODO: notify user
                    println("network is down")
                }
                network_is_down = true
            } catch(e: Throwable) {
                println(e.printStackTrace())
            }
            return null
        }

        suspend fun problem(arg: String) {
            @Serializable
            data class Problem(val problem: String)
            checkNetworkDown {
                client.post("${getUrl()}/api/problem") {
                    contentType(ContentType.Application.Json)
                    setBody(Problem(arg))
                }
            }
        }

        private suspend fun register(): Boolean {
            @Serializable
            data class Register(val userid: ULong, val key: String)
            return checkNetworkDown {
                val response = client.post("${getUrl()}/api/register") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        Register(
                            userid!!,
                            publickey!!.encodeToByteArray(RSA.PublicKey.Format.PEM)
                                .encodeBase64()
                        )
                    )
                }
                val result = response.body<Boolean>()
                return@checkNetworkDown result
            } ?: false
        }

        suspend fun ensureUserExists() {
            if(getKey(userid!!) == null) {
                register()
            }
        }

        private suspend fun getKey(userid: ULong): RSA.OAEP.PublicKey? {
            return checkNetworkDown {
                val response = client.post("${getUrl()}/api/getkey") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"userid\": $userid}")
                }
                if(response.status != HttpStatusCode.OK) {
                    return@checkNetworkDown null
                }
                return@checkNetworkDown crypto.publicKeyDecoder(SHA512).decodeFromByteArray(RSA.PublicKey.Format.PEM, response.bodyAsText().decodeBase64Bytes())
            }
        }

        suspend fun publishLocation(location: LocationValue, user: User): Boolean {
            return checkNetworkDown {
                val key = if(user.encryptionKey != null) {
                    crypto.publicKeyDecoder(SHA512).decodeFromByteArray(RSA.PublicKey.Format.PEM, user.encryptionKey!!.decodeBase64Bytes())
                } else {
                    getKey(user.id)?.also {
                        platform.database.usersDao().upsert(user.copy(
                            encryptionKey = it.encodeToByteArray(RSA.PublicKey.Format.PEM).encodeBase64()
                        ))
                    }
                } ?: return@checkNetworkDown false
                client.post("${getUrl()}/api/location/publish") {
                    contentType(ContentType.Application.Json)
                    setBody(encryptLocation(location, user.id, key))
                }
                return@checkNetworkDown true
            } ?: false
        }

        suspend fun receiveLocations(): List<LocationValue>? {
            return checkNetworkDown {
                val response = client.post("${getUrl()}/api/location/receive") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"userid\": $userid}")
                }
                if(response.status != HttpStatusCode.OK) return@checkNetworkDown null
                val locationsEncrypted = response.body<List<String>>()
                val locations = locationsEncrypted.map { decryptLocation(it) }
                return@checkNetworkDown locations
            }
        }

        suspend fun sendLocationRequest(requested: ULong): Boolean {
            return checkNetworkDown {
                val response = client.post("${getUrl()}/api/request_sharing/request") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"requester\": \"${userid!!.encodeBase26()}\", \"requested\": \"${requested.encodeBase26()}\"}")
                }
                if(response.status != HttpStatusCode.OK) return@checkNetworkDown false;
                return@checkNetworkDown true
            } ?: false
        }

        suspend fun retrieveRequestsOfMe(): List<String> {
            return checkNetworkDown {
                val response = client.post("${getUrl()}/api/request_sharing/retrieve") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"requested\": \"${userid!!.encodeBase26()}\"}")
                }
                if(response.status != HttpStatusCode.OK) return@checkNetworkDown listOf();
                return@checkNetworkDown response.body<List<String>>()
            } ?: listOf()
        }

        private suspend fun encryptLocation(location: LocationValue, recipientUserID: ULong, key: RSA.OAEP.PublicKey): LocationSharingData {
            val cipher = key.encryptor()
            val str = Json.encodeToString(location)
            val encryptedData = cipher.encrypt(str.encodeToByteArray()).encodeBase64()
            return LocationSharingData(recipientUserID, encryptedData)
        }

        private suspend fun decryptLocation(encryptedLocation: String): LocationValue {
            val cipher = privatekey!!.decryptor()
            val decryptedData = cipher.decrypt(encryptedLocation.decodeBase64Bytes()).decodeToString()
            return Json.decodeFromString(decryptedData)
        }

        suspend fun generateKeyPair(): RSA.OAEP.KeyPair {
            return crypto.keyPairGenerator(digest = SHA512).generateKey()
        }

        @Serializable
        private data class LocationSharingData(val recipientUserID: ULong, val encryptedLocation: String)
    }
}