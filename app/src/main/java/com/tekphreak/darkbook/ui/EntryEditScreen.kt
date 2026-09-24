package com.tekphreak.darkbook.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tekphreak.darkbook.R
import com.tekphreak.darkbook.data.ImageStore
import com.tekphreak.darkbook.ui.theme.LocalEntryFontSize

private val SaveIconColor = Color(0xFF808080)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    isEditing: Boolean,
    initialBody: String,
    initialImagePath: String?,
    onBack: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var body by remember { mutableStateOf(initialBody) }
    var imagePath by remember { mutableStateOf(initialImagePath) }
    var saved by remember { mutableStateOf(false) }

    // Swaps in a new image path, deleting whatever file was attached before it —
    // but only if that file was picked during this same session (not yet saved
    // to the entry). The entry's originally-saved image, if the user backs out
    // without saving, is left alone for onSave/updateEntry's own diff to handle.
    fun replaceImage(newPath: String?) {
        val old = imagePath
        if (old != null && old != initialImagePath) {
            ImageStore.deleteImage(context, old)
        }
        imagePath = newPath
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) replaceImage(ImageStore.saveImage(context, uri))
    }

    // Asked for just-in-time, only when creating a new entry, so editing an
    // existing one never re-prompts. Saving proceeds either way — a denial
    // just means the entry ends up with no location, same as any other entry.
    val requestLocationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        saved = true
        onSave(body, imagePath)
    }

    fun handleSave() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!isEditing && !hasLocationPermission) {
            RelockGuard.suppressNextRelock()
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            saved = true
            onSave(body, imagePath)
        }
    }

    // Cleans up an image picked during this session if the screen is left
    // without saving (back button, process death aside) — otherwise it would
    // sit on disk with no Entry ever pointing at it again. Reads `imagePath`/
    // `saved` directly rather than via rememberUpdatedState: both are backed
    // by a stable remember{}'d MutableState, so a direct read here already
    // sees the latest value at the moment onDispose actually runs. Wrapping
    // them in rememberUpdatedState was worse than useless — when saving
    // synchronously flips `saved` and navigates away in the same click
    // handler (the edit-existing-entry path), this composable never gets an
    // intermediate recomposition to commit that value via SideEffect before
    // disposal, so onDispose saw a stale `saved = false` and deleted the
    // image that had just been written to the entry.
    DisposableEffect(Unit) {
        onDispose {
            if (!saved && imagePath != null && imagePath != initialImagePath) {
                ImageStore.deleteImage(context, imagePath!!)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.entry_edit_title else R.string.entry_list_new))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        RelockGuard.suppressNextRelock()
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_attach_paperclip),
                            contentDescription = stringResource(R.string.entry_attach_image),
                            tint = Color.Unspecified
                        )
                    }
                    val canSave = body.isNotBlank()
                    IconButton(onClick = { handleSave() }, enabled = canSave) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save_floppy),
                            contentDescription = stringResource(R.string.entry_save),
                            tint = SaveIconColor.copy(alpha = if (canSave) 1f else 0.38f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            imagePath?.let { path ->
                val bitmap = remember(path) { ImageStore.loadBitmap(context, path) }
                bitmap?.let { bmp ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { replaceImage(null) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.entry_remove_image))
                        }
                    }
                }
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.entry_edit_hint)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = LocalEntryFontSize.current),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )
        }
    }
}
