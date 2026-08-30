package com.tekphreak.darkbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.fragment.app.FragmentActivity
import com.tekphreak.darkbook.data.Entry
import com.tekphreak.darkbook.data.SettingsStore
import com.tekphreak.darkbook.ui.EntryDetailScreen
import com.tekphreak.darkbook.ui.EntryEditScreen
import com.tekphreak.darkbook.ui.EntryListScreen
import com.tekphreak.darkbook.ui.EntryViewModel
import com.tekphreak.darkbook.ui.LockScreen
import com.tekphreak.darkbook.ui.theme.DEFAULT_ENTRY_FONT_SIZE
import com.tekphreak.darkbook.ui.theme.DarkbookTheme
import androidx.compose.ui.unit.sp

private sealed class Screen {
    object Lock : Screen()
    object List : Screen()
    object NewEntry : Screen()
    data class EditEntry(val entry: Entry) : Screen()
    data class Detail(val entry: Entry) : Screen()
}

class MainActivity : FragmentActivity() {

    private val viewModel: EntryViewModel by viewModels()
    private val screenState = mutableStateOf<Screen>(Screen.Lock)
    private val exportEnabledState = mutableStateOf(false)
    private val fontFamilyState = mutableStateOf<FontFamily>(FontFamily.Default)
    private val fontSizeState = mutableStateOf<TextUnit>(DEFAULT_ENTRY_FONT_SIZE)
    private var backgroundedAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportEnabledState.value = SettingsStore.isLongPressExportEnabled(this)
        fontFamilyState.value = SettingsStore.getFontChoice(this).fontFamily
        fontSizeState.value = SettingsStore.getFontSizeSp(this).sp
        setContent {
            val fontFamily by fontFamilyState
            val fontSize by fontSizeState
            DarkbookTheme(fontFamily = fontFamily, bodyFontSize = fontSize) {
                var screen by screenState
                val exportEnabled by exportEnabledState
                val entries by viewModel.entries.collectAsState()

                // Lock/List have nowhere to go back to — let the system default
                // (exit the app) handle those; every other screen maps back to
                // the same place its toolbar back arrow goes.
                BackHandler(enabled = screen !is Screen.Lock && screen !is Screen.List) {
                    when (val current = screen) {
                        is Screen.NewEntry -> screen = Screen.List
                        is Screen.EditEntry -> screen = Screen.Detail(current.entry)
                        is Screen.Detail -> screen = Screen.List
                        else -> {}
                    }
                }

                when (val current = screen) {
                    is Screen.Lock -> LockScreen(
                        activity = this@MainActivity,
                        onUnlocked = { screen = Screen.List }
                    )
                    is Screen.List -> EntryListScreen(
                        entries = entries,
                        exportEnabled = exportEnabled,
                        onOpenSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                        onNewEntry = { screen = Screen.NewEntry },
                        onOpenEntry = { screen = Screen.Detail(it) },
                        onExportEntry = { exportEntryText(it) }
                    )
                    is Screen.NewEntry -> EntryEditScreen(
                        isEditing = false,
                        initialBody = "",
                        initialImagePath = null,
                        onBack = { screen = Screen.List },
                        onSave = { body, imagePath ->
                            viewModel.createEntry(body, imagePath) { screen = Screen.List }
                        }
                    )
                    is Screen.EditEntry -> EntryEditScreen(
                        isEditing = true,
                        initialBody = current.entry.body,
                        initialImagePath = current.entry.imagePath,
                        onBack = { screen = Screen.Detail(current.entry) },
                        onSave = { body, imagePath ->
                            viewModel.updateEntry(current.entry, body, imagePath)
                            screen = Screen.List
                        }
                    )
                    is Screen.Detail -> EntryDetailScreen(
                        entry = current.entry,
                        onBack = { screen = Screen.List },
                        onEdit = { screen = Screen.EditEntry(current.entry) },
                        onDelete = {
                            viewModel.deleteEntry(current.entry)
                            screen = Screen.List
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        exportEnabledState.value = SettingsStore.isLongPressExportEnabled(this)
        fontFamilyState.value = SettingsStore.getFontChoice(this).fontFamily
        fontSizeState.value = SettingsStore.getFontSizeSp(this).sp
        val since = backgroundedAt
        backgroundedAt = null
        if (since != null && System.currentTimeMillis() - since > LOCK_GRACE_PERIOD_MS) {
            screenState.value = Screen.Lock
        }
    }

    private fun exportEntryText(entry: Entry) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, entry.body)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.entry_export_chooser_title)))
    }

    private companion object {
        // Avoids re-locking on quick app-switches per darkbook.md.
        const val LOCK_GRACE_PERIOD_MS = 30_000L
    }
}
