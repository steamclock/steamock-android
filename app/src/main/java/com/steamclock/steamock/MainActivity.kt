package com.steamclock.steamock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.steamclock.steamock.lib.PostmanMockConfig
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.steamclock.steamock.lib.ui.ContentLoadViewState
import com.steamclock.steamock.lib.ui.AvailableMocks

class MainActivity : ComponentActivity() {

    //private val viewModel: PostmanViewModel by viewModels()
    private val postmanConfig = PostmanMockConfig(
        postmanAccessKey = BuildConfig.postmanAccessKey,
        mockCollectionId = "8183416-9575ed23-56ff-48aa-b6e2-f6b920992f1d",
        mockServerUrl = "https://7d081da6-0204-4452-aec6-3bafd98b933f.mock.pstmn.io",
        json = appJson,
        logCalls = true
    )
    private val postmanRepo = PostmanMockRepo(postmanConfig)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val mockCollectionState by postmanRepo.mockCollectionState.collectAsState()
            //val mockedDataResponse by viewModel.apiResponse.collectAsState()

            val stateText = when (val immutableState = mockCollectionState) {
                is ContentLoadViewState.Error -> immutableState.throwable.localizedMessage
                ContentLoadViewState.Loading -> "Loading"
                else -> null
            }

            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                LazyColumn() {
                    // Loading state
                    stateText?.let {
                        item {
                            Text(text = stateText)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // List all mocks in collection
                    item {
                        AvailableMocks(mockRepo = postmanRepo)
                    }

                    // Testing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(modifier = Modifier
                            .background(Color.LightGray)
                            .padding(16.dp)
                        ) {
                            Text(
                                text = "Simulating normal API usage",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Attempt request to:"
                            )

//                            OutlinedTextField(
//                                value = viewModel.appApiService.spoofAPIUrl,
//                                onValueChange = {
//                                    viewModel.appApiService.updateSpoofAPIUrl(it)
//                                }
//                            )

//                            Button(
//                                onClick = { viewModel.runFakeAPI() }
//                            ) {
//                                Text(text = "Send Request")
//                            }

//                            Text(
//                                text = mockedDataResponse
//                            )
                        }
                    }
                }
            }

        }

        lifecycleScope.launch {
            postmanRepo.requestCollectionUpdate()
        }
    }
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}