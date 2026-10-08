package com.alexandro.asianlanguageidentification.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandro.asianlanguageidentification.ui.theme.DarkBlue
import com.alexandro.asianlanguageidentification.ui.theme.LightBackground
import com.alexandro.asianlanguageidentification.ui.theme.LightBlue
import com.alexandro.asianlanguageidentification.ui.theme.PrimaryBlue
import com.alexandro.asianlanguageidentification.ui.theme.SurfaceWhite
import com.alexandro.asianlanguageidentification.ui.theme.TextPrimary
import com.alexandro.asianlanguageidentification.ui.theme.TextSecondary
import com.alexandro.asianlanguageidentification.ui.theme.TextTertiary

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceWhite,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Drawer Modern
                    DrawerHeader()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "MENU UTAMA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    // Daftar Item Menu
                    Screen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected) DarkBlue else TextSecondary
                                )
                            },
                            label = {
                                Column {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) DarkBlue else TextPrimary
                                    )
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                onScreenSelected(screen)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = LightBlue.copy(alpha = 0.85f),
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = DarkBlue,
                                unselectedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                                .height(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer Drawer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .background(LightBackground, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Model v2 • ML & Android",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Asian NLP Classifier",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}

@Composable
private fun DrawerHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBlue, PrimaryBlue)
                )
            )
            .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)
    ) {
        Column {
            // Icon Logo Placeholder dengan Desain Menarik
            Surface(
                shape = CircleShape,
                color = SurfaceWhite.copy(alpha = 0.20f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Logo Aplikasi",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Deteksi Bahasa Asia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = LightBlue.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text(
                    text = "16 BAHASA DIDUKUNG",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = LightBlue,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
