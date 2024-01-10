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
 */
class PostmanAPIClient(
    private val config: PostmanMockConfig
) {
    /**
     * Using Ktor to make Postman API calls
     */
    private val internalClient = HttpClient(CIO) {
        // Use JSON serialization to parse our Postman API responses
        install(ContentNegotiation) {
            json(config.json)
        }

        // Log postman API calls to console if enabled
        if (config.logCalls) {
            install(Logging) {
                logger = LocalConsoleLogger()
                level = LogLevel.ALL
            }
        }

        // Add postman API key to all postman API calls
        defaultRequest {
            headers {
                append("X-API-Key", config.postmanAccessKey)
            }
        }
    }

    //=====================================================================
    // Postman API calls
    //=====================================================================
    private val postmanAPIBaseUrl = "https://api.getpostman.com"
    private val postmanAPICollectionPath = "collections"

    suspend fun getCollection(collectionId: String): Postman.CollectionResponse {
        val collectionUrl = "$postmanAPIBaseUrl/$postmanAPICollectionPath/$collectionId"
        val response = internalClient.get(collectionUrl) {}
        return response.body()
    }
}

/**
 * Utility class to log to console
 */
class LocalConsoleLogger : Logger {
    override fun log(message: String) {
        Log.d("LocalConsoleLogger", message)
    }
}