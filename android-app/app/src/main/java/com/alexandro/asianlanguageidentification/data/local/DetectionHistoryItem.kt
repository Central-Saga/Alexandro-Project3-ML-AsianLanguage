package com.alexandro.asianlanguageidentification.data.local

import java.util.UUID

/**
 * Representasi entitas riwayat deteksi bahasa.
 */
data class DetectionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val textSnippet: String,
    val fullText: String,
    val languageName: String,
    val languageCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val marginScore: Double? = null
)
