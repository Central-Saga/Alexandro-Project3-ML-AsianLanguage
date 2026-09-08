package com.alexandro.asianlanguageidentification.network

import com.google.gson.annotations.SerializedName

data class LanguageResponse(
    @SerializedName("languages") val languages: List<String>
)
