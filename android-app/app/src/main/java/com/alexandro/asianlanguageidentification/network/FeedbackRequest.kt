package com.alexandro.asianlanguageidentification.network

import com.google.gson.annotations.SerializedName

data class FeedbackRequest(
    @SerializedName("text") val text: String,
    @SerializedName("predicted_language") val predictedLanguage: String,
    @SerializedName("corrected_language") val correctedLanguage: String,
    @SerializedName("margin_score") val marginScore: Float?,
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("model_version") val modelVersion: String
)
