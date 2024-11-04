package com.example.visionmate.repository

import com.example.visionmate.api.VisionApi
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import javax.inject.Inject

class VisionRepository @Inject constructor(private val api: VisionApi) {
    fun processVideo(video: MultipartBody.Part): Call<String> {
        return api.processVideo(video)
    }

    fun recognizeText(video: MultipartBody.Part): Call<String> {
        return api.recognizeText(video)
    }

    fun detectObjects(video: MultipartBody.Part): Call<String> {
        return api.detectObjects(video)
    }

    fun testUpload(video: MultipartBody.Part): Call<JsonObject> {
        return api.testUpload(video)
    }
}