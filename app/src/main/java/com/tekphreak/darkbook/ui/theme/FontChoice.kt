package com.tekphreak.darkbook.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.tekphreak.darkbook.R

enum class FontChoice(val id: String, val displayName: String) {
    ROBOTO("roboto", "Roboto"),
    ROBOTO_FLEX("roboto_flex", "Roboto Flex"),
    ROBOTO_MONO("roboto_mono", "Roboto Mono"),
    ROBOTO_SERIF("roboto_serif", "Roboto Serif"),
    NOTO_SANS("noto_sans", "Noto Sans"),
    NOTO_SERIF("noto_serif", "Noto Serif");

    val fontFamily: FontFamily
        get() = when (this) {
            ROBOTO -> FontFamily(Font(R.font.roboto_variable))
            ROBOTO_FLEX -> FontFamily(Font(R.font.roboto_flex_variable))
            ROBOTO_MONO -> FontFamily(Font(R.font.roboto_mono_variable))
            ROBOTO_SERIF -> FontFamily(Font(R.font.roboto_serif_variable))
            NOTO_SANS -> FontFamily(Font(R.font.noto_sans_variable))
            NOTO_SERIF -> FontFamily(Font(R.font.noto_serif_variable))
        }

    companion object {
        fun fromId(id: String?): FontChoice = entries.find { it.id == id } ?: ROBOTO
    }
}
