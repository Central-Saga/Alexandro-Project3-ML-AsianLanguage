package com.alexandro.asianlanguageidentification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexandro.asianlanguageidentification.navigation.AppBottomBar
import com.alexandro.asianlanguageidentification.navigation.AppTopBar
import com.alexandro.asianlanguageidentification.navigation.Screen
import com.alexandro.asianlanguageidentification.ui.components.DeleteConfirmationDialog
import com.alexandro.asianlanguageidentification.ui.screens.about.AboutScreen
import com.alexandro.asianlanguageidentification.ui.screens.detection.DetectionScreen
import com.alexandro.asianlanguageidentification.ui.screens.guide.UsageGuideScreen
import com.alexandro.asianlanguageidentification.ui.screens.history.HistoryScreen
import com.alexandro.asianlanguageidentification.ui.screens.languages.SupportedLanguagesScreen
import com.alexandro.asianlanguageidentification.ui.theme.AsianLanguageIdentificationTheme
import com.alexandro.asianlanguageidentification.ui.theme.LightBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AsianLanguageIdentificationTheme {
                val viewModel: LanguageDetectionViewModel = viewModel()
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: LanguageDetectionViewModel) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    var previousScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
    var resetSignal by rememberSaveable { mutableStateOf(0L) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    val historyList by viewModel.historyList.collectAsState()

    if (showDeleteAllDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.clearAllHistory()
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    Scaffold(
        topBar = {
            if (currentScreen != Screen.SPLASH) {
                AppTopBar(
                    currentScreen = currentScreen,
                    onBackClick = {
                        currentScreen = previousScreen
                    },
                    onAboutClick = {
                        previousScreen = currentScreen
                        currentScreen = Screen.ABOUT
                    },
                    onClearHistoryClick = {
                        showDeleteAllDialog = true
                    },
                    isHistoryEmpty = historyList.isEmpty(),
                    onRefreshLanguagesClick = {
                        viewModel.fetchLanguages()
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen != Screen.SPLASH) {
                AppBottomBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { screen ->
                        previousScreen = currentScreen
                        currentScreen = screen
                    },
                    onFabClick = {
                        previousScreen = currentScreen
                        currentScreen = Screen.DETECTION
                        viewModel.clearResult()
                        resetSignal = System.currentTimeMillis()
                    }
                )
            }
        },
        containerColor = LightBackground
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 220),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "ScreenCrossfade"
        ) { screen ->
            when (screen) {
                Screen.SPLASH -> {
                    com.alexandro.asianlanguageidentification.ui.screens.splash.SplashScreen(
                        onSplashFinished = {
                            currentScreen = Screen.DETECTION
                            previousScreen = Screen.DETECTION
                        }
                    )
                }
                Screen.DETECTION -> {
                    DetectionScreen(
                        isLoading = viewModel.isLoading,
                        predictionResult = viewModel.predictionResult,
                        errorMessage = viewModel.errorMessage,
                        activeLanguages = viewModel.activeLanguages,
                        isFeedbackSubmitting = viewModel.isFeedbackSubmitting,
                        feedbackMessage = viewModel.feedbackMessage,
                        onDetectLanguage = { text -> viewModel.detectLanguage(text) },
                        onClearResult = { viewModel.clearResult() },
                        onRetryDetection = { viewModel.retryLastDetection() },
                        onSubmitFeedback = { text, pred, corr, margin, isCorrect ->
                            viewModel.submitFeedback(text, pred, corr, margin, isCorrect)
                        },
                        onNavigateToSupportedLanguages = {
                            previousScreen = currentScreen
                            currentScreen = Screen.SUPPORTED_LANGUAGES
                        },
                        resetSignal = resetSignal
                    )
                }
                Screen.HISTORY -> {
                    HistoryScreen(
                        historyList = historyList,
                        onDeleteItem = { id -> viewModel.deleteHistoryItem(id) },
                        onClearAllHistory = { showDeleteAllDialog = true }
                    )
                }
                Screen.SUPPORTED_LANGUAGES -> {
                    SupportedLanguagesScreen(
                        activeLanguages = viewModel.activeLanguages,
                        onRefresh = { viewModel.fetchLanguages() }
                    )
                }
                Screen.USAGE_GUIDE -> {
                    UsageGuideScreen(
                        onNavigateToAbout = {
                            previousScreen = currentScreen
                            currentScreen = Screen.ABOUT
                        }
                    )
                }
                Screen.ABOUT -> {
                    AboutScreen()
                }
            }
        }
    }
}
