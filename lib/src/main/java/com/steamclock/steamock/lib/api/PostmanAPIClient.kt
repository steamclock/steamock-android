package com.steamclock.steamock.lib.api

import android.util.Log
import com.steamclock.steamock.lib.PostmanMockConfig
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*

/**
 * PostmanAPIClient wraps up the Postman API calls which allow us to get collection (and
 * eventually mocking) data
 *
 * https://ktor.io/docs/migrating-2.html#feature-plugin-client
 */
class PostmanAPIClient(
    private val config: PostmanMockConfig
) {
    private val postmanAPIBaseUrl = "https://api.getpostman.com"
    private val postmanAPICollectionPath = "collections"

    private val internalClient = HttpClient(CIO) {
        install(ContentNegotiation) {
           json(config.json)
        }

        if (config.logCalls) {
            install(Logging) {
                logger = KoinLocalConsoleLogger()
                level = LogLevel.ALL
            }
        }

        defaultRequest {
            headers {
                append("X-API-Key", config.postmanAccessKey)
            }
        }
    }

    /**
     * Requests data via HttpClient directly; could setup interface?
     */
    suspend fun getCollection(collectionId: String): Postman.CollectionResponse {
        val collectionUrl = "$postmanAPIBaseUrl/$postmanAPICollectionPath/$collectionId"
        val response = internalClient.get(collectionUrl) {}
        return response.body()
    }
}

class KoinLocalConsoleLogger : Logger {
    override fun log(message: String) {
        Log.d("LocalConsoleLogger", message)
    }
}
