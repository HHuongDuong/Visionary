package com.example.visionmate.repository

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.example.visionmate.api.FaceDetectionResponse
import com.example.visionmate.api.TextRecognitionResponse
import com.example.visionmate.api.VisionApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VisionRepository {

    private val _faceDetectionResult = MutableStateFlow<List<String>>(emptyList())
    val faceDetectionResult: StateFlow<List<String>> = _faceDetectionResult.asStateFlow()

    private val _textRecognitionResult = MutableStateFlow<String>("")
    val textRecognitionResult: StateFlow<String> = _textRecognitionResult.asStateFlow()

    private val visionApi: VisionApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://your-fastapi-backend-url/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        visionApi = retrofit.create(VisionApi::class.java)
    }

    fun analyzeImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val imageRequestBody = bitmap.toRequestBody()

        // Send image to FastAPI for face detection
        visionApi.detectFaces(imageRequestBody).enqueue(object : Callback<FaceDetectionResponse> {
            override fun onResponse(call: Call<FaceDetectionResponse>, response: Response<FaceDetectionResponse>) {
                if (response.isSuccessful) {
                    _faceDetectionResult.update { response.body()?.faces ?: emptyList() }
                } else {
                    _faceDetectionResult.update { listOf("Face detection failed: ${response.message()}") }
                }
            }

            override fun onFailure(call: Call<FaceDetectionResponse>, t: Throwable) {
                _faceDetectionResult.update { listOf("Face detection failed: ${t.message}") }
            }
        })

        // Send image to FastAPI for text recognition
        visionApi.recognizeText(imageRequestBody).enqueue(object : Callback<TextRecognitionResponse> {
            override fun onResponse(call: Call<TextRecognitionResponse>, response: Response<TextRecognitionResponse>) {
                if (response.isSuccessful) {
                    _textRecognitionResult.update { response.body()?.text ?: "" }
                } else {
                    _textRecognitionResult.update { "Text recognition failed: ${response.message()}" }
                }
            }

            override fun onFailure(call: Call<TextRecognitionResponse>, t: Throwable) {
                _textRecognitionResult.update { "Text recognition failed: ${t.message}" }
            }
        })

        imageProxy.close()
    }

    private fun Bitmap.toRequestBody(): RequestBody {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return RequestBody.Companion.create("image/jpeg".toMediaTypeOrNull(), byteArray)
    }
}