package com.steamclock.steamock

import PostmanMockInterceptorKtor
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
 */
class ExampleApiClient(
    private val json: Json,
    private val mockingRepo: PostmanMockRepo,
    logCalls: Boolean = true
) {
    /**
     * Ktor client setup, in practice this would probably be done in a DI module.
     */
    private val internalClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }

        if (logCalls) {
            install(Logging) {
                logger = LocalConsoleLogger()
                level = LogLevel.ALL
            }
        }

        // Install our mock interceptor - This is the important, as these interceptors are what will
        // automatically return mock data if enabled and setup.
        install(PostmanMockInterceptorKtor(mockingRepo))
    }

    /**
     * Super dumb wrapper that just fires off an API call directly and returns the response as a string.
     * Allows us to test the mock interceptor.
     */
    suspend fun makeRequest(fullUrl: String): String {
        val response = internalClient.get(fullUrl)
        return response.body()
    }
}

