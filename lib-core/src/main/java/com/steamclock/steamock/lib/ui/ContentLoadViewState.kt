package com.steamclock.steamock.lib.ui

sealed class ContentLoadViewState {
    data object Loading: ContentLoadViewState()
    data object Success: ContentLoadViewState()
    data class Error(val throwable: Throwable): ContentLoadViewState()
}