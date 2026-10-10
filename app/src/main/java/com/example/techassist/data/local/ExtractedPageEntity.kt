package com.example.techassist.data.local

import androidx.room.Entity

@Entity(tableName = "extracted_pages", primaryKeys = ["documentId", "pageNumber"])
data class ExtractedPageEntity(
    val documentId: String,
    val pageNumber: Int,  // 1-based, matches the printed/PDF page number
    val text: String,
)