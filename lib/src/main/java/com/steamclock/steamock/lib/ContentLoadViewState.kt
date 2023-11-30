package com.steamclock.steamock.lib

sealed class ContentLoadViewState {
    object Loading: ContentLoadViewState()
    object Success: ContentLoadViewState()
    data class Error(val throwable: Throwable): ContentLoadViewState()
}