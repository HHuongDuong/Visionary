package com.example.visionmate.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VisionApi {
    // Define the API endpoints here

    @Multipart
    @POST("detect_faces") // Could be changed
    fun detectFaces(@Part image: RequestBody): Call<FaceDetectionResponse>

    @Multipart
    @POST("recognize_text") // Could be changed
    fun recognizeText(@Part image: RequestBody): Call<TextRecognitionResponse>
}

data class FaceDetectionResponse(val faces: List<String>)
data class TextRecognitionResponse(val text: String)