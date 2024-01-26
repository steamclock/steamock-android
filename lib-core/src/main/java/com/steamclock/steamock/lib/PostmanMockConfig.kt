package com.steamclock.steamock.lib

import kotlinx.serialization.json.Json


// https://learning.postman.com/docs/designing-and-developing-your-api/mocking-data/mock-with-api/
// todo investigate
// - x-mock-response-code header
// - x-mock-response-name or x-mock-response-id to specify the exact response you want the mock server to return


// https://learning.postman.com/docs/designing-and-developing-your-api/mocking-data/creating-dynamic-responses/
// - pulling out post path parameters

data class PostmanMockConfig(
    val postmanAccessKey: String,
    val mockCollectionId: String,
    val mockServerUrl: String,
    val json: Json,
    val logCalls: Boolean
)