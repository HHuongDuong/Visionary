package com.example.visionmate.repository

import com.example.visionmate.api.Api
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import javax.inject.Inject

class Repository @Inject constructor(private val api: Api) {
    suspend fun recognizeDocument(file: MultipartBody.Part) = api.documentApi.recognizeDocument(file)
    suspend fun detectCurrency(file: MultipartBody.Part) = api.currencyApi.detectCurrency(file)
    suspend fun captionImage(file: MultipartBody.Part) = api.imageApi.captionImage(file)
    suspend fun recognizeProduct(file: MultipartBody.Part) = api.imageApi.recognizeProduct(file)
    suspend fun downloadPdf(pdfPath: String) = api.downloadApi.downloadPdf(pdfPath)
    suspend fun downloadAudio(audioPath: String) = api.downloadApi.downloadAudio(audioPath)
}