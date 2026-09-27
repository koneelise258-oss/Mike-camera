package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = DarkBg,
    primaryContainer = GlassSurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = CyanOptical,
    onSecondary = DarkBg,
    secondaryContainer = GlassSurfaceMedium,
    onSecondaryContainer = GlassIcyBlue,
    tertiary = AmberStudio,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkBgSecondary,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderMiddle,
    outlineVariant = GlassBorderBottom
)

@Composable
fun MikeAiPhotoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
