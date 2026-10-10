package com.example.techassist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,          // UUID string, generated at import time
    val displayName: String,             // original file name shown to the user
    val filePath: String,                // path under app filesDir, e.g. "documents/<id>.pdf"
    val sizeBytes: Long,
    val pageCount: Int,
    val chunkCount: Int,
    val status: DocumentStatus,
    val errorMessage: String?,
    val importedAtEpochMillis: Long,
)