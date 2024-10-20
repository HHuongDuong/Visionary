package com.example.visionmate.di

import com.example.visionmate.api.VisionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

const val BASE_URL = "https://httpbin.org/"

@Module
@InstallIn(SingletonComponent::class)
object VisionModule {
    @Provides
    @Singleton
    fun provideVisionApi(): VisionApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VisionApi::class.java)
    }
}