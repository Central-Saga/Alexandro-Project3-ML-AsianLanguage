package com.alexandro.asianlanguageidentification.ui.screens.detection

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.network.PredictionResponse
import com.alexandro.asianlanguageidentification.ui.components.LanguageBadge
import com.alexandro.asianlanguageidentification.ui.theme.AsianLanguageIdentificationTheme
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.ErrorRed
import com.alexandro.asianlanguageidentification.ui.theme.ErrorRedContainer
import com.alexandro.asianlanguageidentification.ui.theme.ErrorRedOnContainer
import com.alexandro.asianlanguageidentification.ui.theme.LightBackground
import com.alexandro.asianlanguageidentification.ui.theme.LightBlue
import com.alexandro.asianlanguageidentification.ui.theme.OutlineBorder
import com.alexandro.asianlanguageidentification.ui.theme.OutlineLight
import com.alexandro.asianlanguageidentification.ui.theme.PrimaryBlue
import com.alexandro.asianlanguageidentification.ui.theme.SuccessGreen
import com.alexandro.asianlanguageidentification.ui.theme.SuccessGreenContainer
import com.alexandro.asianlanguageidentification.ui.theme.SuccessGreenOnContainer
import com.alexandro.asianlanguageidentification.ui.theme.SurfaceWhite
import com.alexandro.asianlanguageidentification.ui.theme.TextPrimary
import com.alexandro.asianlanguageidentification.ui.theme.TextSecondary
import com.alexandro.asianlanguageidentification.ui.theme.TextTertiary
import com.alexandro.asianlanguageidentification.ui.theme.UnknownAmber
import com.alexandro.asianlanguageidentification.ui.theme.UnknownAmberContainer
import com.alexandro.asianlanguageidentification.ui.theme.UnknownAmberOnContainer
import com.alexandro.asianlanguageidentification.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset

private data class LanguageItemInfo(
    val name: String,
    val nativeScript: String,
    val isoCode: String,
    val sampleText: String,
    val flagResId: Int? = null
)

private val LANGUAGE_INFO_MAP = mapOf(
    "indonesian" to LanguageItemInfo("Indonesia", "Bahasa Indonesia", "ID", "Hari ini cuaca sangat cerah dan saya sedang belajar kecerdasan buatan.", R.drawable.flag_indonesian),
    "malay" to LanguageItemInfo("Melayu", "Bahasa Melayu", "MY", "Saya hendak pergi ke pasar untuk membeli pelbagai buah-buahan segar hari ini.", R.drawable.flag_malay),
    "thai" to LanguageItemInfo("Thai", "ภาษาไทย", "TH", "สวัสดีครับ ยินดีที่ได้รู้จักทุกคนและยินดีต้อนรับสู่ระบบตรวจจับภาษา", R.drawable.flag_thai),
    "vietnamese" to LanguageItemInfo("Vietnam", "Tiếng Việt", "VN", "Tôi rất thích học lập trình và nghiên cứu các mô hình trí tuệ nhân tạo hiện đại.", R.drawable.flag_vietnamese),
    "fil_tl" to LanguageItemInfo("Filipino", "Tagalog", "PH", "Magandang umaga sa inyong lahat mga kaibigan, kumusta po kayo ngayon?", R.drawable.flag_fil_tl),
    "khmer" to LanguageItemInfo("Khmer", "ភាសាខ្មែរ", "KM", "សួស្តីបងប្អូនទាំងអស់គ្នា សូមស្វាគមន៍មកកាន់ប្រព័ន្ធសម្គាល់ភាសា", R.drawable.flag_khmer),
    "lao" to LanguageItemInfo("Lao", "ພາສາລາວ", "LA", "ສະບາຍດີ ຂອບໃຈຫຼາຍໆ ສຳລັບຄວາມຊ່ວຍເຫຼືອ ແລະ ການຮ່ວມມືທຸກຢ່າງ", R.drawable.flag_lao),
    "japanese" to LanguageItemInfo("Jepang", "日本語", "JP", "明日は友達と一緒に映画を見に行きます。", R.drawable.flag_japanese),
    "korean" to LanguageItemInfo("Korea", "한국어", "KR", "언어 감지 앱에 오신 것을 환영합니다.", R.drawable.flag_korean),
    "chinese" to LanguageItemInfo("Mandarin", "中文", "CN", "欢迎来到语言检测应用程序。", R.drawable.flag_chinese),
    "hindi" to LanguageItemInfo("Hindi", "हिन्दी", "IN", "भाषा पहचान ऐप में आपका स्वागत है।", R.drawable.flag_hindi),
    "arabic" to LanguageItemInfo("Arab", "العربية", "SA", "مرحباً بك في تطبيق التعرف على اللغات.", R.drawable.flag_arabic),
    "farsi" to LanguageItemInfo("Persia", "فارسی", "IR", "به برنامه تشخیص زبان خوش آمدید.", R.drawable.flag_farsi),
    "hebrew" to LanguageItemInfo("Ibrani", "עברית", "IL", "ברוכים הבאים לאפליקציית זיהוי השפות.", R.drawable.flag_hebrew),
    "turkish" to LanguageItemInfo("Turki", "Türkçe", "TR", "Dil algılama uygulamasına hoş geldiniz.", R.drawable.flag_turkish),
    "urdu" to LanguageItemInfo("Urdu", "اردو", "PK", "زبان کی شناخت کی ایپ میں خوش آمدید۔", R.drawable.flag_urdu)
)

