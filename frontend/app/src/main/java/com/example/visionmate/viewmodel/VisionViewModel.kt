package com.example.visionmate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionmate.repository.VisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class VisionViewModel @Inject constructor(private val repository: VisionRepository) : ViewModel() {
    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    var isRecording = MutableStateFlow(false)

    fun processVideo(video: MultipartBody.Part, requestType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = when (requestType) {
                    "Recognize Text" -> repository.recognizeText(video).execute()
                    "Detect Objects" -> repository.detectObjects(video).execute()
                    else -> repository.processVideo(video).execute()
                }
                if (response.isSuccessful) {
                    _result.value = "Success: ${response.body()}"
                } else {
                    _result.value = "Error: ${response.message()}"
                }
            } catch (e: Exception) {
                _result.value = "Exception: ${e.message}"
            }
        }
    }

    fun testUpload(video: MultipartBody.Part) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.testUpload(video).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        _result.value = "Test Success: ${response.body()}"
                        Log.d("TestUpload", "Sending image of size: ${video.body?.contentLength()} bytes")
                    } else {
                        _result.value = "Test Error: ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _result.value = "Test Exception: ${e.message}"
                }
            }
        }
    }


}