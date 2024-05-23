package com.steamclock.steamock.lib.repo

import android.util.Log
import com.steamclock.steamock.lib.ui.ContentLoadViewState
import com.steamclock.steamock.lib.PostmanMockConfig
import com.steamclock.steamock.lib.api.Postman
import com.steamclock.steamock.lib.api.PostmanAPIClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * When setting up an API call in Postman, the administrator can name the call in Postman.
 * For example, "Get Dashboard" could be the name of the postman call for the "{base}/api/dashboard" endpoint.
 * This typealias aims to make it clear that we are referencing that custom Postman name.
 */
typealias ApiName = String

/**
 * Small data class that associates a mocked API (endpoint) name in Postman with it's URL.
 * This is mostly used for the sample app to allow us to easily simulate all of the API calls.
 */
data class MockedAPI(val name: ApiName, val url: String?)

/**
 * PostmanMockRepo is responsible for storing the collection of available mocks, all mocks that are currently
 * enabled/active,
 */
class PostmanMockRepo(
    private val config: PostmanMockConfig
) {
    // Allow mocks to be enabled/disabled easily (ie. add to debug menu)
    var mockState: MockState = MockState.ENABLED
    var mockResponseDelayMs = 0

    private val postmanClient = PostmanAPIClient(config)
    fun updatePostmanAccessKey(newKey: String) {
        postmanClient.updatePostmanAccessKey(newKey)
    }

    /**
     * Load state for list of all mocks available on Postman.
     */
    private val mutableMockCollectionState = MutableStateFlow<ContentLoadViewState>(ContentLoadViewState.Loading)
    val mockCollectionState = mutableMockCollectionState.asStateFlow()

    /**
     * The full postman collection, which contains a list of all available Postman mocks
     */
    private val mutableMockCollection = MutableStateFlow<Postman.Collection?>(null)
    val mockCollection = mutableMockCollection.asStateFlow()

    /**
     * List of all APIs/Endpoints that have mocks; mostly used for our sample app to allow us to easily simulate
     * all of the API calls.
     */
    private val mutableMockedAPIs = MutableStateFlow<List<MockedAPI>>(emptyList())
    val mockedAPIs = mutableMockedAPIs.asStateFlow()

    /**
     * List of all groups found in the Postman collection.
     * A group is a named set of mocks, usually geared towards mocking an entire environment, which can be collectively
     * enabled together at once.
     */
    private val mutableMockGroups = MutableStateFlow<Set<String>>(setOf())
    val mockGroups = mutableMockGroups.asStateFlow()

    /**
     * Mapping of all mocks enabled by the user, keyed by the API name.
     */
    private val mutableEnabledMocks = MutableStateFlow<Map<ApiName, Postman.SavedMock>>(mapOf())
    val enabledMocks = mutableEnabledMocks.asStateFlow()

    //==================================================================
    // Public methods
    //==================================================================
    suspend fun requestCollectionUpdate() {
        queryMockCollection(config.mockCollectionId)
    }

    suspend fun enableMock(apiName: ApiName, mock: Postman.SavedMock) {
        val mocks = mutableEnabledMocks.value.toMutableMap() // Create a new mutable map based on the existing value
        mocks[apiName] = mock
        mutableEnabledMocks.emit(mocks)
    }

    suspend fun disableMock(apiName: String) {
        val mocks = mutableEnabledMocks.value.toMutableMap()
        mocks.remove(apiName)
        mutableEnabledMocks.emit(mocks)
    }

    suspend fun clearAllMocks() {
        mutableEnabledMocks.emit(mapOf())
    }

    suspend fun enableAllMocksForGroup(name: String) {
        clearAllMocks()

        val flattenedItems = flattenedItems(mockCollection.value?.item)
            .filter { !it.savedMocks.isNullOrEmpty() }

        flattenedItems.forEach { item ->
            item.getMockForGroup(name)?.let { mock ->
                enableMock(item.name, mock)
            }
        }
    }

    /**
     * Checks to see if the request path matches any of the paths for currently enabled mocks.
     * If one matches, it will return MockResponse.HasMockUrl containing the FULL mock URL, or
     * MockResponse.NoneAvailable if we cannot or do not want to mock this path.
     */
    fun getMockForPath(requestUrlPath: String): MockResponse {
        if (mockState == MockState.DISABLED) {
            return MockResponse.NoneAvailable(null)
        }

        return try {
            // Look up mock based on the URL path being requested.
            val mock = enabledMocks.value.firstNotNullOfOrNull { mock ->
                val mockEncodedPath = mock.value.originalRequest.url.fullPath
                if (requestUrlPath.contains(mockEncodedPath, ignoreCase = true)) {
                    Log.v("MockingRequestInterceptor", "Found enabled mock: $mock")
                    mock.value
                } else {
                    null
                }
            } ?: return MockResponse.NoneAvailable(hadError = null)

            // Pass back the mocking server URL we should use in place
            MockResponse.HasMockUrl(mock.id, getMockedUrl(mock))
        } catch (e: Exception) {
            MockResponse.NoneAvailable(hadError = e)
        }
    }

    //==================================================================
    // Private methods
    //==================================================================
    /**
     * Queries the given collection ID on Postman; if successful, updates the mockCollection and mockGroups
     * and will set the mutableMockCollectionState accordingly.
     */
    private suspend fun queryMockCollection(collectionId: String) {
        mutableMockCollectionState.emit(ContentLoadViewState.Loading)
        try {
            val response = postmanClient.getCollection(collectionId)!!.collection
            mutableMockCollection.emit(response)

            // Pull out the list of all APIs that have mocks along with their name; mostly used for our
            // sample app to allow us to easily simulate all of the API calls.
            mutableMockedAPIs.emit(
                flattenedItems(response.item)
                    .filter { !it.savedMocks.isNullOrEmpty() }
                    .map {
                        val url = it.savedMocks?.firstOrNull()?.originalRequest?.url?.fullPath
                        MockedAPI(it.name, url)
                    }
            )

            // Determine available groups from response
            mutableMockGroups.emit(findMockingGroups(response))

            // Indicate mocks are ready to be used
            mutableMockCollectionState.emit(ContentLoadViewState.Success)
        } catch (e: Exception) {
            mutableMockCollectionState.emit(ContentLoadViewState.Error(e))
        }
    }

    /**
     * Postman collections may contain folders.
     * Recursively iterate through all items in the collection, returning a flattened list of all items
     * so that we can more easily search through all mocks as a single list.
     */
    private fun flattenedItems(objects: List<Postman.Item>?): List<Postman.Item> {
        return objects?.flatMap { obj ->
            val children = obj.item ?: emptyList()
            listOf(obj) + flattenedItems(children)
        } ?: emptyList()
    }

    /**
     * Iterate through all available mocks and return a list of all group names found across all
     * available Postman mocks.
     */
    private fun findMockingGroups(collection: Postman.Collection): Set<String> {
        val groupNames = mutableSetOf<String>()
        val flattenedItems = flattenedItems(collection.item)
            .filter { !it.savedMocks.isNullOrEmpty() }

        flattenedItems.forEach { item ->
            item.savedMocks?.forEach { mock ->
                mock.originalRequest.url.getQueryValueFor("group")?.let { group ->
                    groupNames.add(group)
                }
            }
        }

        return groupNames.toSet()
    }

    /**
     * Returns the URL we need to use to request the given PostmanMock.
     * This will be built from the mockServerUrl and the path/query parameters of the original request
     * for the selected mock.
     */
    private fun getMockedUrl(mock: Postman.SavedMock): String {
        return StringBuilder().apply {
            append(config.mockServerUrl)
            append("/")
            append(mock.originalRequest.url.path.joinToString("/"))
            if (mock.originalRequest.url.query.isNotEmpty()) {
                append("?")
                append(mock.originalRequest
                    .url.query
                    .joinToString("&") { "${it.key}=${it.value}" }
                )
            }
        }.toString()
    }
}