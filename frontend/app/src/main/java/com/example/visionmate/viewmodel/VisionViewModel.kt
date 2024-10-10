package com.example.visionmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.ImageProxy
import com.example.visionmate.repository.VisionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VisionViewModel @Inject constructor(
    private val repository: VisionRepository
) : ViewModel() {

    val faceDetectionResult: StateFlow<List<String>> = repository.faceDetectionResult
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val textRecognitionResult: StateFlow<String> = repository.textRecognitionResult
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun analyzeImage(imageProxy: ImageProxy) {
        repository.analyzeImage(imageProxy)
    }
}