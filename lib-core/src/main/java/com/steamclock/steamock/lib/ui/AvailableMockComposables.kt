package com.steamclock.steamock.lib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steamclock.steamock.lib.repo.PostmanMockRepo
import com.steamclock.steamock.lib.api.Postman
import com.steamclock.steamock.lib.repo.ApiName
import kotlinx.coroutines.launch

/**
 * Shows a list of all available mocks, given a PostmanMockRepo
 */
@Composable
fun AvailableMocks(
    modifier: Modifier = Modifier,
    mockRepo: PostmanMockRepo
) {
    val mockCollection by mockRepo.mockCollection.collectAsState()
    val enabledMocks by mockRepo.enabledMocks.collectAsState()
    val mockingGroups by mockRepo.mockGroups.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var selectedGroupName by remember { mutableStateOf<String?>(null) }

    mockCollection?.let { collection ->
        AvailableMocks(
            modifier = modifier,
            collection = collection,
            enabledMocks = enabledMocks,
            mockResponseDelayMs = mockRepo.mockResponseDelayMs,
            onUpdateMockDelayMs = { mockRepo.mockResponseDelayMs = it }, // todo Flow?
            onAllMocksCleared = {
                coroutineScope.launch {
                    mockRepo.clearAllMocks()
                }
            },
            onMockChanged = { apiId, mock ->
                coroutineScope.launch {
                    if (mock != null) {
                        mockRepo.enableMock(apiId, mock)
                    } else {
                        mockRepo.disableMock(apiId)
                    }
                }
            },

            // Support for mocking "groups"
            availableGroups = mockingGroups,
            selectedGroupName = selectedGroupName,
            onMockGroupSelected = { name ->
                selectedGroupName = name
                name?.let {
                    coroutineScope.launch {
                        mockRepo.enableAllMocksForGroup(name)
                    }
                }
            }
        )
    }
}

/**
 * Will show the API name and a list of all available mocks for that API. Users will  be able to
 * select and deselect mocks via checkboxes.
 */
@Composable
private fun MocksForApi(
    modifier: Modifier = Modifier,
    api: Postman.TypedItem.API,
    enabledMock: Postman.SavedMock?,
    onMockSelected: (Postman.SavedMock) -> Unit,
    onMockDeselected: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val icon = if (isExpanded) {
        Icons.Default.KeyboardArrowUp
    } else {
        Icons.Default.KeyboardArrowDown
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = api.name,
                fontSize = 16.sp
            )

            Text(
                text = enabledMock?.name ?: "",
                color = Color.DarkGray,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp
            )
        }

        Icon(
            icon, contentDescription = null
        )
    }

    // List all available mocks for the API...
    if (isExpanded) {
        api.response.forEach { mock ->
            AvailableMock(
                mock = mock,
                isEnabled = (enabledMock?.id == mock.id),
                onCheckedChange = { checked ->
                    if (checked) {
                        onMockSelected(mock)
                    } else {
                        onMockDeselected()
                    }
                }
            )
        }
    }
}

/**
 * PostmanItem may be a "Folder" of items, or an API; this Composable will iteratively call itself
 * when a Folder is present, as a folder may contain multiple items.
 */
