package com.alexandro.asianlanguageidentification.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.LightBlue
import com.alexandro.asianlanguageidentification.ui.theme.PrimaryBlue
import com.alexandro.asianlanguageidentification.ui.theme.SurfaceWhite
import com.alexandro.asianlanguageidentification.ui.theme.TextSecondary

@Composable
fun LanguageBadge(
    name: String,
    modifier: Modifier = Modifier,
    isoCode: String? = null,
    flagEmoji: String? = null,
    nativeScript: String? = null,
    onClick: (() -> Unit)? = null
) {
    val displayCode = isoCode ?: deriveIsoCode(name)
    val badgeModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Surface(
        modifier = badgeModifier,
        color = SurfaceWhite,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LightBlue.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar badge 2-huruf yang rapi dan konsisten (menggantikan flag emoji yang sering rusak di Android)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getBadgeColor(displayCode)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayCode,
                    color = SurfaceWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = name,
                    color = DarkBlue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!nativeScript.isNullOrBlank()) {
                    Text(
                        text = nativeScript,
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun deriveIsoCode(name: String): String {
    val clean = name.trim().lowercase()
    return when {
        clean.contains("indo") -> "ID"
        clean.contains("mela") || clean.contains("malay") -> "MY"
        clean.contains("thai") -> "TH"
        clean.contains("viet") -> "VN"
        clean.contains("fil") || clean.contains("taga") -> "PH"
        clean.contains("khmer") || clean.contains("kamboja") -> "KM"
        clean.contains("lao") -> "LA"
        else -> clean.take(2).uppercase()
    }
}

@Composable
private fun getBadgeColor(iso: String): Color {
    return when (iso.uppercase()) {
        "ID" -> Color(0xFFDC2626) // Merah Indonesia
        "MY" -> Color(0xFF1E40AF) // Biru Melayu
        "TH" -> Color(0xFF4338CA) // Biru/Indigo Thai
        "VN" -> Color(0xFFD97706) // Oranye/Emas Vietnam
        "PH" -> Color(0xFF0284C7) // Biru Filipina
        "KM" -> Color(0xFF475569) // Slate Khmer
        "LA" -> Color(0xFF059669) // Emerald Lao
        else -> PrimaryBlue
    }
}
