package com.example.visionmate.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
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
            .baseUrl("http://yourserver.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        visionApi = retrofit.create(VisionApi::class.java)
    }

    suspend fun sendFrames(frames: List<Bitmap>, feature: String) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        frames.forEach { frame ->
            frame.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        }
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArrayOutputStream.toByteArray())
        val body = MultipartBody.Part.createFormData("frames", "frames.jpg", requestBody)

        visionApi.sendFrames(body, feature).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("VisionRepository", "Frames sent successfully")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("VisionRepository", "Failed to send frames", t)
            }
        })
    }
}