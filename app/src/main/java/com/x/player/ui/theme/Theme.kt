package com.x.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val XColors = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF26005F),
    primaryContainer = Color(0xFF3C2A7A),
    onPrimaryContainer = Color(0xFFE6DDFF),
    secondary = Color(0xFFCBC0E8),
    onSecondary = Color(0xFF332D4A),
    background = Color(0xFF0B0B12),
    onBackground = Color(0xFFE7E3EF),
    surface = Color(0xFF0B0B12),
    onSurface = Color(0xFFE7E3EF),
    surfaceVariant = Color(0xFF1D1B2A),
    onSurfaceVariant = Color(0xFFB4AFC4),
    outline = Color(0xFF7A7589),
    error = Color(0xFFFFB4AB),
)

@Composable
fun XTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XColors,
        typography = Typography(),
        content = content,
    )
}
