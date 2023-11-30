package com.steamclock.steamock.lib.data
import com.steamclock.steamock.lib.mocks.PostmanMockConfig

sealed class PostmanMockEnv(val postmanMockConfig: PostmanMockConfig, val appApiStartingUrl: String) {

    companion object {
        private const val accessKey = ""
    }

    // todo put access key in local properties file?
    object ShaylaMockGPS : PostmanMockEnv(
        PostmanMockConfig(
            postmanAccessKey = accessKey,
            mockCollectionId = "8183416-3899cbb7-90f4-425d-98ef-639e77da615f",
            mockServerUrl = "https://9f676ae9-0f2b-442a-915b-fc24fd62cf61.mock.pstmn.io",
            json = appJson,
            logCalls = true
        ),
        appApiStartingUrl = "https://api.test.gypsyguide.com/v2/catalog"
    )

    object AmyMockGraphQL : PostmanMockEnv(
        PostmanMockConfig(
            postmanAccessKey = accessKey,
            mockCollectionId = "23678740-5847c244-5b44-4cf4-98e1-cd308a524fcb",
            mockServerUrl = "https://e67ab2fb-3520-4bc7-8cfb-d45eaf467e50.mock.pstmn.io",
            json = appJson,
            logCalls = true
        ),
        appApiStartingUrl = "https://idontexist.com/graphql/:id"
    )

    object CIDInvestingDemo : PostmanMockEnv(
        PostmanMockConfig(
            postmanAccessKey = accessKey,
            mockCollectionId = "8183416-9575ed23-56ff-48aa-b6e2-f6b920992f1d",
            mockServerUrl = "https://1c6a81de-e4cd-432e-b2ce-cee1c0471766.mock.pstmn.io",
            json = appJson,
            logCalls = true
        ),
        appApiStartingUrl = "https://demo.cidirectinvesting.com/api/investment_accounts/dashboard"
    )
}