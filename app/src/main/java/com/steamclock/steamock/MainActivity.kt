package com.steamclock.steamock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.steamclock.steamock.lib.PostmanMockConfig
import com.steamclock.steamock.lib.repo.MockState
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import com.steamclock.steamock.lib.ui.AvailableMocks
import com.steamclock.steamock.lib.ui.ContentLoadViewState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    //=================================================================
    // Postman mocking setup; in an actually app this would most likely
    // be setup via dependency injection.
    //=================================================================
    private val postmanConfig = PostmanMockConfig(
        postmanAccessKey = com.steamclock.steamock.lib_core.BuildConfig.postmanAccessKey,
        mockCollectionId = com.steamclock.steamock.lib_core.BuildConfig.postmanMockCollectionId,
        mockServerUrl = com.steamclock.steamock.lib_core.BuildConfig.postmanMockServerUrl,
        json = appJson,
        logCalls = true
    )
    private val postmanRepo = PostmanMockRepo(postmanConfig).apply {
        // Do not run HTTP requests if no mocks are enabled.
        mockState = MockState.MOCKS_ONLY
    }
    //=================================================================

    //=================================================================
    // Simulating a repository that would exist in an actual application.
    // This example is using Ktor to make HTTP requests and will use a
    // PostmanMockInterceptorKtor, along with our PostmanMockRepo to provide
    // mocks to the application.
    //
    // Again this would most likely be setup via dependency injection, and
    // is only setup here for simplicity of the example.
    //=================================================================
    private val exampleAPIClient = ExampleApiClient(
        json = appJson,
        mockingRepo = postmanRepo,
        logCalls = true
    )
    private val exampleApiRepo = ExampleApiRepo(exampleAPIClient)
    //=================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val mockCollectionState by postmanRepo.mockCollectionState.collectAsState()
            val exampleAPIResponse by exampleApiRepo.apiResponse.collectAsState()
            var exampleAPIUrl by remember { mutableStateOf(BuildConfig.exampleDefaultUrl) }
            var openAlertDialog by remember { mutableStateOf(true) }

            val stateText = when (val immutableState = mockCollectionState) {
                is ContentLoadViewState.Error -> immutableState.throwable.localizedMessage
                ContentLoadViewState.Loading -> "Fetching available Postman mocks..."
                else -> null
            }

            if (openAlertDialog) {
                WelcomeDialog { openAlertDialog = false }
            }

            if (exampleAPIResponse.isNotEmpty()) {
                ResponseDialog(
                    text = exampleAPIResponse,
                    onDismiss = { exampleApiRepo.clearLastResponse() }
                )
            }

            LaunchedEffect(Unit) {
                postmanRepo.requestCollectionUpdate()
            }

            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column {
                    when (mockCollectionState) {
                        is ContentLoadViewState.Error,
                        is ContentLoadViewState.Loading -> {
                            stateText?.let {
                                Text(modifier = Modifier.padding(16.dp), text = stateText)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        is ContentLoadViewState.Success -> {
                            // Input to allow us to test the interception
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .wrapContentSize()
                            ) {
                                Text(
                                    text = "Request Simulator",
                                    style = MaterialTheme.typography.h6
                                )

                                OutlinedTextField(
                                    value = exampleAPIUrl,
                                    onValueChange = { exampleAPIUrl = it }
                                )

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            exampleApiRepo.makeRequest(exampleAPIUrl)
                                        }
                                    }
                                ) {
                                    Text(text = "Send Request")
                                }
                            }

                            // Available mocks takes up the rest of the page
                            Box(modifier = Modifier
                                .weight(1f)
                                .border(4.dp, MaterialTheme.colors.primary)) {
                                AvailableMocks(
                                    mockRepo = postmanRepo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        title = {
            Text(text = "Postman Mocking Sample")
        },
        text = {
            Text(
                text = "This app is meant to demonstrate how to use Postman to mock APIs in your Android app." +
                        "To get started, you will need to update the local properties for the sample app with your mocking environment setup.\n\n" +
                        "Once setup, you can view and enable all mocks in the \"Available Postman Mocks\" section below." +
                        "The \"Intercept Requests\" section can be used to test how calls are intercepted and mocks are returned.",
                modifier = Modifier.padding(16.dp)
            )
        },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                onClick = { onDismissRequest() }
            ) {
                Text("Ok")
            }
        }
    )
}

@Composable
private fun ResponseDialog(
    text: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        content = {
            Column(
                modifier = Modifier.fillMaxHeight(0.7f).fillMaxWidth().background(Color.White)
            ) {
                Box(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .align(Alignment.End)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("Ok")
                    }
                }
            }
        }
    )
}