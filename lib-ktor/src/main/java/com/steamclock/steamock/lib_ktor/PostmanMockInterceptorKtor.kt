package com.steamclock.steamock.lib_ktor

import android.util.Log
import com.steamclock.steamock.lib.repo.MockResponse
import com.steamclock.steamock.lib.repo.MockState
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.header
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.util.AttributeKey

/**
 * PostmanMockInterceptorKtor is a Ktor HttpClientPlugin which intercepts outgoing requests and checks to see if a mock is available
 * and enabled for the request. If so, it replaces the request URL with the mocked URL.
 */
class PostmanMockInterceptorKtor(
    private val postmanMockRepo: PostmanMockRepo
) : HttpClientPlugin<Unit, PostmanMockInterceptorKtor> { // No HttpClientFeature Config, using PostmanMockRepo directly.

    override val key: AttributeKey<PostmanMockInterceptorKtor> = AttributeKey("MockingRequestInterceptor")

    override fun prepare(block: Unit.() -> Unit): PostmanMockInterceptorKtor = this

    override fun install(feature: PostmanMockInterceptorKtor, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
            when (val mockResponse = postmanMockRepo.getMockForPath(context.url.encodedPath)) {
                is MockResponse.NoneAvailable -> {
                    mockResponse.hadError?.let { /* todo Could do specific error handling here */ }

                    // Determine if we want to block the original request from continuing.
                    // todo look into returning a 403/500 error instead of blocking the request.
                    when (postmanMockRepo.mockState) {
                        MockState.MOCKS_ONLY -> throw IllegalStateException("Mocking enforced, but no mock selected for ${context.url.buildString()}")
                        else -> proceedWith(it)
                    }
                }
                is MockResponse.HasMockUrl -> {
                    try {
                        context.header("x-mock-response-id", mockResponse.mockId)

                        // Add delay header if desired
                        if (postmanMockRepo.mockResponseDelayMs > 0) {
                            Log.d("MockingRequestInterceptor", "x-mock-response-delay: ${postmanMockRepo.mockResponseDelayMs}ms")
                            context.header("x-mock-response-delay", postmanMockRepo.mockResponseDelayMs)
                        }

                        // Replace full url
                        context.url { takeFrom(mockResponse.mockUrl) }
                        Log.d("MockingRequestInterceptor", "Updated request URL to use mock: ${context.url.buildString()}")
                    } catch (e: Exception) {
                        Log.w("MockingRequestInterceptor", "Failed to load mock\n${e.stackTraceToString()}")
                        when (postmanMockRepo.mockState) {
                            MockState.MOCKS_ONLY -> throw IllegalStateException("Mocking enforced, but failed to set mock for ${context.url.buildString()}")
                            else -> proceedWith(it)
                        }
                    }
                }
            }
        }
    }
}