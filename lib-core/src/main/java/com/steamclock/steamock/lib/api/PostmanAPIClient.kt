package com.steamclock.steamock.lib.api

import com.steamclock.steamock.lib.PostmanMockConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync

/**
 * PostmanAPIClient wraps up the Postman API calls which allow us to get collection (and
 * eventually mocking) data
 */
class PostmanAPIClient(
    private val config: PostmanMockConfig
) {
    private val json = Json{
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Using basic Okhttp client to run simple postman API calls.
    private val client = OkHttpClient()

    //=====================================================================
    // Postman API calls
    //=====================================================================
    private val postmanAPIBaseUrl = "https://api.getpostman.com"
    private val postmanAPICollectionPath = "collections"

    suspend fun getCollection(collectionId: String): Postman.CollectionResponse? {
        val collectionUrl = "$postmanAPIBaseUrl/$postmanAPICollectionPath/$collectionId"
        val request = Request.Builder()
            .url(collectionUrl)
            .header("X-API-Key", config.postmanAccessKey)
            .build()

        val response = client.newCall(request).executeAsync()
        return if (response.isSuccessful) {
            val responseBody = response.body.string()
            try {
                json.decodeFromString<Postman.CollectionResponse>(responseBody)
            } catch (e: Exception) {
                // todo Handle the error response
                println("Error:  ${e.message}")
                null
            }
        } else {
            // todo Handle the error response
            println("Error: ${response.code} - ${response.message}")
            null
        }
    }
}