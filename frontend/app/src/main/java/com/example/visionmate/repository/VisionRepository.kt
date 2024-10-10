package com.example.visionmate.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.visionmate.api.FaceDetectionResponse
import com.example.visionmate.api.TextRecognitionResponse
import com.example.visionmate.api.VisionApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VisionRepository {

    private val visionApi: VisionApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://httpbin.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        visionApi = retrofit.create(VisionApi::class.java)
    }

    fun detectFaces(imageProxy: ImageProxy, callback: (List<String>) -> Unit) {
        val bitmap = imageProxy.toBitmap()
        val imagePart = bitmap.toMultipartBodyPart()

        visionApi.detectFaces(imagePart).enqueue(object : Callback<FaceDetectionResponse> {
            override fun onResponse(call: Call<FaceDetectionResponse>, response: Response<FaceDetectionResponse>) {
                if (response.isSuccessful) {
                    val faces = response.body()?.faces ?: emptyList()
                    Log.d("VisionRepository", "Face detection successful: $faces")
                    callback(faces)
                } else {
                    val errorMessage = "Face detection failed: ${response.message()}"
                    Log.d("VisionRepository", errorMessage)
                    callback(listOf(errorMessage))
                }
            }

            override fun onFailure(call: Call<FaceDetectionResponse>, t: Throwable) {
                val errorMessage = "Face detection failed: ${t.message}"
                Log.d("VisionRepository", errorMessage)
                callback(listOf(errorMessage))
            }
        })

        imageProxy.close()
    }

    fun recognizeText(imageProxy: ImageProxy, callback: (String) -> Unit) {
        val bitmap = imageProxy.toBitmap()
        val imagePart = bitmap.toMultipartBodyPart()

        visionApi.recognizeText(imagePart).enqueue(object : Callback<TextRecognitionResponse> {
            override fun onResponse(call: Call<TextRecognitionResponse>, response: Response<TextRecognitionResponse>) {
                if (response.isSuccessful) {
                    val text = response.body()?.text ?: ""
                    Log.d("VisionRepository", "Text recognition successful: $text")
                    callback(text)
                } else {
                    val errorMessage = "Text recognition failed: ${response.message()}"
                    Log.d("VisionRepository", errorMessage)
                    callback(errorMessage)
                }
            }

            override fun onFailure(call: Call<TextRecognitionResponse>, t: Throwable) {
                val errorMessage = "Text recognition failed: ${t.message}"
                Log.d("VisionRepository", errorMessage)
                callback(errorMessage)
            }
        })

        imageProxy.close()
    }

    private fun Bitmap.toMultipartBodyPart(): MultipartBody.Part {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
        return MultipartBody.Part.createFormData("image", "image.jpg", requestBody)
    }
}