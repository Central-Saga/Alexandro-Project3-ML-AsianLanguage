package com.alexandro.asianlanguageidentification.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Palet Utama Aplikasi Sesuai Panduan Desain
val DarkBlue: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF93C5FD) else Color(0xFF0B3478)

val PrimaryBlue: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF3B82F6) else Color(0xFF2563C4)

val LightBlue: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF1E3A8A) else Color(0xFFBFD7FF)

val LightBackground: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF0F172A) else Color(0xFFF5F7FC)

val SurfaceWhite: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFFFFFFF)

// Warna Status & Notifikasi Lembut
val SuccessGreen: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF087F5B)
val SuccessGreenContainer: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF064E3B) else Color(0xFFE6F4EA)
val SuccessGreenOnContainer: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF065F46)

val UnknownAmber: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309)
val UnknownAmberContainer: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF78350F) else Color(0xFFFEF3C7)
val UnknownAmberOnContainer: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFFDE68A) else Color(0xFF92400E)

val ErrorRed: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFF87171) else Color(0xFFDC2626)
val ErrorRedContainer: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
val ErrorRedOnContainer: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFFECACA) else Color(0xFF991B1B)

// Warna Tipografi & Batas
val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF1E293B)
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF64748B)
val TextTertiary: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF64748B) else Color(0xFF94A3B8)

val OutlineBorder: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)
val OutlineLight: Color
    @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFF1F5F9)