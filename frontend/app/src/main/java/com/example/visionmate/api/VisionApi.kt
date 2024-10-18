package com.example.visionmate.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VisionApi {
    @Multipart
    @POST("/process_frames")
    fun sendFrames(
        @Part frames: MultipartBody.Part,
        @Part("feature") feature: String
    ): Call<Void>
}