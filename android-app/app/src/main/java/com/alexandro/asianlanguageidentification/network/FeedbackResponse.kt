package com.alexandro.asianlanguageidentification.network

import com.google.gson.annotations.SerializedName

data class FeedbackResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)
