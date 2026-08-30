package com.tekphreak.darkbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

private val DarkbookColorScheme = darkColorScheme(
    background = DarkbookBackground,
    surface = DarkbookBackground,
    surfaceVariant = DarkbookSurfaceElevated,
    onBackground = DarkbookText,
    onSurface = DarkbookText,
    primary = DarkbookAccent,
    onPrimary = DarkbookBackground,
    secondary = DarkbookHint,
    onSecondary = DarkbookBackground,
)

// Always dark — this app has no light theme by design (OLED-friendly diary).
@Composable
fun DarkbookTheme(fontFamily: FontFamily = FontFamily.Default, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkbookColorScheme,
        typography = darkbookTypography(fontFamily),
        content = content
    )
}
