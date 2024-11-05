package com.example.visionmate.di

import com.example.visionmate.api.CurrencyApi
import com.example.visionmate.api.DocumentApi
import com.example.visionmate.api.ImageApi
import com.example.visionmate.api.DownloadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.net.InetAddress

const val PORT = 8080
val BASE_URL = "http://${InetAddress.getLocalHost().hostAddress}:${PORT}/"

@Module
@InstallIn(SingletonComponent::class)
object VisionModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDocumentApi(retrofit: Retrofit): DocumentApi {
        return retrofit.create(DocumentApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCurrencyApi(retrofit: Retrofit): CurrencyApi {
        return retrofit.create(CurrencyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImageApi(retrofit: Retrofit): ImageApi {
        return retrofit.create(ImageApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDownloadApi(retrofit: Retrofit): DownloadApi {
        return retrofit.create(DownloadApi::class.java)
    }

}
