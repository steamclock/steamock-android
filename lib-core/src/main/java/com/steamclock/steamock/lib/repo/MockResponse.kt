package com.steamclock.steamock.lib.repo

sealed class MockResponse {
    class NoneAvailable(val hadError: Exception?): MockResponse()
    class HasMockUrl(val mockUrl: String): MockResponse()
}