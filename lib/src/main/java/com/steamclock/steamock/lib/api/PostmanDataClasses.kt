package com.steamclock.steamock.lib.api

import kotlinx.serialization.Serializable

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
        val schema: String,
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
        val response: List<Response>? = null,
        //val uid: String
    ) {
        fun getMockForGroup(groupName: String): Response? {
            return response?.firstOrNull { mock ->
               mock.originalRequest.url.getQueryValueFor("group") == groupName
            }
        }
    }

    /**
     * Items may be individual APIs or folders of Items, based on the properties they include;
     * TypedItem allows us to force a class so that we can be more explicit with our
     * Composables.
     */
    sealed class TypedItem(val name: String, val id: String) {
        class API(name: String, id: String, val response: List<Response>): TypedItem(name, id)
        class Folder(name: String, id: String, val item: List<Item>): TypedItem(name, id)

        companion object {
            fun from(item: Item): TypedItem {
                return when {
                    item.item != null && item.response == null -> {
                        Folder(item.name, item.id, item.item)
                    }
                    item.item == null && item.response != null -> {
                        API(item.name, item.id, item.response)
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

    // Specific Mock level
    @Serializable
    data class Response(
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

//    fun OriginalRequest.modifyHost(newHost: String): OriginalRequest {
//        val newRawUrl = StringBuilder()
//        newRawUrl.append(newHost)
//        newRawUrl.append(url.path.joinToString("/"))
//        if (url.query.isNotEmpty()) {
//            val queryString = url.query.joinToString("&") { "${it.key}=${it.value}" }
//            newRawUrl.append("?$queryString")
//        }
//
//        val newHostUrl = Url(newHost)
//        val newHostPostmanUrl = Url(
//            raw = newRawUrl.toString(),
//            protocol = newHostUrl.protocol.toString(),
//            host = newHostUrl.host.split("."),
//            path = this.url.path,
//            query = this.url.query
//        )
//
//        return OriginalRequest(
//            method = this.method,
//            url = newHostPostmanUrl
//        )
//    }

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

    @Serializable
    data class Header(
        val key: String,
        val value: String
    )
}

