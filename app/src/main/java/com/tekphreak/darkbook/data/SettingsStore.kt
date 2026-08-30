package com.tekphreak.darkbook.data

import android.content.Context
import com.tekphreak.darkbook.ui.theme.DEFAULT_ENTRY_FONT_SIZE
import com.tekphreak.darkbook.ui.theme.FontChoice

/** Plain (unencrypted) UI preferences — no diary content lives here. */
object SettingsStore {
    private const val PREFS_NAME = "darkbook_settings"
    private const val KEY_LONG_PRESS_EXPORT = "long_press_export_enabled"
    private const val KEY_FONT_CHOICE = "font_choice"
    private const val KEY_FONT_SIZE_SP = "font_size_sp"

    fun isLongPressExportEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LONG_PRESS_EXPORT, true)

    fun setLongPressExportEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LONG_PRESS_EXPORT, enabled).apply()
    }

    fun getFontChoice(context: Context): FontChoice =
        FontChoice.fromId(prefs(context).getString(KEY_FONT_CHOICE, null))

    fun setFontChoice(context: Context, choice: FontChoice) {
        prefs(context).edit().putString(KEY_FONT_CHOICE, choice.id).apply()
    }

    fun getFontSizeSp(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SIZE_SP, DEFAULT_ENTRY_FONT_SIZE.value)

    fun setFontSizeSp(context: Context, sizeSp: Float) {
        prefs(context).edit().putFloat(KEY_FONT_SIZE_SP, sizeSp).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
