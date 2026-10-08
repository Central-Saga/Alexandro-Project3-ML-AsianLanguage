package com.alexandro.asianlanguageidentification.ui.screens.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.data.local.DetectionHistoryItem
import com.alexandro.asianlanguageidentification.ui.components.DeleteConfirmationDialog
import com.alexandro.asianlanguageidentification.ui.theme.AsianLanguageIdentificationTheme
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.ErrorRed
import com.alexandro.asianlanguageidentification.ui.theme.LightBackground
import com.alexandro.asianlanguageidentification.ui.theme.LightBlue
import com.alexandro.asianlanguageidentification.ui.theme.OutlineBorder
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyList: List<DetectionHistoryItem>,
    onDeleteItem: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmClearDialog by rememberSaveable { mutableStateOf(false) }

    if (showConfirmClearDialog) {
        DeleteConfirmationDialog(
            onConfirm = { onClearAllHistory() },
            onDismiss = { showConfirmClearDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        if (historyList.isEmpty()) {
            EmptyHistoryView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Riwayat Terbaru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkBlue
                            )
                            Text(
                                text = "Menyimpan ${historyList.size} dari maksimal 20 deteksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { showConfirmClearDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Hapus semua", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                items(historyList, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: DetectionHistoryItem,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    val formattedDate = dateFormat.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Surface(
                    color = if (item.isSuccess) SuccessGreenContainer else UnknownAmberContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (item.isSuccess) item.languageName else "Bahasa tidak dikenali",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSuccess) SuccessGreenOnContainer else UnknownAmberOnContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus item ini",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Potongan Teks
            Text(
                text = item.textSnippet,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.marginScore != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Skor margin: ${"%.4f".format(item.marginScore)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = LightBlue.copy(alpha = 0.4f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = DarkBlue,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Belum Ada Riwayat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Teks yang Anda uji melalui deteksi bahasa akan tersimpan secara otomatis di sini hingga maksimal 20 item terbaru.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    AsianLanguageIdentificationTheme {
        HistoryScreen(
            historyList = listOf(
                DetectionHistoryItem(
                    textSnippet = "Bapak presiden sedang berpidato di istana negara Jakarta hari ini.",
                    fullText = "Bapak presiden sedang berpidato di istana negara Jakarta hari ini.",
                    languageName = "Indonesia",
                    languageCode = "indonesian",
                    isSuccess = true,
                    marginScore = 0.208
                ),
                DetectionHistoryItem(
                    textSnippet = "Bonjour, je suis en train d'apprendre la langue française.",
                    fullText = "Bonjour, je suis en train d'apprendre la langue française.",
                    languageName = "Bahasa tidak dikenali",
                    languageCode = "unknown",
                    isSuccess = false,
                    marginScore = -0.111
                )
            ),
            onDeleteItem = {},
            onClearAllHistory = {}
        )
    }
}
