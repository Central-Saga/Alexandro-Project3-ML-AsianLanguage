package com.alexandro.asianlanguageidentification.network

data class PredictionResponse(
    val status: String,
    val language_code: String,
    val language_name: String,
    val margin_score: Double,
    val text_length: Int
)
