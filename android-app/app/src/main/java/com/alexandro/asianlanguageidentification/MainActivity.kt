package com.alexandro.asianlanguageidentification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexandro.asianlanguageidentification.network.PredictionResponse
import com.alexandro.asianlanguageidentification.ui.theme.AsianLanguageIdentificationTheme

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon


private val PrimaryBlue = Color(0xFF174EA6)
private val DarkBlue = Color(0xFF092B63)
private val LightBackground = Color(0xFFF4F7FC)
private val SuccessGreen = Color(0xFF087F5B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AsianLanguageIdentificationTheme {
                val viewModel: LanguageDetectionViewModel = viewModel()
                LanguageDetectionRoute(viewModel)
            }
        }
    }
}

@Composable
fun LanguageDetectionRoute(viewModel: LanguageDetectionViewModel) {
    LanguageDetectionScreen(
        isLoading = viewModel.isLoading,
        predictionResult = viewModel.predictionResult,
        errorMessage = viewModel.errorMessage,
        activeLanguages = viewModel.activeLanguages,
        isFeedbackSubmitting = viewModel.isFeedbackSubmitting,
        feedbackMessage = viewModel.feedbackMessage,
        onDetectLanguage = { text -> viewModel.detectLanguage(text) },
        onClearResult = { viewModel.clearResult() },
        onSubmitFeedback = { text, predicted, corrected, margin, isCorrect -> 
            viewModel.submitFeedback(text, predicted, corrected, margin, isCorrect)
        }
    )
}

@Composable
fun LanguageDetectionScreen(
    isLoading: Boolean,
    predictionResult: PredictionResponse?,
    errorMessage: String?,
    activeLanguages: List<String>,
    isFeedbackSubmitting: Boolean,
    feedbackMessage: String?,
    onDetectLanguage: (String) -> Unit,
    onClearResult: () -> Unit,
    onSubmitFeedback: (String, String, String, Float?, Boolean) -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var showCorrectionUI by rememberSaveable { mutableStateOf(false) }
    var selectedCorrectionLanguage by rememberSaveable { mutableStateOf("") }
    var otherLanguageInput by rememberSaveable { mutableStateOf("") }
    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    val minimumCharacters = 20
    val textLength = inputText.trim().length
    val isInputValid = textLength >= minimumCharacters

    val supportedLanguages = if (activeLanguages.isNotEmpty()) {
        activeLanguages
    } else {
        listOf(
            "Indonesia",
            "Melayu",
            "Vietnam",
            "Thai",
            "Khmer",
            "Lao",
            "Filipino/Tagalog"
        )
    }

    Scaffold(
        containerColor = LightBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "Masukkan teks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Masukkan sebuah kalimat untuk mengetahui bahasanya.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF607089)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                showCorrectionUI = false
                                onClearResult()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            maxLines = 9,
                            label = {
                                Text("Teks yang ingin dideteksi")
                            },
                            placeholder = {
                                Text(
                                    "Contoh: Saya sedang mempelajari machine learning."
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    inputText.isBlank() ->
                                        "Minimal $minimumCharacters karakter"

                                    !isInputValid ->
                                        "Tambahkan ${minimumCharacters - textLength} karakter lagi"

                                    else -> "Teks siap dideteksi"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isInputValid) {
                                    SuccessGreen
                                } else {
                                    Color(0xFF6B7280)
                                }
                            )

                            Text(
                                text = "$textLength karakter",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }

                        if (inputText.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    inputText = ""
                                    showCorrectionUI = false
                                    onClearResult()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Hapus teks")
                            }
                        }

                        Button(
                            onClick = {
                                onDetectLanguage(inputText)
                            },
                            enabled = isInputValid && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "Memproses...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Deteksi Bahasa",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (predictionResult != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F1FF)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Hasil Deteksi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = predictionResult.language_name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Kode: ${predictionResult.language_code}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF3D587A)
                            )
                            
                            Text(
                                text = "Skor: ${predictionResult.margin_score}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF3D587A)
                            )
                            
                            Text(
                                text = "Panjang Teks: ${predictionResult.text_length}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF3D587A)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (feedbackMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Text(
                                text = feedbackMessage,
                                modifier = Modifier.padding(16.dp),
                                color = SuccessGreen
                            )
                        }
                    } else if (isFeedbackSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (!showCorrectionUI) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { 
                                onSubmitFeedback(inputText, predictionResult.language_code, predictionResult.language_code, predictionResult.margin_score, true) 
                            }, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) {
                                Text("Hasil sudah benar")
                            }
                            Button(onClick = { showCorrectionUI = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))) {
                                Text("Koreksi hasil")
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Pilih bahasa yang benar:", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(onClick = { expandedDropdown = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) {
                                        Text(if (selectedCorrectionLanguage.isEmpty()) "Pilih Bahasa" else selectedCorrectionLanguage)
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                                        supportedLanguages.forEach { lang ->
                                            DropdownMenuItem(text = { Text(lang) }, onClick = { 
                                                selectedCorrectionLanguage = lang
                                                expandedDropdown = false 
                                            })
                                        }
                                        DropdownMenuItem(text = { Text("Bahasa lain") }, onClick = { 
                                            selectedCorrectionLanguage = "Bahasa lain"
                                            expandedDropdown = false 
                                        })
                                    }
                                }
                                
                                if (selectedCorrectionLanguage == "Bahasa lain") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = otherLanguageInput,
                                        onValueChange = { otherLanguageInput = it },
                                        label = { Text("Ketik nama bahasa") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showCorrectionUI = false }) {
                                        Text("Batal")
                                    }
                                    Button(onClick = {
                                        val corrected = if (selectedCorrectionLanguage == "Bahasa lain") otherLanguageInput else selectedCorrectionLanguage
                                        if (corrected.isNotBlank()) {
                                            onSubmitFeedback(inputText, predictionResult.language_code, corrected, predictionResult.margin_score, false)
                                            showCorrectionUI = false
                                        }
                                    }) {
                                        Text("Kirim Koreksi")
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Terjadi Kesalahan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }

                SupportedLanguagesCard(
                    languages = supportedLanguages
                )

                Text(
                    text = "Model v2 • Character TF-IDF + LinearSVC",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B8798)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBlue,
                        PrimaryBlue
                    )
                )
            )
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 42.dp,
                bottom = 30.dp
            )
    ) {
        Column {
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = "7 BAHASA DIDUKUNG",
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 7.dp
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Deteksi Bahasa Asia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kenali bahasa dari teks dengan cepat dan mudah.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun SupportedLanguagesCard(
    languages: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Bahasa yang didukung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(14.dp))

            languages.chunked(2).forEach { rowLanguages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowLanguages.forEach { language ->
                        LanguageBadge(
                            name = language,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (rowLanguages.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LanguageBadge(
    name: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF0F5FF),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 11.dp
            ),
            color = PrimaryBlue,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun LanguageDetectionPreview() {
    AsianLanguageIdentificationTheme {
        LanguageDetectionScreen(
            isLoading = false,
            predictionResult = null,
            errorMessage = null,
            activeLanguages = emptyList(),
            isFeedbackSubmitting = false,
            feedbackMessage = null,
            onDetectLanguage = {},
            onClearResult = {},
            onSubmitFeedback = { _, _, _, _, _ -> }
        )
    }
}
