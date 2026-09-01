package com.alexandro.asianlanguageidentification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandro.asianlanguageidentification.network.PredictionRequest
import com.alexandro.asianlanguageidentification.network.PredictionResponse
import com.alexandro.asianlanguageidentification.network.RetrofitClient
import kotlinx.coroutines.launch
import android.util.Log

class LanguageDetectionViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    
    var predictionResult by mutableStateOf<PredictionResponse?>(null)
        private set
        
    var errorMessage by mutableStateOf<String?>(null)
        private set

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
    }
}
