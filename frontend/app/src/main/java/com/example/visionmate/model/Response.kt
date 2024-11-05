package com.example.visionmate.model;


data class CurrencyResponse(
    val total_money: String,
    val audio_path: Double
)

data class DocumentResponse(
    val text: String,
    val audio_path: String,
    val pdf_path: String
)

data class ImageCaptionResponse(
    val description: String,
    val audio_path: String
)

data class ProductResponse(
    val audio_path: String
)

