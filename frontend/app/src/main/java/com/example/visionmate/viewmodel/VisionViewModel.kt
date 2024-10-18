package com.example.visionmate.viewmodel

import android.graphics.Bitmap
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

class VisionViewModel : ViewModel() {
    private val visionRepository = VisionRepository()

    fun sendFrames(frames: List<Bitmap>, feature: String) {
        viewModelScope.launch {
            visionRepository.sendFrames(frames, feature)
        }
    }
}