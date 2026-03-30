package com.example.biblememorize.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = NeonBlue,
    onPrimary = Color.White,
    primaryContainer = DeepBlue,
    onPrimaryContainer = Mist,
    secondary = Lime,
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Night,
    background = Night,
    onBackground = Color.White,
    surface = Graphite,
    surfaceVariant = Slate,
    onSurface = Color.White,
    onSurfaceVariant = Silver,
    outline = Silver.copy(alpha = 0.65f)
)

@Composable
fun BibleMemorizeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
