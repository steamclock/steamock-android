package com.steamclock.steamock.lib.ui

sealed class ContentLoadViewState {
    object Loading: ContentLoadViewState()
    object Success: ContentLoadViewState()
    data class Error(val throwable: Throwable): ContentLoadViewState()
}