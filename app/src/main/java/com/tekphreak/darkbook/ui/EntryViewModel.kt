package com.tekphreak.darkbook.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tekphreak.darkbook.data.DarkbookDatabase
import com.tekphreak.darkbook.data.Entry
import com.tekphreak.darkbook.data.ImageStore
import com.tekphreak.darkbook.data.LocationHelper
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = DarkbookDatabase.getInstance(application).entryDao()

    val entries: StateFlow<List<Entry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getById(id: Long): Entry? = dao.getById(id)

    fun createEntry(body: String, imagePath: String?, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            // Location is a snapshot taken once at creation, same as createdAt —
            // it isn't re-captured on later edits.
            val location = LocationHelper.getCurrentLocation(getApplication())
            val id = dao.insert(
                Entry(
                    createdAt = System.currentTimeMillis(),
                    body = body,
                    imagePath = imagePath,
                    latitude = location?.first,
                    longitude = location?.second
                )
            )
            onSaved(id)
        }
    }

    fun updateEntry(entry: Entry, newBody: String, newImagePath: String?) {
        viewModelScope.launch {
            if (entry.imagePath != null && entry.imagePath != newImagePath) {
                ImageStore.deleteImage(getApplication(), entry.imagePath)
            }
            dao.update(entry.copy(body = newBody, imagePath = newImagePath, editedAt = System.currentTimeMillis()))
        }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            dao.delete(entry)
            entry.imagePath?.let { ImageStore.deleteImage(getApplication(), it) }
        }
    }
}
