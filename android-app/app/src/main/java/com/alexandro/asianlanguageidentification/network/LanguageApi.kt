package com.alexandro.asianlanguageidentification.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

interface LanguageApi {
    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): Response<PredictionResponse>

    @POST("feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest): Response<FeedbackResponse>

    @GET("languages")
    suspend fun getLanguages(): Response<LanguageResponse>
}
