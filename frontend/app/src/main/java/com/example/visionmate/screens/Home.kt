package com.example.visionmate.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visionmate.ui.components.CameraPreview
import com.example.visionmate.ui.components.VMBottomBar
import com.example.visionmate.ui.components.VMTopBar
import com.example.visionmate.viewmodel.VisionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun Home(viewModel: VisionViewModel = viewModel()) {
    var selectedFeature by remember { mutableStateOf("Recognize Text") }
    val isRecording by viewModel.isRecording.collectAsState()
    val result by viewModel.result.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Log.d("HomeComposable", "Home composable is being executed")

    Scaffold(
        topBar = {
            VMTopBar()
        },
        bottomBar = {
            VMBottomBar(onRecognizeTextClick = { selectedFeature = "Recognize Text" })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            CameraPreview(
                isRecording = isRecording,
                onRecordButtonClick = {
                    viewModel.isRecording.value = !isRecording

                    Log.d("HomeComposable", "Recording state changed: ${viewModel.isRecording.value}")
                    if (viewModel.isRecording.value) {
                        Log.d("HomeComposable", "Started recording")
                    } else {
                        Log.d("HomeComposable", "Stopped recording")
                    }

                    coroutineScope.launch {
                        imageCapture?.let {
                            captureFramesAndSendToServer(viewModel, selectedFeature, context, it)
                        }
                    }
                },
                onImageCaptureCreated = { capture -> imageCapture = capture }
            )
            result?.let {
                Log.d("VisionMate", "API Result: $it")
                Text(text = it)
            }
        }
    }
}

suspend fun captureFramesAndSendToServer(viewModel: VisionViewModel, requestType: String, context: Context, imageCapture: ImageCapture) {
    Log.d("CaptureFrames", "captureFramesAndSendToServer started")
    withContext(Dispatchers.IO) {
        while (viewModel.isRecording.value) {
            delay(100)
            val imageProxy = captureImage(imageCapture, context)
            if (imageProxy == null) {
                Log.d("CaptureFrames", "Image capture failed or was null")
                break
            }

            val byteArray = imageProxyToByteArray(imageProxy)
            val requestBody = byteArray.toRequestBody()
            val videoPart = MultipartBody.Part.createFormData("video", "frame.jpg", requestBody)

            viewModel.testUpload(videoPart)

            imageProxy.close()

            delay(5000)
        }
        Log.d("CaptureFrames", "Exited while loop")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun captureImage(imageCapture: ImageCapture, context: Context): ImageProxy? {
    return withContext(Dispatchers.IO) {
        try {
            val imageProxy = suspendCancellableCoroutine<ImageProxy> { continuation ->
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            continuation.resume(image)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            continuation.resumeWithException(exception)
                        }
                    }
                )
            }
            imageProxy
        } catch (e: Exception) {
            Log.e("CaptureImage", "Error capturing image: ${e.message}")
            null
        }
    }
}

private fun imageProxyToByteArray(image: ImageProxy): ByteArray {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    Log.d("CaptureFrames", "Captured image size: ${bytes.size} bytes")
    return bytes
}
