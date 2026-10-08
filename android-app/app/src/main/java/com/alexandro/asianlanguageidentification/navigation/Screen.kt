package com.alexandro.asianlanguageidentification.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    SPLASH(
        title = "Memuat...",
        icon = Icons.Default.Translate,
        description = "Splash Screen"
    ),
    DETECTION(
        title = "Deteksi Bahasa",
        icon = Icons.Default.Translate,
        description = "Identifikasi bahasa Asia dari teks"
    ),
    HISTORY(
        title = "Riwayat Deteksi",
        icon = Icons.Default.History,
        description = "Lihat riwayat hasil deteksi bahasa sebelumnya"
    ),
    SUPPORTED_LANGUAGES(
        title = "Bahasa Didukung",
        icon = Icons.Default.Language,
        description = "Daftar bahasa regional Asia"
    ),
    USAGE_GUIDE(
        title = "Cara Penggunaan",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        description = "Panduan langkah penggunaan aplikasi"
    ),
    ABOUT(
        title = "Tentang Aplikasi",
        icon = Icons.Default.Info,
        description = "Informasi versi aplikasi dan model ML"
    )
}
