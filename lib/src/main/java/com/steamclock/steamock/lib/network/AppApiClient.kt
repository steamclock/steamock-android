package com.steamclock.steamock.lib.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import kotlinx.serialization.json.Json

/**
 * Allows sample app to simulate calling the APIs we want to test mocks for.
 * In practice we shouldn't need this because our apps will be running requests via their
 * own repos/clients.
 */
class AppApiClient(
    private val json: Json,
//    private val mockingRepo: PostmanMockRepo,
    initialRequestAPI: String,
    logCalls: Boolean = true
) {
    // At the moment, it is not recommended to use reactive streams to define the state variable
    // a for TextField (which is where we are showing this value). Use mutableStateOf with update
    // method instead.
    var spoofAPIUrl by mutableStateOf(initialRequestAPI)
        private set
    fun updateSpoofAPIUrl(newAPI: String) { spoofAPIUrl = newAPI }

    private val internalClient = HttpClient(CIO) {


//        install(JsonFeature) {
//            serializer = KotlinxSerializer(json)
//        }
//
//        if (logCalls) {
//            install(Logging) {
//                logger = KoinLocalConsoleLogger()
//                level = LogLevel.ALL
//            }
//        }
//
//        install(PostmanMockInterceptorKtor(mockingRepo))
    }

    private fun setupInterceptor() {
        internalClient.plugin(HttpSend).intercept { request ->
            // todo Add any extra http request params that may be required.
            val originalCall = execute(request)
            if (originalCall.response.status.value !in 100..399) {
                execute(request)
            } else {
                originalCall
            }
        }
    }

//    suspend fun testingSpoofCall(): String {
//        return internalClient.get(spoofAPIUrl) {
//            // todo Add any extra http request params that may be required.
//        }
//    }

}