private val LANGUAGE_DISPLAY_NAMES = mapOf(
    "fil_tl" to "Filipino/Tagalog",
    "indonesian" to "Indonesia",
    "khmer" to "Khmer",
    "lao" to "Lao",
    "malay" to "Melayu",
    "thai" to "Thai",
    "vietnamese" to "Vietnam",
    "japanese" to "Jepang",
    "korean" to "Korea",
    "chinese" to "Mandarin",
    "hindi" to "Hindi",
    "arabic" to "Arab",
    "farsi" to "Persia",
    "hebrew" to "Ibrani",
    "turkish" to "Turki",
    "urdu" to "Urdu"
)

private fun languageDisplayName(code: String): String =
    LANGUAGE_DISPLAY_NAMES[code.lowercase()] ?: code

private fun languageInfo(code: String): LanguageItemInfo {
    val clean = code.lowercase().trim()
    return LANGUAGE_INFO_MAP[clean] ?: LanguageItemInfo(
        name = languageDisplayName(clean),
        nativeScript = "",
        isoCode = clean.take(2).uppercase(),
        sampleText = ""
    )
}

@Composable
private fun getAvatarColor(iso: String): Color {
    return when (iso.uppercase()) {
        "ID" -> Color(0xFFDC2626) // Merah
        "MY" -> Color(0xFF1E40AF) // Biru Navy
        "TH" -> Color(0xFF4338CA) // Indigo
        "VN" -> Color(0xFFD97706) // Oranye/Amber
        "PH" -> Color(0xFF0284C7) // Sky Blue
        "KM" -> Color(0xFF475569) // Slate
        "LA" -> Color(0xFF059669) // Emerald
        else -> PrimaryBlue
    }
}

