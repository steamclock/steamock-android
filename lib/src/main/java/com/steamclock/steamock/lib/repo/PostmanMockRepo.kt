package com.steamclock.steamock.lib.repo

import android.util.Log
import com.steamclock.steamock.lib.ui.ContentLoadViewState
import com.steamclock.steamock.lib.PostmanMockConfig
import com.steamclock.steamock.lib.api.Postman
import com.steamclock.steamock.lib.api.PostmanAPIClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

typealias ApiName = String // Note this user defined in Postman and may not be related to the actual URL path

class PostmanMockRepo(
    private val config: PostmanMockConfig
) {
    // Allow mocks to be enabled/disabled easily (ie. add to debug menu)
    var mockState: MockState = MockState.ENABLED
    var mockResponseDelayMs = 0

    private val postmanClient = PostmanAPIClient(config)

    /**
     * Load state for collection calls
     */
    private val mutableMockCollectionState = MutableStateFlow<ContentLoadViewState>(
        ContentLoadViewState.Loading
    )
    val mockCollectionState = mutableMockCollectionState.asStateFlow()

    /**
     * List of all items in a Postman Collection
     */
    private val mutableMockCollection = MutableStateFlow<Postman.Collection?>(null)
    val mockCollection = mutableMockCollection.asStateFlow()

    private val mutableMockGroups = MutableStateFlow<Set<String>>(setOf())
    val mockGroups = mutableMockGroups.asStateFlow()

    /**
     *
     */
    private val mutableEnabledMocks = MutableStateFlow<Map<ApiName, Postman.Response>>(mapOf())
    val enabledMocks = mutableEnabledMocks.asStateFlow()

    //==================================================================
    // Public methods, setting up mocks
    //==================================================================
//    suspend fun syncEnabledMocks() {
//        // todo sync with datastore? Use dataStore directly?
//        // In the future we could download mocks for offline usage
//        // For now start off empty
//
//        //
//        mutableEnabledMocks.emit(hashMapOf())
//    }

    suspend fun requestCollectionUpdate() {
        queryMockCollection(config.mockCollectionId)
    }

    /**
     * Will populate mutableMockCollectionState with the postman collection of the given ID.
     */
    suspend fun queryMockCollection(collectionId: String) {
        mutableMockCollectionState.emit(ContentLoadViewState.Loading)

        try {
            val response = postmanClient.getCollection(collectionId).collection
            mutableMockCollection.emit(response)
            mutableMockGroups.emit(findMockingGroups(response))
            mutableMockCollectionState.emit(ContentLoadViewState.Success)
        } catch (e: Exception) {
            mutableMockCollectionState.emit(ContentLoadViewState.Error(e))
        }
    }

    private fun flattenedItems(objects: List<Postman.Item>?): List<Postman.Item> {
        return objects?.flatMap { obj ->
            val children = obj.item ?: emptyList()
            listOf(obj) + flattenedItems(children)
        } ?: emptyList()
    }

    /**
     * Iterate through all available mocks - return a list of all the names found as "group"
     * query properties.
     */
    private fun findMockingGroups(collection: Postman.Collection): Set<String> {
        val groupNames = mutableSetOf<String>()
        val flattenedItems = flattenedItems(collection.item)
            .filter { !it.response.isNullOrEmpty() }

        flattenedItems.forEach { item ->
            item.response?.forEach { mock ->
                mock.originalRequest.url.getQueryValueFor("group")?.let { group ->
                    groupNames.add(group)
                }
                Log.v("shayla", mock.originalRequest.url.query.toString())
            }
        }

        Log.v("shayla", flattenedItems.size.toString())
        return groupNames.toSet()
    }

    suspend fun enableMock(apiName: ApiName, mock: Postman.Response) {
        val mocks = mutableEnabledMocks.value?.toMutableMap() ?: mutableMapOf() // Create a new mutable map based on the existing value
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
            .filter { !it.response.isNullOrEmpty() }

        flattenedItems.forEach { item ->
            item.getMockForGroup(name)?.let { mock ->
                enableMock(item.name, mock)
            }
        }
    }

    //==================================================================
    // Public methods, using mocks
    //==================================================================
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
            val mock = enabledMocks.value?.firstNotNullOf { mock ->
                val mockEncodedPath = mock.value.originalRequest.url.fullPath
                if (requestUrlPath.contains(mockEncodedPath, ignoreCase = true)) {
                    Log.v("MockingRequestInterceptor", "Found enabled mock: $mock")
                    mock.value
                } else {
                    null
                }
            } ?: return MockResponse.NoneAvailable(hadError = null)

            // Pass back the mocking server URL we should use in place
            MockResponse.HasMockUrl(getMockedUrl(mock))
        } catch (e: Exception) {
            MockResponse.NoneAvailable(hadError = e)
        }
    }

    //==================================================================
    // Private methods
    //==================================================================
    private fun getMockedUrl(mock: Postman.Response): String {
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