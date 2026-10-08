package com.alexandro.asianlanguageidentification.ui.screens.languages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.ui.theme.AsianLanguageIdentificationTheme
import com.alexandro.asianlanguageidentification.R
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.LightBackground
import com.alexandro.asianlanguageidentification.ui.theme.LightBlue
import com.alexandro.asianlanguageidentification.ui.theme.PrimaryBlue
import com.alexandro.asianlanguageidentification.ui.theme.SurfaceWhite
import com.alexandro.asianlanguageidentification.ui.theme.TextPrimary
import com.alexandro.asianlanguageidentification.ui.theme.TextSecondary
import com.alexandro.asianlanguageidentification.ui.theme.UnknownAmber
import com.alexandro.asianlanguageidentification.ui.theme.UnknownAmberContainer
import com.alexandro.asianlanguageidentification.ui.theme.UnknownAmberOnContainer

data class LanguageInfo(
    val code: String,
    val name: String,
    val nativeScript: String,
    val scriptType: String,
    val sampleWord: String,
    val flagResId: Int? = null
)

val DEFAULT_LANGUAGES_INFO = listOf(
    LanguageInfo("indonesian", "Indonesia", "Bahasa Indonesia", "Latin", "Selamat datang", R.drawable.flag_indonesian),
    LanguageInfo("malay", "Melayu", "Bahasa Melayu", "Latin", "Selamat tengah hari", R.drawable.flag_malay),
    LanguageInfo("vietnamese", "Vietnam", "Tiếng Việt", "Latin Extended", "Xin chào", R.drawable.flag_vietnamese),
    LanguageInfo("thai", "Thai", "ภาษาไทย", "Aksara Thai", "สวัสดี (Sawasdee)", R.drawable.flag_thai),
    LanguageInfo("khmer", "Khmer", "ភាសាខ្មែរ", "Aksara Khmer", "សួស្តី (Sour Sdey)", R.drawable.flag_khmer),
    LanguageInfo("lao", "Lao", "ພາສາລາວ", "Aksara Lao", "ສະບາຍດີ (Sabaidee)", R.drawable.flag_lao),
    LanguageInfo("fil_tl", "Filipino/Tagalog", "Wikang Filipino", "Latin", "Mabuhay", R.drawable.flag_fil_tl),
    LanguageInfo("japanese", "Jepang", "日本語", "Aksara Jepang", "こんにちは (Konnichiwa)", R.drawable.flag_japanese),
    LanguageInfo("korean", "Korea", "한국어", "Hangul", "안녕하세요 (Annyeonghaseyo)", R.drawable.flag_korean),
    LanguageInfo("chinese", "Mandarin", "中文", "Aksara Han", "你好 (Nǐ hǎo)", R.drawable.flag_chinese),
    LanguageInfo("hindi", "Hindi", "हिन्दी", "Dewanagari", "नमस्ते (Namaste)", R.drawable.flag_hindi),
    LanguageInfo("arabic", "Arab", "العربية", "Arab", "مرحباً (Marhaba)", R.drawable.flag_arabic),
    LanguageInfo("farsi", "Persia", "فارسی", "Arab", "سلام (Salaam)", R.drawable.flag_farsi),
    LanguageInfo("hebrew", "Ibrani", "עברית", "Ibrani", "שלום (Shalom)", R.drawable.flag_hebrew),
    LanguageInfo("turkish", "Turki", "Türkçe", "Latin", "Merhaba", R.drawable.flag_turkish),
    LanguageInfo("urdu", "Urdu", "اردو", "Arab", "ہیلو (Hello)", R.drawable.flag_urdu)
)

@Composable
fun SupportedLanguagesScreen(
    activeLanguages: List<String>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languagesToShow = if (activeLanguages.isNotEmpty()) {
        // Gabungkan dengan info detail jika cocok
        activeLanguages.map { langKey ->
            DEFAULT_LANGUAGES_INFO.find {
                it.name.equals(langKey, ignoreCase = true) || it.code.equals(langKey, ignoreCase = true)
            } ?: LanguageInfo(langKey.lowercase(), langKey, langKey, "Regional", "-")
        }
    } else {
        DEFAULT_LANGUAGES_INFO
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Penjelasan Penting (Span Full)
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, UnknownAmber.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    UnknownAmberContainer.copy(alpha = 0.8f),
                                    UnknownAmberContainer.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = UnknownAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = UnknownAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Batasan Bahasa Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = UnknownAmberOnContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Model saat ini dilatih khusus untuk bahasa Asia di bawah ini. Bahasa lain (seperti Inggris, Prancis, atau Jerman) belum didukung dan akan diklasifikasikan sebagai 'Bahasa tidak dikenali'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = UnknownAmberOnContainer.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Header Daftar Bahasa (Span Full)
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Bahasa Didukung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                OutlinedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Segarkan", fontSize = 12.sp, color = PrimaryBlue)
                }
            }
        }

        // Grid Kartu Bahasa
        items(languagesToShow) { info ->
            LanguageDetailCard(info = info)
        }
    }
}

@Composable
private fun LanguageDetailCard(info: LanguageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (info.flagResId != null) {
                Image(
                    painter = painterResource(id = info.flagResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.15f
                )
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    color = LightBlue.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = info.scriptType,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = info.nativeScript,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = LightBackground.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${info.sampleWord}\"",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportedLanguagesScreenPreview() {
    AsianLanguageIdentificationTheme {
        SupportedLanguagesScreen(
            activeLanguages = emptyList(),
            onRefresh = {}
        )
    }
}