@Composable
fun DetectionScreen(
    isLoading: Boolean,
    predictionResult: PredictionResponse?,
    errorMessage: String?,
    activeLanguages: List<String>,
    isFeedbackSubmitting: Boolean,
    feedbackMessage: String?,
    onDetectLanguage: (String) -> Unit,
    onClearResult: () -> Unit,
    onRetryDetection: () -> Unit,
    onSubmitFeedback: (String, String, String, Float?, Boolean) -> Unit,
    onNavigateToSupportedLanguages: () -> Unit,
    resetSignal: Long = 0L,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var showCorrectionUI by rememberSaveable { mutableStateOf(false) }
    var selectedCorrectionLanguage by rememberSaveable { mutableStateOf("") }
    var otherLanguageInput by rememberSaveable { mutableStateOf("") }
    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(resetSignal) {
        if (resetSignal > 0L) {
            inputText = ""
            showCorrectionUI = false
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val minimumCharacters = 20
    val textLength = inputText.trim().length
    val isInputValid = textLength >= minimumCharacters

    val supportedLanguages = if (activeLanguages.isNotEmpty()) {
        activeLanguages
    } else {
        listOf(
            "indonesian",
            "malay",
            "thai",
            "vietnamese",
            "fil_tl",
            "khmer",
            "lao"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Hero Banner Modern dengan Rounded Corners yang menyatu dengan tata letak
        HeroCard()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Input Teks Utama
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, OutlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    // Header Bar di dalam Card Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LightBlue.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Masukkan Teks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )
                        }

                        // Tombol Aksi Cepat: Tempel & Hapus
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (inputText.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        inputText = ""
                                        showCorrectionUI = false
                                        onClearResult()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Hapus",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        inputText = clip
                                        showCorrectionUI = false
                                        onClearResult()
                                    } else {
                                        Toast.makeText(context, "Papan klip kosong", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Tempel",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Tempel",
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            showCorrectionUI = false
                            onClearResult()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 7,
                        placeholder = {
                            Text(
                                "Ketik teks atau pilih contoh kalimat di bawah untuk mendeteksi bahasa...",
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = OutlineBorder,
                            focusedContainerColor = LightBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = LightBackground.copy(alpha = 0.35f)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Bar Karakter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when {
                                isInputValid -> SuccessGreenContainer
                                textLength > 0 -> UnknownAmberContainer
                                else -> LightBackground
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when {
                                    inputText.isBlank() -> "Minimal $minimumCharacters karakter"
                                    !isInputValid -> "Kurang ${minimumCharacters - textLength} karakter lagi"
                                    else -> "✓ Teks siap dideteksi"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isInputValid -> SuccessGreenOnContainer
                                    textLength > 0 -> UnknownAmberOnContainer
                                    else -> TextSecondary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "$textLength karakter",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chips Contoh Cepat untuk Kemudahan Pengujian
                    Text(
                        text = "Contoh kalimat cepat (ketuk untuk coba):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LANGUAGE_INFO_MAP.values.forEach { info ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        inputText = info.sampleText
                                        showCorrectionUI = false
                                        onClearResult()
                                    },
                                color = LightBlue.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, LightBlue.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceWhite),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = info.flagResId!!),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Text(
                                        text = info.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol Deteksi Utama
                    Button(
                        onClick = {
                            if (isInputValid && !isLoading) {
                                onDetectLanguage(inputText)
                            }
                        },
                        enabled = isInputValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            disabledContainerColor = OutlineBorder
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SurfaceWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Menganalisis teks…",
                                color = SurfaceWhite,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Deteksi Bahasa",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Animated Visibility untuk Hasil Prediksi
            AnimatedVisibility(
                visible = predictionResult != null,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut()
            ) {
                if (predictionResult != null) {
                    val isUnknown = predictionResult.status == "unknown" || predictionResult.language_code == "unknown"
                    val info = languageInfo(predictionResult.language_code)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnknown) UnknownAmberContainer else Color(0xFFF0F6FF)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isUnknown) UnknownAmber.copy(alpha = 0.4f) else LightBlue
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Header bar hasil
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isUnknown) UnknownAmber.copy(alpha = 0.2f) else SuccessGreenContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isUnknown) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isUnknown) UnknownAmberOnContainer else SuccessGreenOnContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isUnknown) "BELUM DIKENALI" else "TERDETEKSI",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUnknown) UnknownAmberOnContainer else SuccessGreenOnContainer
                                            )
                                        }
                                    }

                                    Surface(
                                        color = SurfaceWhite.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, OutlineBorder)
                                    ) {
                                        Text(
                                            text = "Margin: ${"%.4f".format(predictionResult.margin_score)}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkBlue
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Konten Bahasa
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Avatar ISO Code
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(getAvatarColor(info.isoCode)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = info.isoCode,
                                            color = SurfaceWhite,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = predictionResult.language_name,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnknown) UnknownAmberOnContainer else DarkBlue
                                        )
                                        if (info.nativeScript.isNotBlank()) {
                                            Text(
                                                text = "${info.nativeScript} • Kode: ${predictionResult.language_code}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        } else {
                                            Text(
                                                text = "Kode: ${predictionResult.language_code}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Tombol Aksi: Sesuai, Koreksi, Salin
                                if (feedbackMessage != null) {
                                    Surface(
                                        color = SuccessGreenContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = SuccessGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = feedbackMessage,
                                                color = SuccessGreenOnContainer,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                } else if (isFeedbackSubmitting) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = PrimaryBlue,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else if (!showCorrectionUI) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val margin = predictionResult.margin_score.toFloat()
                                                onSubmitFeedback(
                                                    inputText,
                                                    predictionResult.language_code,
                                                    predictionResult.language_code,
                                                    margin,
                                                    true
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Akurat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showCorrectionUI = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = SurfaceWhite,
                                                contentColor = DarkBlue
                                            ),
                                            border = BorderStroke(1.dp, OutlineLight)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Koreksi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val copyText = "${predictionResult.language_name} (${predictionResult.language_code})\n$inputText"
                                                    clipboardManager.setText(AnnotatedString(copyText))
                                                    Toast.makeText(context, "Hasil disalin ke papan klip", Toast.LENGTH_SHORT).show()
                                                },
                                            color = SurfaceWhite,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, OutlineLight)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Salin Hasil",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Form Koreksi Bahasa
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                        border = BorderStroke(1.dp, OutlineBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Pilih bahasa yang benar:",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkBlue
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = { expandedDropdown = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (selectedCorrectionLanguage.isEmpty()) {
                                                                "Pilih Bahasa"
                                                            } else {
                                                                languageDisplayName(selectedCorrectionLanguage)
                                                            },
                                                            color = if (selectedCorrectionLanguage.isEmpty()) TextSecondary else DarkBlue
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = expandedDropdown,
                                                    onDismissRequest = { expandedDropdown = false }
                                                ) {
                                                    supportedLanguages.forEach { lang ->
                                                        DropdownMenuItem(
                                                            text = { Text(languageDisplayName(lang)) },
                                                            onClick = {
                                                                selectedCorrectionLanguage = lang
                                                                expandedDropdown = false
                                                            }
                                                        )
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text("Bahasa lain (ketik manual)") },
                                                        onClick = {
                                                            selectedCorrectionLanguage = "Bahasa lain"
                                                            expandedDropdown = false
                                                        }
                                                    )
                                                }
                                            }

                                            if (selectedCorrectionLanguage == "Bahasa lain") {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                OutlinedTextField(
                                                    value = otherLanguageInput,
                                                    onValueChange = { otherLanguageInput = it },
                                                    label = { Text("Ketik nama bahasa") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { showCorrectionUI = false }) {
                                                    Text("Batal", color = TextSecondary)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = {
                                                        val corrected = if (selectedCorrectionLanguage == "Bahasa lain") {
                                                            otherLanguageInput
                                                        } else {
                                                            selectedCorrectionLanguage
                                                        }
                                                        if (corrected.isNotBlank()) {
                                                            val margin = predictionResult.margin_score.toFloat()
                                                            onSubmitFeedback(
                                                                inputText,
                                                                predictionResult.language_code,
                                                                corrected,
                                                                margin,
                                                                false
                                                            )
                                                            showCorrectionUI = false
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                                ) {
                                                    Text("Kirim Koreksi", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pesan Error Jaringan / Server
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRedContainer),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Tidak Dapat Terhubung",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRedOnContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRedOnContainer
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Hint perintah ADB reverse
                            Surface(
                                color = SurfaceWhite.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Jalankan di terminal PC: adb reverse tcp:8000 tcp:8000",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = ErrorRedOnContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onRetryDetection,
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceWhite,
                                    contentColor = ErrorRedOnContainer
                                ),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Coba lagi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Tombol Navigasi ke Layar Bahasa yang Didukung
            Button(
                onClick = onNavigateToSupportedLanguages,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Lihat Daftar Bahasa yang Didukung",
                    fontWeight = FontWeight.Bold,
                    color = SurfaceWhite
                )
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(DarkBlue, PrimaryBlue, androidx.compose.ui.graphics.Color(0xFF3B82F6)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = SurfaceWhite.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SurfaceWhite,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AI MODEL v3",
                                color = SurfaceWhite,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Text(
                        text = "Bahasa Asia",
                        color = SurfaceWhite.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Deteksi Bahasa Teks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SurfaceWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Kenali bahasa teks regional Asia secara cepat, akurat, dan otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SurfaceWhite.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun SupportedLanguagesSection(
    languages: List<String>,
    onLanguageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, OutlineBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bahasa yang Didukung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
                Surface(
                    color = LightBlue.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "16 BAHASA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Model dilatih mengenali kalimat dalam bahasa-bahasa berikut. Ketuk kartu untuk menguji contoh:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            val sortedLanguages = languages.sortedBy { languageInfo(it).name }
            sortedLanguages.chunked(2).forEach { rowLangs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowLangs.forEach { lang ->
                        val info = languageInfo(lang)
                        LanguageBadge(
                            name = info.name,
                            isoCode = info.isoCode,
                            nativeScript = info.nativeScript,
                            onClick = { onLanguageClick(info.sampleText) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowLangs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetectionScreenPreview() {
    AsianLanguageIdentificationTheme {
        DetectionScreen(
            isLoading = false,
            predictionResult = PredictionResponse(
                status = "success",
                language_code = "indonesian",
                language_name = "Indonesia",
                margin_score = 0.42,
                text_length = 45
            ),
            errorMessage = null,
            activeLanguages = listOf("Indonesia", "Melayu", "Vietnam", "Thai", "Khmer", "Lao", "Filipino/Tagalog"),
            isFeedbackSubmitting = false,
            feedbackMessage = null,
            onDetectLanguage = {},
            onClearResult = {},
            onRetryDetection = {},
            onSubmitFeedback = { _, _, _, _, _ -> },
            onNavigateToSupportedLanguages = {}
        )
    }
}
