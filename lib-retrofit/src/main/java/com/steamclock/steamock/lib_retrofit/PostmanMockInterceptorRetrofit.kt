package com.steamclock.steamock.lib_retrofit

import android.util.Log
import com.steamclock.steamock.lib.repo.MockResponse
import com.steamclock.steamock.lib.repo.MockState
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import okhttp3.Interceptor
import okhttp3.Response

class PostmanMockInterceptorRetrofit(private val postmanMockRepo: PostmanMockRepo): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val ongoing = chain.request().newBuilder()
        var request = chain.request()

        when (val mockResponse = postmanMockRepo.getMockForPath(request.url.encodedPath)) {
            is MockResponse.NoneAvailable -> {
                mockResponse.hadError?.let { /* todo Could do specific error handling here */ }

                // Determine if we want to block the original request from continuing.
                return when (postmanMockRepo.mockState) {
                    MockState.MOCKS_ONLY -> throw IllegalStateException("Mocking enforced, but no mock selected for ${request.url}")
                    else -> chain.proceed(ongoing.build())
                }
            }
            is MockResponse.HasMockUrl -> {
                return try {
                    // Add delay header if desired
                    if (postmanMockRepo.mockResponseDelayMs > 0) {
                        Log.d("MockingRequestInterceptor", "x-mock-response-delay: ${postmanMockRepo.mockResponseDelayMs}ms")
                        ongoing.addHeader("x-mock-response-delay", postmanMockRepo.mockResponseDelayMs.toString())
                    }

                    // Replace full url
                    Log.d("MockingRequestInterceptor", "Changed context to: ${mockResponse.mockUrl}")
                    request = request.newBuilder().url(mockResponse.mockUrl).build()
                    chain.proceed(request)
                } catch (e: Exception) {
                    Log.w("MockingRequestInterceptor", "Failed to load mock\n${e.stackTraceToString()}")
                    when (postmanMockRepo.mockState) {
                        MockState.MOCKS_ONLY -> throw IllegalStateException("Mocking enforced, but failed to set mock for ${request.url}")
                        else -> chain.proceed(ongoing.build())
                    }
                }
            }
        }
    }
}