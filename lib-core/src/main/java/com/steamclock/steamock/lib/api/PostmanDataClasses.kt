package com.steamclock.steamock.lib.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Postman actually sends back more data than we need, for now I have just commented out the unused fields,
 * but in the future we could remove them if they go unused.
 */
@Serializable
object Postman {
    @Serializable
    data class CollectionResponse(
        val collection: Collection
    )

    @Serializable
    data class Collection(
        val info: Info,
        val item: List<Item>
    )

    @Serializable
    data class Info(
        //val _postman_id: String,
        val name: String,
        //val schema: String,
        //val fork: Fork,
        val updatedAt: String,
        //val uid: String
    )

//    @Serializable
//    data class Fork(
//        val label: String,
//        val createdAt: String,
//        val from: String
//    )

    @Serializable
    data class Item(
        // Common
        val name: String,
        val id: String,

        // Folders contain a list of items
        val item: List<Item>? = null,
        // API calls contain request and response data
        //val protocolProfileBehavior: ProtocolProfileBehavior,
        //val request: Request,
        @SerialName("response") val savedMocks: List<SavedMock>? = null,
        //val uid: String
    ) {
        fun getMockForGroup(groupName: String): SavedMock? {
            return savedMocks?.firstOrNull { mock ->
               mock.originalRequest.url.getQueryValueFor("group") == groupName
            }
        }
    }

    /**
     * "Item"s may be individual APIs or folders of Items, and we want to be able to parse them into separate types
     * so we can build our Composables accordingly
     */
    sealed class TypedItem(val name: String, val id: String) {
        class API(name: String, id: String, val response: List<SavedMock>): TypedItem(name, id)
        class Folder(name: String, id: String, val item: List<Item>): TypedItem(name, id)

        companion object {
            fun from(item: Item): TypedItem {
                return when {
                    item.item != null && item.savedMocks == null -> {
                        Folder(item.name, item.id, item.item)
                    }
                    item.item == null && item.savedMocks != null -> {
                        API(item.name, item.id, item.savedMocks)
                    }
                    else -> {
                        API(item.name, item.id, listOf())
                    }
                }
            }
        }
    }

//    @Serializable
//    data class ProtocolProfileBehavior(
//        val disableBodyPruning: Boolean
//    )

//    @Serializable
//    data class Request(
//        val method: String,
//        val header: List<Any>
//    )

    // Specific Mock level; this is actually a "response" object from the Postman json, however,
    // we rename it here for clarity.
    @Serializable
    data class SavedMock(
        val id: String,
        val name: String,
        val originalRequest: OriginalRequest,
        val status: String? = null,
        val code: Int? = null,
        //val _postman_previewlanguage: String,
        //val header: List<Header>,
        //val cookie: List<Any>,
        //val responseTime: Any?,
        //val body: String, // <-- Actually mocked body
        val uid: String
    )

    @Serializable
    data class OriginalRequest(
        val method: String,
        //val header: List<Any>,
        val url: Url
    )

    @Serializable
    data class Url(
        val raw: String,
        val protocol: String? = null,
        val host: List<String>,
        val path: List<String>,
        val query: List<Query> = listOf()
    ) {
        @Transient
        val fullPath = path.joinToString("/")

        @Transient
        val queryString = query?.joinToString("&") { "${it.key}=${it.value}" }

        @Transient
        val fullPathAndQueryString = "$fullPath?$queryString"

        fun getQueryValueFor(key: String): String? {
            val query = query.firstOrNull { it.key == key }
            return query?.value
        }
    }

    @Serializable
    data class Query(
        val key: String,
        val value: String? = null,
        val description: String? = null
    )

//    @Serializable
//    data class Header(
//        val key: String,
//        val value: String
//    )
}