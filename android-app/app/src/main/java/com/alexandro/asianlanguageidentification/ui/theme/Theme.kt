package com.alexandro.asianlanguageidentification.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AsianLanguageIdentificationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Tetapkan false agar warna palet kustom aplikasi tetap konsisten
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val lightColorScheme = lightColorScheme(
        primary = PrimaryBlue,
        onPrimary = SurfaceWhite,
        primaryContainer = LightBlue,
        onPrimaryContainer = DarkBlue,
        secondary = DarkBlue,
        onSecondary = SurfaceWhite,
        secondaryContainer = LightBlue.copy(alpha = 0.5f),
        onSecondaryContainer = DarkBlue,
        background = LightBackground,
        onBackground = TextPrimary,
        surface = SurfaceWhite,
        onSurface = TextPrimary,
        surfaceVariant = LightBackground,
        onSurfaceVariant = TextSecondary,
        outline = OutlineBorder,
        outlineVariant = OutlineLight,
        error = ErrorRed,
        onError = SurfaceWhite,
        errorContainer = ErrorRedContainer,
        onErrorContainer = ErrorRedOnContainer
    )

    val darkColorScheme = darkColorScheme(
        primary = LightBlue,
        onPrimary = DarkBlue,
        primaryContainer = PrimaryBlue,
        onPrimaryContainer = SurfaceWhite,
        secondary = LightBlue,
        onSecondary = DarkBlue,
        background = DarkBlue,
        onBackground = SurfaceWhite,
        surface = DarkBlue,
        onSurface = SurfaceWhite,
        error = ErrorRedContainer,
        onError = ErrorRedOnContainer
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val statusBarColor = DarkBlue.toArgb()
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = statusBarColor
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}