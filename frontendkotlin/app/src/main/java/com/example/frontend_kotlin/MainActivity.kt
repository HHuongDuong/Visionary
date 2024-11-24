package com.example.frontend_kotlin

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var previewView: PreviewView
    private var selectedButton: MaterialButton? = null
    private var selectedButtonText: String? = null
    private lateinit var imageCapture: ImageCapture
    private lateinit var endpoints: Map<String, String>
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.camera_preview)

        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = tts.availableLanguages.firstOrNull { it.language == "vi" }
            }
        }

        endpoints = mapOf(
            getString(R.string.text) to "/document_recognition",
            getString(R.string.money) to "/currency_detection",
            getString(R.string.item) to "/image_captioning",
            getString(R.string.product) to "/product_recognition",
            getString(R.string.distance) to "/distance_estimate",
            getString(R.string.face) to "/face_detection/recognize"
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        val firstButton: MaterialButton = findViewById(R.id.text_button)
        firstButton.performClick()

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("vi", "VN")
        } else {
            Log.e("MainActivity", "TextToSpeech initialization failed")
        }
    }

    override fun onDestroy() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    fun onMenuClick(view: View) {
        Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
    }

    fun onQuestionMarkClick(view: View) {
        Toast.makeText(this, "Question mark clicked", Toast.LENGTH_SHORT).show()
    }


    fun onButtonClick(view: View) {
        if (view is MaterialButton) {
            selectedButton?.iconTint = ContextCompat.getColorStateList(this, R.color.primary)
            view.iconTint = ContextCompat.getColorStateList(this, R.color.selected)
            selectedButton = view
            selectedButtonText = view.contentDescription.toString()
            Log.d("MainActivity", "Button clicked: $selectedButtonText")

            tts.speak(selectedButtonText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun onCaptureClick(view: View) {
        val photoFile = File(externalMediaDirs.firstOrNull(), "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d("MainActivity", msg)
                    playCaptureSound()
                    sendImageToEndpoint(photoFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("MainActivity", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun playCaptureSound() {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    private fun sendImageToEndpoint(photoFile: File) {
        val endpoint = endpoints[selectedButtonText]
        if (endpoint == null) {
            Log.e("MainActivity", "No endpoint found for selected button: $selectedButtonText")
            return
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", photoFile.name, photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("http://112.137.129.161:8000$endpoint")
            .post(requestBody)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Error sending image to endpoint", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("MainActivity", "HTTP error! status: $response")
                    return
                }

                val jsonResponse = response.body?.string()
                if (jsonResponse != null) {
                    Log.d("MainActivity", "JSON Response: $jsonResponse")

                    val audioPath = JSONObject(jsonResponse).optString("audio_path")
                    if (audioPath.isNotEmpty()) {
                        val encodedAudioPath = Uri.encode(audioPath)
                        val audioFileUrl = "http://112.137.129.161:8000/download_audio?audio_path=$encodedAudioPath"
                        Log.d("MainActivity", "Audio File URL: $audioFileUrl")
                        playAudioFromUrl(audioFileUrl)
                    }
                } else {
                    Log.e("MainActivity", "Response body is null")
                }
            }
        })
    }

    private fun playAudioFromUrl(audioUrl: String) {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(audioUrl)
            setOnPreparedListener { start() }
            setOnCompletionListener { release() }
            setOnErrorListener { mp, what, extra ->
                Log.e("MainActivity", "Error playing audio: what=$what, extra=$extra")
                mp.release()
                true
            }
            prepareAsync()
        }
    }
}