package com.example.biblememorize.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = WarmGold,
    onPrimary = Linen,
    primaryContainer = Sand,
    onPrimaryContainer = Charcoal,
    secondary = Olive,
    onSecondary = Linen,
    secondaryContainer = Clay.copy(alpha = 0.32f),
    onSecondaryContainer = Charcoal,
    tertiary = Clay,
    onTertiary = Charcoal,
    tertiaryContainer = Sand,
    onTertiaryContainer = Charcoal,
    background = Linen,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    onSurfaceVariant = Charcoal.copy(alpha = 0.7f)
)

private val DarkColors = darkColorScheme(
    primary = Clay,
    onPrimary = Charcoal,
    primaryContainer = Olive,
    onPrimaryContainer = Linen,
    secondary = WarmGold,
    onSecondary = Charcoal,
    tertiary = Sand,
    onTertiary = Charcoal,
    background = Charcoal,
    onBackground = Linen,
    surface = Color(0xFF3A352F),
    onSurface = Linen,
    onSurfaceVariant = Linen.copy(alpha = 0.74f)
)

@Composable
fun BibleMemorizeTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
