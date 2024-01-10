package com.steamclock.steamock

import kotlinx.serialization.json.Json

// Dumb singleton, pull from DI in actual implementation
val appJson = Json{
    isLenient = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    //serializersModule = SerializersModule {} // Contextual and Polymorphic serialization
}
