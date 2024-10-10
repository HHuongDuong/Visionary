package com.example.visionmate.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.visionmate.ui.components.CameraPermissionHandler
import com.example.visionmate.ui.components.VMBottomBar
import com.example.visionmate.ui.components.VMTopBar
import com.example.visionmate.viewmodel.VisionViewModel
import androidx.core.content.ContextCompat

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Home(viewModel: VisionViewModel) {
    val imageProxyState = remember { mutableStateOf<ImageProxy?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val context = LocalContext.current

    Scaffold(
        topBar = { VMTopBar() },
        bottomBar = {
            VMBottomBar(
                onDetectFacesClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                Log.d("Home", "Calling detectFaces")
                                viewModel.detectFaces(image)
                                imageProxyState.value = image
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("Home", "Image capture failed: ${exception.message}", exception)
                            }
                        }
                    )
                },
                onRecognizeTextClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                Log.d("Home", "Calling recognizeText")
                                viewModel.recognizeText(image)
                                imageProxyState.value = image
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("Home", "Image capture failed: ${exception.message}", exception)
                            }
                        }
                    )
                },
                onOtherActionClick = {
                    // Handle other actions
                }
            )
        }
    ) {
        CameraPermissionHandler(
            imageCapture = imageCapture,
            onImageCaptured = { imageProxy ->
                Log.d("Home", "ImageProxy captured: $imageProxy")
                imageProxyState.value = imageProxy
            }
        )
    }
}
