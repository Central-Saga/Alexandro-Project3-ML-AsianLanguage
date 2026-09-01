package com.alexandro.asianlanguageidentification.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LanguageApi {
    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): Response<PredictionResponse>
}
