package com.steamclock.steamock.lib.repo

sealed class MockResponse {
    class NoneAvailable(val hadError: Exception?): MockResponse()
    class HasMockUrl(val mockId: String, val mockUrl: String): MockResponse()
}