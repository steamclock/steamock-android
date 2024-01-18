package com.steamclock.steamock.lib

import kotlinx.serialization.json.Json

data class PostmanMockConfig(
    val postmanAccessKey: String,
    val mockCollectionId: String,
    val mockServerUrl: String,
    val json: Json,
    val logCalls: Boolean
)