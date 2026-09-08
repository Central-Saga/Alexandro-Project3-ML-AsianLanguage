package com.alexandro.asianlanguageidentification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandro.asianlanguageidentification.network.PredictionRequest
import com.alexandro.asianlanguageidentification.network.PredictionResponse
import com.alexandro.asianlanguageidentification.network.RetrofitClient
import com.alexandro.asianlanguageidentification.network.FeedbackRequest
import kotlinx.coroutines.launch
import android.util.Log

class LanguageDetectionViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    
    var predictionResult by mutableStateOf<PredictionResponse?>(null)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var activeLanguages by mutableStateOf<List<String>>(emptyList())
        private set
        
    var isFeedbackSubmitting by mutableStateOf(false)
        private set
        
    var feedbackMessage by mutableStateOf<String?>(null)
        private set
        
    init {
        fetchLanguages()
    }

    private fun fetchLanguages() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getLanguages()
                if (response.isSuccessful) {
                    activeLanguages = response.body()?.languages ?: emptyList()
                } else {
                    Log.w("LanguageDetection", "Gagal mengambil daftar bahasa, menggunakan fallback.")
                }
            } catch (e: Exception) {
                Log.e("LanguageDetection", "Error mengambil daftar bahasa", e)
            }
        }
    }

    fun detectLanguage(text: String) {
        if (text.trim().length < 20) return
        
        isLoading = true
        errorMessage = null
        predictionResult = null
        
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.predict(PredictionRequest(text))
                if (response.isSuccessful) {
                    predictionResult = response.body()
                } else if (response.code() == 422) {
                    errorMessage = "Data tidak valid atau kurang panjang."
                } else {
                    errorMessage = "Terjadi kesalahan pada server (HTTP ${response.code()})"
                }
            }catch (e: Exception) {
                Log.e("LanguageDetection", "Request prediksi gagal", e)
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            }
            finally {
                isLoading = false
            }
        }
    }
    
    fun clearResult() {
        predictionResult = null
        errorMessage = null
        feedbackMessage = null
    }

    fun submitFeedback(
        text: String,
        predictedLanguage: String,
        correctedLanguage: String,
        marginScore: Float?,
        isCorrect: Boolean
    ) {
        isFeedbackSubmitting = true
        feedbackMessage = null
        
        viewModelScope.launch {
            try {
                val req = FeedbackRequest(
                    text = text,
                    predictedLanguage = predictedLanguage,
                    correctedLanguage = correctedLanguage,
                    marginScore = marginScore,
                    isCorrect = isCorrect,
                    modelVersion = "1.0.0" // Default for now
                )
                val response = RetrofitClient.api.submitFeedback(req)
                if (response.isSuccessful) {
                    feedbackMessage = "Koreksi berhasil dikirim dan menunggu validasi."
                } else {
                    feedbackMessage = "Gagal mengirim koreksi (HTTP ${response.code()})."
                }
            } catch (e: Exception) {
                Log.e("LanguageDetection", "Request feedback gagal", e)
                feedbackMessage = "Error: ${e.message}"
            } finally {
                isFeedbackSubmitting = false
            }
        }
    }
}
