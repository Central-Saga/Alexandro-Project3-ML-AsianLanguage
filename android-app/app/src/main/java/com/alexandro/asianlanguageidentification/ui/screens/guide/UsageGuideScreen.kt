package com.alexandro.asianlanguageidentification.ui.screens.guide

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.ui.theme.AsianLanguageIdentificationTheme
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

data class GuideStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)

val GUIDE_STEPS = listOf(
    GuideStep(
        stepNumber = 1,
        title = "Masukkan Teks Minimal 20 Karakter",
        description = "Ketik atau salin kalimat yang ingin Anda deteksi ke dalam kolom input. Tombol deteksi akan aktif secara otomatis setelah panjang teks mencapai minimal 20 karakter.",
        icon = Icons.Default.TextFields
    ),
    GuideStep(
        stepNumber = 2,
        title = "Tekan Tombol Deteksi Bahasa",
        description = "Ketuk tombol 'Deteksi Bahasa'. Aplikasi akan mengirimkan teks ke backend FastAPI untuk diproses menggunakan model Machine Learning.",
        icon = Icons.Default.Search
    ),
    GuideStep(
        stepNumber = 3,
        title = "Lihat Hasil Prediksi",
        description = "Hasil prediksi akan ditampilkan secara visual. Jika bahasa cocok, kartu biru muda akan muncul. Jika bahasa tidak didukung atau tingkat keyakinan rendah, kartu amber 'Bahasa tidak dikenali' akan ditampilkan.",
        icon = Icons.Default.CheckCircle
    ),
    GuideStep(
        stepNumber = 4,
        title = "Konfirmasi atau Koreksi Hasil",
        description = "Bantu tingkatkan kualitas model dengan menekan 'Hasil sudah benar' jika sesuai, atau 'Koreksi hasil' untuk memilih bahasa sebenarnya sebagai feedback bagi sistem retraining.",
        icon = Icons.Default.Feedback
    )
)

@Composable
fun UsageGuideScreen(
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tips Card Utama
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = UnknownAmberContainer),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = UnknownAmber,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tips Akurasi Terbaik",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = UnknownAmberOnContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gunakan kalimat yang lengkap dan wajar. Model Character TF-IDF bekerja paling optimal ketika terdapat pola n-gram karakter yang cukup untuk dianalisis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UnknownAmberOnContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Text(
            text = "4 Langkah Mudah Penggunaan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DarkBlue,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Daftar Langkah
        GUIDE_STEPS.forEach { step ->
            StepCard(step = step)
        }

        // Card Pintasan ke Tentang Aplikasi
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(1.dp),
            onClick = onNavigateToAbout
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LightBlue.copy(alpha = 0.35f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DarkBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tentang Aplikasi & Spesifikasi ML",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lihat arsitektur sistem, versi, dan teknologi model",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StepCard(step: GuideStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Badge Nomor Langkah
            Surface(
                color = PrimaryBlue,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = step.stepNumber.toString(),
                        color = SurfaceWhite,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageGuideScreenPreview() {
    AsianLanguageIdentificationTheme {
        UsageGuideScreen()
    }
}
