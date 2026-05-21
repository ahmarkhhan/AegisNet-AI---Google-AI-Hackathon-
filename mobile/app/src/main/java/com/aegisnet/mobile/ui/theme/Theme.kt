package com.aegisnet.mobile.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// AegisNet Military Premium EOC Cyber-Command Color Palette
val AegisDark = Color(0xFF080C14)       // Immersive slate-midnight background
val AegisPanel = Color(0xFF0F1A30)      // Sleek tactical control panel
val AegisSlate = Color(0xFF1E2F50)      // High-contrast slate gridlines and borders
val AegisPrimary = Color(0xFF00E5FF)    // Vivid neon electric cyan primary accent
val AegisAlert = Color(0xFFFF2A6D)      // Neon danger/casualty red
val AegisWarn = Color(0xFFFF9F0A)       // Warm emergency amber
val AegisSuccess = Color(0xFF05FF80)    // Vivid safety/mesh-synced green

private val DarkColorScheme = darkColorScheme(
    primary = AegisPrimary,
    onPrimary = Color.White,
    background = AegisDark,
    onBackground = Color(0xFFF8FAFC),
    surface = AegisPanel,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = AegisSlate,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = AegisAlert,
    onError = Color.White,
)

@Composable
fun AegisNetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
