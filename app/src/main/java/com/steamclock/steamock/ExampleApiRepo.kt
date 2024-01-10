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
        mutableApiResponse.emit(client.makeRequest(fullUrl))
    }
}

