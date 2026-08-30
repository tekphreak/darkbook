package com.tekphreak.darkbook.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tekphreak.darkbook.R
import com.tekphreak.darkbook.data.Entry
import com.tekphreak.darkbook.data.ImageStore
import com.tekphreak.darkbook.ui.theme.LocalEntryFontSize
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

// Threads' package name has changed before (Meta rebrand history) — verify at build time.
private const val THREADS_PACKAGE = "com.instagram.barcelona"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entry: Entry,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareConfirm by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    val createdLabel = remember(entry.createdAt) {
        Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).format(ENTRY_DATE_FORMATTER)
    }
    val editedLabel = entry.editedAt?.let {
        stringResource(R.string.entry_edited_label, Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(ENTRY_DATE_FORMATTER))
    }
    val locationLabel = remember(entry.latitude, entry.longitude) {
        val lat = entry.latitude
        val lon = entry.longitude
        if (lat != null && lon != null) String.format(Locale.US, "%.5f, %.5f", lat, lon) else null
    }
    val bitmap = remember(entry.imagePath) { entry.imagePath?.let { ImageStore.loadBitmap(context, it) } }

    // Takes priority over the screen-level back handling in MainActivity while
    // the full-screen image is up, so back closes it before leaving the screen.
    BackHandler(enabled = showFullScreenImage) {
        showFullScreenImage = false
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                createdLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            locationLabel?.let {
                                Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.entry_edit_title))
                        }
                        IconButton(onClick = { showShareConfirm = true }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_button))
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.entry_delete))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                editedLabel?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 8.dp)
                            .clickable { showFullScreenImage = true },
                        contentScale = ContentScale.Crop
                    )
                }
                Text(entry.body, style = MaterialTheme.typography.bodyLarge.copy(fontSize = LocalEntryFontSize.current))
            }
        }

        if (showFullScreenImage) {
            bitmap?.let { bmp ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { showFullScreenImage = false },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.entry_delete_confirm_title)) },
                text = { Text(stringResource(R.string.entry_delete_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }) { Text(stringResource(R.string.entry_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.share_confirm_cancel))
                    }
                }
            )
        }

        if (showShareConfirm) {
            AlertDialog(
                onDismissRequest = { showShareConfirm = false },
                title = { Text(stringResource(R.string.share_confirm_title)) },
                text = { Text(stringResource(R.string.share_confirm_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        showShareConfirm = false
                        shareToThreads(context, entry.body)
                    }) { Text(stringResource(R.string.share_confirm_proceed)) }
                },
                dismissButton = {
                    TextButton(onClick = { showShareConfirm = false }) {
                        Text(stringResource(R.string.share_confirm_cancel))
                    }
                }
            )
        }
    }
}

// Along with the long-press export in EntryListScreen, this Intent is the only
// egress point for entry content in the whole app — nothing here logs, caches,
// or transmits the body anywhere else.
private fun shareToThreads(context: Context, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
        setPackage(THREADS_PACKAGE)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(fallback, null))
    }
}
