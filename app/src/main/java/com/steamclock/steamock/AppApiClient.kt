package com.steamclock.steamock

import PostmanMockInterceptorKtor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import com.steamclock.steamock.lib.api.LocalConsoleLogger
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Allows sample app to simulate calling the APIs we want to test mocks for.
 * In practice we shouldn't need this because our apps will be running requests via their
 * own repos/clients.
 */
class AppApiClient(
    private val json: Json,
    private val mockingRepo: PostmanMockRepo,
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
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
                encodeDefaults = true
            })
        }

        if (logCalls) {
            install(Logging) {
                logger = LocalConsoleLogger()
                level = LogLevel.ALL
            }
        }

        install(PostmanMockInterceptorKtor(mockingRepo))
    }

    suspend fun testingSpoofCall(): String {
        val response = internalClient.get(spoofAPIUrl)
        return response.body()
    }

}

