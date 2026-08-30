package com.tekphreak.darkbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val editedAt: Long? = null,
    val body: String,
    val imagePath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