@Composable
private fun PostmanItem(
    item: Postman.Item,
    startOffset: Dp = 0.dp, // Used to indent folder items for visual grouping
    enabledMocks: Map<ApiName, Postman.SavedMock>?,
    onMockChanged: (ApiName, Postman.SavedMock?) -> Unit // ApiName, MockRawUrl, Selected
) {
    when (val typed = Postman.TypedItem.from(item)) {
        is Postman.TypedItem.API -> {
            if (typed.response.isNullOrEmpty()) {
                // If we have no mocks for the API, we don't want to show anything.
                return
            }

            // Else show all the mocks available for the given API
            val enabledMock = enabledMocks?.get(item.name)
            Spacer(modifier = Modifier.height(8.dp))
            MocksForApi(
                modifier = Modifier.padding(start = startOffset),
                api = typed,
                enabledMock = enabledMock,
                onMockSelected = { mock -> onMockChanged(typed.name, mock) },
                onMockDeselected = { onMockChanged(typed.name, null) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        is Postman.TypedItem.Folder -> {
            // We have a folder of possible items, show the older name and then iterate
            // through all items in the folder.
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                text = typed.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            typed.item.forEach { item ->
                PostmanItem(
                    item = item,
                    startOffset = startOffset + 12.dp,
                    enabledMocks = enabledMocks,
                    onMockChanged = onMockChanged
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AvailableMocks(
    modifier: Modifier,
    collection: Postman.Collection,
    enabledMocks: Map<ApiName, Postman.SavedMock>?,
    mockResponseDelayMs: Int,
    onUpdateMockDelayMs: (Int) -> Unit,
    onAllMocksCleared: () -> Unit,
    onMockChanged: (ApiName, Postman.SavedMock?) -> Unit, // ApiName, MockRawUrl, Selected
    // Support for mock "groups"
    availableGroups: Set<String>,
    selectedGroupName: String?,
    onMockGroupSelected: (String?) -> Unit // GroupName
) {
    var delay by rememberSaveable { mutableStateOf(mockResponseDelayMs) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // todo delay can input letters which breaks, validate input

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        //---------------------------------------------------------------
        // Collection name
        //---------------------------------------------------------------
        Text(
            text = "Mocks available for ${collection.info.name}",
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(8.dp))

        // todo left off scroll up and down aren't working?

        //---------------------------------------------------------------
        // Global actions
        //---------------------------------------------------------------
        GlobalActions(
            delay = delay,
            onDelayUpdated = { delay = it },
            onDelayDone = {
                onUpdateMockDelayMs(delay)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
            availableGroups = availableGroups,
            selectedGroupName = selectedGroupName,
            onGroupSelected = { onMockGroupSelected.invoke(it) },
            onAllMocksCleared = onAllMocksCleared
        )

        Spacer(modifier = Modifier.height(16.dp))

        //---------------------------------------------------------------
        // APIs in collection
        //---------------------------------------------------------------
        collection.let { collection ->
            // Show each item (API) in the collection that has at least one saved mock
            collection.item.forEach {
                PostmanItem(
                    item = it,
                    enabledMocks = enabledMocks,
                    onMockChanged = onMockChanged
                )
            }
        }
    }
}

@Composable
private fun GlobalActions(
    delay: Int,
    onDelayUpdated: (Int) -> Unit,
    onDelayDone: () -> Unit,
    availableGroups: Set<String>,
    selectedGroupName: String?,
    onGroupSelected: (String?) -> Unit,
    onAllMocksCleared: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        TextField(
            modifier = Modifier.weight(1f),
            value = delay.toString(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onDelayDone()
                }
            ),
            onValueChange = {
                val result = it.replaceFirst("^0+(?!$)", "")
                val delay = if (result.isBlank()) { 0 } else { result.toInt() }
                onDelayUpdated(delay)
            },
            label = {
                Text("Delay (in ms)")
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MockingColors.inputBackground
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Only show group dropdown selector if there are groups available.
        if (availableGroups.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                GroupDropDownMenu(
                    groupNames = availableGroups,
                    selectedName = selectedGroupName,
                    onSelected = { onGroupSelected(it) }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Button(
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight(),
            onClick = onAllMocksCleared
        ) {
            Text(
                text = "Clear"
            )
        }
    }
}

@Composable
fun GroupDropDownMenu(
    groupNames: Set<String>,
    selectedName: String?,
    onSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = { isExpanded = true })
            .background(MockingColors.inputBackground),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Select Group",
                modifier = Modifier.wrapContentSize()
            )

            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier
                .background(MockingColors.inputBackground)
        ) {
            groupNames.forEachIndexed { index, name ->
                DropdownMenuItem(onClick = {
                    onSelected.invoke(name)
                    isExpanded = false
                }) {
                    Text(text = name)
                }
            }
        }
    }
}

@Composable
fun AvailableMock(
    modifier: Modifier = Modifier,
    mock: Postman.SavedMock,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isEnabled,
            onCheckedChange = onCheckedChange
        )

        Column(
            modifier = modifier.weight(1f)
        ) {
            Text(
                text = mock.name,
                fontSize = 16.sp
            )
            Text(
                text = mock.originalRequest.url.fullPathAndQueryString,
                color = Color.DarkGray,
                fontSize = 10.sp
            )
        }
    }
}


@Composable
@Preview(widthDp = 400, showBackground = true, backgroundColor = 0xFFFFFFFF)
fun GlobalActionsPreview() {
    MaterialTheme {
        GlobalActions(
            delay = 1000,
            onDelayUpdated = {},
            onDelayDone = {},
            availableGroups = setOf("Group 1", "Group 2"),
            selectedGroupName = "Group 1",
            onGroupSelected = {},
            onAllMocksCleared = {}
        )
    }
}


@Composable
@Preview(widthDp = 200)
fun AvailableMockPreview() {
    MaterialTheme {
        AvailableMock(
            isEnabled = true,
            mock = Postman.SavedMock(
                id = "1",
                name = "test",
                originalRequest = Postman.OriginalRequest(
                    method = "GET",
                    url = Postman.Url(
                        raw = "https://api.test.gypsyguide.com/v2/catalog?valid=true",
                        protocol = "https",
                        host = listOf("api", "test", "gypsyguide", "com"),
                        path = listOf("v2", "catalog"),
                        query = listOf(Postman.Query("valid", "true", description = "test desc"))
                    )
                ),
                status = "OK",
                code = 200,
                uid = "???"
            ),
            onCheckedChange = {
            }
        )
    }
}

