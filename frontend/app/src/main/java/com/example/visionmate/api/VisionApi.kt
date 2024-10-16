package com.example.visionmate.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VisionApi {
    @Multipart
    @POST("post") // Mock endpoint
    fun detectFaces(@Part image: MultipartBody.Part): Call<FaceDetectionResponse>

    @Multipart
    @POST("post") // Mock endpoint
    fun recognizeText(@Part image: MultipartBody.Part): Call<TextRecognitionResponse>
}

data class FaceDetectionResponse(val faces: List<String>)
data class TextRecognitionResponse(val text: String)