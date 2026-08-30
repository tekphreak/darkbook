package com.tekphreak.darkbook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DEFAULT_ENTRY_FONT_SIZE = 16.sp
val MIN_ENTRY_FONT_SIZE = 12.sp
val MAX_ENTRY_FONT_SIZE = 28.sp

fun darkbookTypography(fontFamily: FontFamily) = Typography(
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = DEFAULT_ENTRY_FONT_SIZE
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    )
)
