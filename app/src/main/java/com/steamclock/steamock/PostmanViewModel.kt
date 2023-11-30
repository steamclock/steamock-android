
// Left off todo,
// - This viewmodel was a weird hybrid, need to make something better for our example.
// - Could try to use toml file again now that I have the rest of the imports working


//package com.steamclock.steamock
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.steamclock.steamock.lib.mocks.MockState
//import com.steamclock.steamock.lib.mocks.PostmanMockRepo
//import com.steamclock.steamock.mocks.MockState
//import com.steamclock.steamock.mocks.PostmanMockRepo
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//class PostmanViewModel : ViewModel() {
//
//    // ============================================================
//    // TEST ENV SETUP
//    private val testEnv = PostmanMockEnv.CIDInvestingDemo
//    val mockRepo = PostmanMockRepo(testEnv.postmanMockConfig).apply {
//        // For this example we never actually want to make the *real* request.
//        mockState = MockState.MOCKS_ONLY
//    }
//    // ============================================================
//
//    private val mutableApiResponse = MutableStateFlow("")
//    val apiResponse = mutableApiResponse.asStateFlow()
//
//    //val appApiService = AppApiClient(appJson, mockRepo, testEnv.appApiStartingUrl)
//
//    fun queryMockCollection() {
//        viewModelScope.launch {
//            mockRepo.queryMockCollection(testEnv.postmanMockConfig.mockCollectionId)
//        }
//    }
//
//    fun runFakeAPI() {
//        viewModelScope.launch {
//            try {
//                mutableApiResponse.emit("Loading...")
//                mutableApiResponse.emit(appApiService.testingSpoofCall())
//            } catch (e: Exception) {
//                mutableApiResponse.emit(e.message.toString())
//            }
//        }
//    }
//}