package com.alexandro.asianlanguageidentification

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alexandro.asianlanguageidentification.data.local.DetectionHistoryItem
import com.alexandro.asianlanguageidentification.data.local.HistoryRepository
import com.alexandro.asianlanguageidentification.network.FeedbackRequest
import com.alexandro.asianlanguageidentification.network.PredictionRequest
import com.alexandro.asianlanguageidentification.network.PredictionResponse
import com.alexandro.asianlanguageidentification.network.RetrofitClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class LanguageDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository(application)

    // State Riwayat Deteksi Lokal
    val historyList: StateFlow<List<DetectionHistoryItem>> = historyRepository.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    private var lastPredictedText: String = ""

    companion object {
        private const val TAG = "LanguageDetection"
        const val FRIENDLY_NETWORK_ERROR =
            "Tidak dapat terhubung ke server. Pastikan backend aktif dan koneksi perangkat telah disiapkan."
        val FALLBACK_LANGUAGES = listOf(
            "fil_tl",
            "indonesian",
            "khmer",
            "lao",
            "malay",
            "thai",
            "vietnamese",
            "japanese",
            "javanese"
        )
    }

    init {
        fetchLanguages()
    }

    fun fetchLanguages() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getLanguages()
                if (response.isSuccessful) {
                    val languages = response.body()?.languages
                    if (!languages.isNullOrEmpty()) {
                        activeLanguages = languages
                    } else {
                        activeLanguages = FALLBACK_LANGUAGES
                    }
                } else {
                    Log.w(TAG, "Gagal mengambil daftar bahasa (HTTP ${response.code()}), menggunakan fallback lokal.")
                    activeLanguages = FALLBACK_LANGUAGES
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saat mengambil daftar bahasa dari backend: ${e.message}", e)
                activeLanguages = FALLBACK_LANGUAGES
            }
        }
    }

    fun detectLanguage(text: String) {
        val trimmed = text.trim()
        if (trimmed.length < 20 || isLoading) return

        isLoading = true
        errorMessage = null
        predictionResult = null
        feedbackMessage = null
        lastPredictedText = trimmed

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.predict(PredictionRequest(trimmed))
                if (response.isSuccessful) {
                    val body = response.body()
                    predictionResult = body
                    if (body != null) {
                        // Simpan ke riwayat lokal
                        val isUnknown = body.status == "unknown" || body.language_code == "unknown"
                        val snippet = if (trimmed.length > 80) trimmed.take(80) + "…" else trimmed
                        val historyItem = DetectionHistoryItem(
                            textSnippet = snippet,
                            fullText = trimmed,
                            languageName = if (isUnknown) "Bahasa tidak dikenali" else body.language_name,
                            languageCode = if (isUnknown) "unknown" else body.language_code,
                            isSuccess = !isUnknown,
                            marginScore = body.margin_score
                        )
                        historyRepository.addHistory(historyItem)
                    }
                } else if (response.code() == 422) {
                    errorMessage = "Teks tidak valid atau panjang teks kurang dari 20 karakter."
                } else {
                    errorMessage = "Terjadi kesalahan pada server (HTTP ${response.code()}). Silakan coba beberapa saat lagi."
                }
            } catch (e: IOException) {
                Log.e(TAG, "IOException saat prediksi bahasa: ${e.message}", e)
                errorMessage = FRIENDLY_NETWORK_ERROR
            } catch (e: Exception) {
                Log.e(TAG, "Exception tidak terduga saat prediksi: ${e.message}", e)
                errorMessage = FRIENDLY_NETWORK_ERROR
            } finally {
                isLoading = false
            }
        }
    }

    fun retryLastDetection() {
        if (lastPredictedText.isNotBlank()) {
            detectLanguage(lastPredictedText)
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
                    modelVersion = "1.0.0"
                )
                val response = RetrofitClient.api.submitFeedback(req)
                if (response.isSuccessful) {
                    feedbackMessage = "Koreksi berhasil dikirim dan menunggu validasi."
                } else {
                    feedbackMessage = "Gagal mengirim koreksi (HTTP ${response.code()})."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengirimkan feedback: ${e.message}", e)
                feedbackMessage = "Gagal mengirimkan feedback. Periksa koneksi jaringan Anda."
            } finally {
                isFeedbackSubmitting = false
            }
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            historyRepository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
        }
    }
}
