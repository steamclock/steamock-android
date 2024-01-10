package com.steamclock.steamock.lib.repo

enum class MockState {
    ENABLED, /* Mocking enabled */
    DISABLED, /* Mocking disabled */
    MOCKS_ONLY, /* Mocking enabled and enforced for all calls */
}