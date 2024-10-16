package com.example.visionmate.di

import com.example.visionmate.repository.VisionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object VisionModule {

    @Provides
    @ViewModelScoped
    fun provideVisionRepository(): VisionRepository {
        return VisionRepository()
    }
}