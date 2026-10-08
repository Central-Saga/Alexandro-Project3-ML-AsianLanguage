package com.alexandro.asianlanguageidentification.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.ErrorRed
import com.alexandro.asianlanguageidentification.ui.theme.PrimaryBlue
import com.alexandro.asianlanguageidentification.ui.theme.SurfaceWhite

@Composable
fun AppTopBar(
    currentScreen: Screen,
    onBackClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onClearHistoryClick: (() -> Unit)? = null,
    isHistoryEmpty: Boolean = true,
    onRefreshLanguagesClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side (Navigation icon or Title)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentScreen == Screen.ABOUT) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = DarkBlue
                    )
                }
            }
            Text(
                text = currentScreen.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkBlue,
                modifier = Modifier.padding(start = if (currentScreen == Screen.ABOUT) 0.dp else 4.dp)
            )
        }

        // Right side (Actions)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (currentScreen) {
                Screen.HISTORY -> {
                    if (!isHistoryEmpty && onClearHistoryClick != null) {
                        IconButton(onClick = onClearHistoryClick) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Hapus Semua Riwayat",
                                tint = ErrorRed
                            )
                        }
                    }
                }
                Screen.SUPPORTED_LANGUAGES -> {
                    if (onRefreshLanguagesClick != null) {
                        IconButton(onClick = onRefreshLanguagesClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Perbarui Daftar Bahasa",
                                tint = PrimaryBlue
                            )
                        }
                    }
                }
                Screen.DETECTION, Screen.USAGE_GUIDE -> {
                    IconButton(onClick = onAboutClick) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Tentang Aplikasi",
                            tint = DarkBlue
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
