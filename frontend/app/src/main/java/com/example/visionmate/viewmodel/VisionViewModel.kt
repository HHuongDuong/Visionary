package com.example.visionmate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.ImageProxy
import com.example.visionmate.repository.VisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisionViewModel @Inject constructor(
    private val repository: VisionRepository
) : ViewModel() {

    private val _faceDetectionResult = MutableStateFlow<List<String>>(emptyList())
    val faceDetectionResult: StateFlow<List<String>> = _faceDetectionResult.asStateFlow()

    private val _textRecognitionResult = MutableStateFlow<String>("")
    val textRecognitionResult: StateFlow<String> = _textRecognitionResult.asStateFlow()

    fun detectFaces(imageProxy: ImageProxy) {
        Log.d("VisionViewModel", "Detecting faces...")
        repository.detectFaces(imageProxy) { result ->
            Log.d("VisionViewModel", "Face detection result: $result")
            _faceDetectionResult.value = result
        }
    }

    fun recognizeText(imageProxy: ImageProxy) {
        Log.d("VisionViewModel", "Recognizing text...")
        repository.recognizeText(imageProxy) { result ->
            Log.d("VisionViewModel", "Text recognition result: $result")
            _textRecognitionResult.value = result
        }
    }
}