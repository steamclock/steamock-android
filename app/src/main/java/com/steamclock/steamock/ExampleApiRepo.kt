package com.steamclock.steamock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Allows sample app to simulate calling the APIs we want to test mocks for.
 */
class ExampleApiRepo(
    private val client: ExampleApiClient
) {
    private val mutableApiResponse = MutableStateFlow("")
    val apiResponse = mutableApiResponse.asStateFlow()

    suspend fun makeRequest(fullUrl: String) {
        mutableApiResponse.emit("Loading...")
        try {
            mutableApiResponse.emit(client.makeRequest(fullUrl))
        } catch (e: Exception) {
            // If mocking enforced, but no mock is found, then our client will throw an exception
            mutableApiResponse.emit(e.localizedMessage ?: "Unknown error")
            return
        }
    }
}