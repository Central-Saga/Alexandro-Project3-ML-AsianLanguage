package com.alexandro.asianlanguageidentification.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.PrimaryBlue
import com.alexandro.asianlanguageidentification.ui.theme.SurfaceWhite
import com.alexandro.asianlanguageidentification.ui.theme.TextPrimary
import com.alexandro.asianlanguageidentification.ui.theme.TextTertiary

/**
 * Custom Shape dengan lengkungan cradle halus di bagian atas tengah
 * untuk menampung Floating Action Button circular.
 */
class BottomBarCradleShape(
    private val cradleRadius: Dp = 36.dp,
    private val cornerRadius: Dp = 22.dp,
    private val cradleDepth: Dp = 24.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cradleR = with(density) { cradleRadius.toPx() }
        val cornerR = with(density) { cornerRadius.toPx() }
        val depth = with(density) { cradleDepth.toPx() }
        val cx = size.width / 2f

        val path = Path().apply {
            // Mulai sudut kiri atas
            moveTo(0f, cornerR)
            quadraticTo(0f, 0f, cornerR, 0f)

            // Garis lurus ke awal lengkungan cradle
            val margin = with(density) { 14.dp.toPx() }
            val leftStart = cx - cradleR - margin
            val rightEnd = cx + cradleR + margin

            lineTo(leftStart, 0f)

            // Lengkungan cubic bezier halus ke dalam cradle
            val handleOffset = with(density) { 12.dp.toPx() }
            cubicTo(
                leftStart + handleOffset, 0f,
                cx - cradleR * 0.7f, depth,
                cx, depth
            )
            // Lengkungan naik kembali dari cradle
            cubicTo(
                cx + cradleR * 0.7f, depth,
                rightEnd - handleOffset, 0f,
                rightEnd, 0f
            )

            // Garis lurus ke sudut kanan atas
            lineTo(size.width - cornerR, 0f)
            quadraticTo(size.width, 0f, size.width, cornerR)

            // Garis ke bawah kanan
            lineTo(size.width, size.height)

            // Garis ke bawah kiri
            lineTo(0f, size.height)
            close()
        }

        return Outline.Generic(path)
    }
}

@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        // Bar Utama dengan Shape Lengkung (Cradle) & Bayangan Lembut
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = BottomBarCradleShape(),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.14f)
                ),
            shape = BottomBarCradleShape(),
            color = SurfaceWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 1: Home / Beranda
                BottomNavItem(
                    icon = Icons.Outlined.Home,
                    selectedIcon = Icons.Filled.Home,
                    label = "Beranda",
                    isSelected = currentScreen == Screen.DETECTION,
                    onClick = { onScreenSelected(Screen.DETECTION) },
                    modifier = Modifier.weight(1f)
                )

                // Item 2: Riwayat Deteksi
                BottomNavItem(
                    icon = Icons.Outlined.History,
                    selectedIcon = Icons.Filled.History,
                    label = "Riwayat",
                    isSelected = currentScreen == Screen.HISTORY,
                    onClick = { onScreenSelected(Screen.HISTORY) },
                    modifier = Modifier.weight(1f)
                )

                // Space tengah untuk FAB
                Spacer(modifier = Modifier.width(68.dp))

                // Item 3: Bahasa Didukung
                BottomNavItem(
                    icon = Icons.Outlined.Language,
                    selectedIcon = Icons.Filled.Language,
                    label = "Bahasa",
                    isSelected = currentScreen == Screen.SUPPORTED_LANGUAGES,
                    onClick = { onScreenSelected(Screen.SUPPORTED_LANGUAGES) },
                    modifier = Modifier.weight(1f)
                )

                // Item 4: Panduan & Info
                BottomNavItem(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Panduan",
                    isSelected = currentScreen == Screen.USAGE_GUIDE || currentScreen == Screen.ABOUT,
                    onClick = { onScreenSelected(Screen.USAGE_GUIDE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating Action Button Bulat Tengah (+) yang menonjol ke atas
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        ) {
            Surface(
                onClick = onFabClick,
                shape = CircleShape,
                color = DarkBlue,
                shadowElevation = 8.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Deteksi Baru",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryBlue else TextTertiary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PrimaryBlue else TextTertiary,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
