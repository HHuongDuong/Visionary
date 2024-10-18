import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream

@Composable
fun CameraPreview(
    onCapture: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = {
                isRecording = !isRecording
                if (isRecording) {
                    coroutineScope.launch {
                        captureFramesAndSendToServer()
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    isRecording = !isRecording
                    if (isRecording) {
                        coroutineScope.launch {
                            captureFramesAndSendToServer()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRecording) "Pause" else "Record",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

suspend fun captureFramesAndSendToServer() {
    // Capture frames (this is a placeholder, implement your frame capture logic)
    val frames: List<Bitmap> = captureFrames()

    // Convert frames to ByteArray
    val byteArrayOutputStream = ByteArrayOutputStream()
    frames.forEach { frame ->
        frame.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    }
    val byteArray = byteArrayOutputStream.toByteArray()

    // Create MultipartBody.Part
    val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
    val multipartBody = MultipartBody.Part.createFormData("frames", "frames.jpg", requestBody)

    // Send frames to server
    withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.api.sendFrames(multipartBody, "feature_name").execute()
            if (response.isSuccessful) {
                Log.d("VisionMate", "Frames sent successfully")
            } else {
                Log.e("VisionMate", "Failed to send frames: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("VisionMate", "Error sending frames", e)
        }
    }
}

// Placeholder function to capture frames
fun captureFrames(): List<Bitmap> {
    // Implement your frame capture logic here
    return listOf()
}