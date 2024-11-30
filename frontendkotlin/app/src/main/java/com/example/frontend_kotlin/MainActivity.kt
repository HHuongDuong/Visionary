package com.example.frontend_kotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import com.example.frontend_kotlin.utils.Utils
import android.speech.tts.UtteranceProgressListener
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

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val BASE_URL = "http://112.137.129.161:8000"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        lateinit var tts: TextToSpeech
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.camera_preview)

        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = Locale("vi", "VN")
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("MainActivity", "TTS started")
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            val intent = Intent(this@MainActivity, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("MainActivity", "TTS error")
                    }
                })
            } else {
                Log.e("MainActivity", "TextToSpeech initialization failed")
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
        tts.stop()
        tts.shutdown()
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
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
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
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
                    tts.speak("Chụp ảnh thành công", TextToSpeech.QUEUE_FLUSH, null, null)
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
        Log.d("sendImageToEndpoint", "Endpoint: $endpoint")
        if (endpoint == null) {
            return
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        val mediaType = "image/png".toMediaTypeOrNull()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", photoFile.name, photoFile.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url("$BASE_URL$endpoint")
            .post(requestBody)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "multipart/form-data")
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Error sending image to endpoint", e)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("MainActivity", "HTTP error! status: ${response.code}")
                    return
                } else {
                    Log.d("MainActivity", "HTTP success! status: ${response.code}")
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e("MainActivity", "Empty response body")
                    return
                }

                Log.d("MainActivity", "Response body: $responseBody")

                val jsonObject = JSONObject(responseBody)
                val text: String? = jsonObject.optString("text", null)
                val totalMoney: String? = jsonObject.optString("total_money", null)
                val description: String? = jsonObject.optString("description", null)
                // Add more here

                val responseText = when (selectedButtonText) {
                    getString(R.string.text) -> text
                    getString(R.string.money) -> totalMoney
                    getString(R.string.item) -> description
                    getString(R.string.product) -> TODO()
                    getString(R.string.distance) -> TODO()
                    getString(R.string.face) -> TODO()
                    else -> "Không thể xử lý yêu cầu"
                }
                Log.d("MainActivity", "Response text: $responseText")
                tts.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")

                // document recognition activity screen
                if (selectedButtonText == getString(R.string.text)) {
                    val intent = Intent(this@MainActivity, DocumentActivity::class.java).apply {
                        putExtra("responseText", responseText)
                    }
                    startActivity(intent)
                }
            }
        })
    }

}
