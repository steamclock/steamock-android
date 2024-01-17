package com.steamclock.steamock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.steamclock.steamock.lib.PostmanMockConfig
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.steamclock.steamock.lib.repo.MockState
import com.steamclock.steamock.lib.ui.ContentLoadViewState
import com.steamclock.steamock.lib.ui.AvailableMocks

class MainActivity : ComponentActivity() {

    //=================================================================
    // Postman mocking setup; in an actually app this would most likely
    // be setup via dependency injection.
    //=================================================================
    private val postmanConfig = PostmanMockConfig(
        postmanAccessKey = com.steamclock.steamock.lib.BuildConfig.postmanAccessKey,
        mockCollectionId = com.steamclock.steamock.lib.BuildConfig.postmanMockCollectionId,
        mockServerUrl = com.steamclock.steamock.lib.BuildConfig.postmanMockServerUrl,
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
            var showingStubs by remember { mutableStateOf(false) }
            var showingExample by remember { mutableStateOf(false) }

            val stateText = when (val immutableState = mockCollectionState) {
                is ContentLoadViewState.Error -> immutableState.throwable.localizedMessage
                ContentLoadViewState.Loading -> "Fetching available Postman mocks..."
                else -> null
            }

            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                LazyColumn {
                    when (mockCollectionState) {
                        is ContentLoadViewState.Error,
                        is ContentLoadViewState.Loading -> {
                            stateText?.let {
                                item {
                                    Text(modifier = Modifier.padding(16.dp), text = stateText)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                        is ContentLoadViewState.Success -> {
                            // List all mocks
                            item {
                                CollapsableContent(
                                    title = "Available Postman Mocks",
                                    isExpanded = showingStubs,
                                    onRowClicked = { showingStubs = !showingStubs },
                                    content = { AvailableMocks(mockRepo = postmanRepo) }
                                )
                            }

                            // Setup intercept requests
                            item {
                                Spacer(modifier = Modifier.height(16.dp))

                                CollapsableContent(
                                    title = "Intercept Requests",
                                    isExpanded = showingExample,
                                    onRowClicked = { showingExample = !showingExample },
                                    content = {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
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

                                            Text(
                                                text = exampleAPIResponse
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                }

                if (openAlertDialog) {
                    WelcomeDialog { openAlertDialog = false }
                }
            }
        }

        lifecycleScope.launch {
            postmanRepo.requestCollectionUpdate()
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
private fun CollapsableContent(
    title: String,
    isExpanded : Boolean,
    onRowClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    Divider()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClicked() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f).padding(8.dp),
            fontSize = 24.sp,
            text = title,
        )
        Icon(
            modifier = Modifier.wrapContentSize().padding(8.dp),
            imageVector =  if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
    Divider()

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}