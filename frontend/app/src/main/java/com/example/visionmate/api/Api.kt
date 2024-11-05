package com.example.visionmate.api

import com.example.visionmate.model.CurrencyResponse
import com.example.visionmate.model.DocumentResponse
import com.example.visionmate.model.ImageCaptionResponse
import com.example.visionmate.model.ProductResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Query
import javax.inject.Singleton
import javax.inject.Inject
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Part

@Singleton
class Api @Inject constructor(
    val documentApi: DocumentApi,
    val currencyApi: CurrencyApi,
    val imageApi: ImageApi,
    val downloadApi: DownloadApi
)

interface DocumentApi {
    @Multipart
    @POST("/document_recognition")
    suspend fun recognizeDocument(
        @Part file: MultipartBody.Part
    ): Response<DocumentResponse>

}

interface CurrencyApi {
    @Multipart
    @POST("/currency_detection")
    suspend fun detectCurrency(@Body file: MultipartBody.Part): Response<CurrencyResponse>
}

interface ImageApi {
    @Multipart
    @POST("/image_captioning")
    suspend fun captionImage(@Body file: MultipartBody.Part): Response<ImageCaptionResponse>

    @Multipart
    @POST("/product_recognition")
    suspend fun recognizeProduct(@Body file: MultipartBody.Part): Response<ProductResponse>
}

interface DownloadApi {
    @GET("/download_pdf")
    suspend fun downloadPdf(
        @Query("pdf_path") pdfPath: String
    ): Response<ResponseBody>

    @GET("/download_audio")
    suspend fun downloadAudio(
        @Query("audio_path") audioPath: String
    ): Response<ResponseBody>
}
