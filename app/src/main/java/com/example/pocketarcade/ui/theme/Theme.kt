package com.example.pocketarcade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OledBlack = Color(0xFF000000)
val ArcadeSurface = Color(0xFF0B0B12)
val ArcadeSurfaceVariant = Color(0xFF151522)
val NeonCyan = Color(0xFF00F5D4)
val NeonMagenta = Color(0xFFFF007F)
val NeonYellow = Color(0xFFFEE440)

private val ArcadeDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OledBlack,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    tertiary = NeonYellow,
    background = OledBlack,
    onBackground = Color.White,
    surface = ArcadeSurface,
    onSurface = Color.White,
    surfaceVariant = ArcadeSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF2A2A3D)
)

@Composable
fun PocketArcadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArcadeDarkColorScheme,
        content = content
    )
}
