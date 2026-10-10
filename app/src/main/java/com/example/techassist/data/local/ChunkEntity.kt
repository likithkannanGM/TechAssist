package com.example.techassist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chunks")
data class ChunkEntity(
    @PrimaryKey val id: String,       // UUID string
    val documentId: String,
    val pageNumber: Int,
    val chunkIndexOnPage: Int,        // 0-based position within the page, for ordering/debugging
    val text: String,
    val wordCount: Int,
)