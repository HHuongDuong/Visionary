package com.example.visionmate.api

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VisionApi {
    @Multipart
    @POST("process")
    fun processVideo(@Part video: MultipartBody.Part): Call<String>

    @Multipart
    @POST("recognizeText")
    fun recognizeText(@Part video: MultipartBody.Part): Call<String>

    @Multipart
    @POST("detectObjects")
    fun detectObjects(@Part video: MultipartBody.Part): Call<String>

    // TEST ENDPOINT
    @Multipart
    @POST("/post")
    fun testUpload(@Part video: MultipartBody.Part): Call<JsonObject>

}