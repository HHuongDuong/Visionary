package com.example.visionmate.screens

import CameraPreview
import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.visionmate.ui.components.VMBottomBar
import com.example.visionmate.ui.components.VMTopBar
import com.example.visionmate.viewmodel.VisionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Home(viewModel: VisionViewModel) {
    var selectedFeature by remember { mutableStateOf("Recognize Text") }

    Scaffold(
        topBar = {
            VMTopBar()
        },
        bottomBar = {
            VMBottomBar(onRecognizeTextClick = { selectedFeature = "Recognize Text" })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            CameraPreview(onCapture = { bitmap ->
                viewModel.sendFrames(listOf(bitmap), selectedFeature)
            })
        }
    }
}