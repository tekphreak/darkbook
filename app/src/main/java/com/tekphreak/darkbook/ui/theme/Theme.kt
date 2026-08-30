package com.tekphreak.darkbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit

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

/**
 * Deliberately kept separate from Typography.bodyLarge: Material3's
 * MaterialTheme wraps all content in ProvideTextStyle(typography.bodyLarge),
 * so every unstyled Text() in the app — checkboxes, dialogs, Settings labels —
 * inherits it by default. The size slider in Settings is meant to resize only
 * diary entry text, so it lives here instead and each entry-text render site
 * opts in explicitly via `MaterialTheme.typography.bodyLarge.copy(fontSize = LocalEntryFontSize.current)`.
 */
val LocalEntryFontSize = compositionLocalOf { DEFAULT_ENTRY_FONT_SIZE }

// Always dark — this app has no light theme by design (OLED-friendly diary).
@Composable
fun DarkbookTheme(
    fontFamily: FontFamily = FontFamily.Default,
    bodyFontSize: TextUnit = DEFAULT_ENTRY_FONT_SIZE,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalEntryFontSize provides bodyFontSize) {
        MaterialTheme(
            colorScheme = DarkbookColorScheme,
            typography = darkbookTypography(fontFamily),
            content = content
        )
    }
}
