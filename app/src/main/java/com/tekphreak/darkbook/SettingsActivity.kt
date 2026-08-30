package com.tekphreak.darkbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tekphreak.darkbook.data.SettingsStore
import com.tekphreak.darkbook.ui.SplashContent
import com.tekphreak.darkbook.ui.theme.DarkbookTheme
import com.tekphreak.darkbook.ui.theme.FontChoice
import com.tekphreak.darkbook.ui.theme.MAX_ENTRY_FONT_SIZE
import com.tekphreak.darkbook.ui.theme.MIN_ENTRY_FONT_SIZE

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var fontChoice by remember { mutableStateOf(SettingsStore.getFontChoice(this)) }
            var fontSizeSp by remember { mutableFloatStateOf(SettingsStore.getFontSizeSp(this)) }
            DarkbookTheme(fontFamily = fontChoice.fontFamily, bodyFontSize = fontSizeSp.sp) {
                SettingsScreen(
                    fontChoice = fontChoice,
                    onFontChoiceChange = {
                        fontChoice = it
                        SettingsStore.setFontChoice(this, it)
                    },
                    fontSizeSp = fontSizeSp,
                    onFontSizeChange = {
                        fontSizeSp = it
                        SettingsStore.setFontSizeSp(this, it)
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    fontChoice: FontChoice,
    onFontChoiceChange: (FontChoice) -> Unit,
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var longPressExportEnabled by remember { mutableStateOf(SettingsStore.isLongPressExportEnabled(context)) }
    var showSplashPreview by remember { mutableStateOf(false) }

    // Lets the splash preview also be dismissed with the back button, not just a tap.
    BackHandler(enabled = showSplashPreview) {
        showSplashPreview = false
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = longPressExportEnabled, onClick = {
                            longPressExportEnabled = !longPressExportEnabled
                            SettingsStore.setLongPressExportEnabled(context, longPressExportEnabled)
                        })
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Checkbox(
                        checked = longPressExportEnabled,
                        onCheckedChange = {
                            longPressExportEnabled = it
                            SettingsStore.setLongPressExportEnabled(context, it)
                        }
                    )
                    Text(
                        stringResource(R.string.settings_long_press_export),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    stringResource(R.string.settings_font_family),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(16.dp)
                )
                FontChoice.entries.forEach { choice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = choice == fontChoice,
                                onClick = { onFontChoiceChange(choice) }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        RadioButton(selected = choice == fontChoice, onClick = { onFontChoiceChange(choice) })
                        Text(
                            choice.displayName,
                            fontFamily = choice.fontFamily,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    stringResource(R.string.settings_font_size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    stringResource(R.string.settings_font_size_preview),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSizeSp.sp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Slider(
                    value = fontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = MIN_ENTRY_FONT_SIZE.value..MAX_ENTRY_FONT_SIZE.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Button(
                    onClick = { showSplashPreview = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.settings_show_splash))
                }
            }
        }

        if (showSplashPreview) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showSplashPreview = false }
            ) {
                SplashContent()
            }
        }
    }
}
