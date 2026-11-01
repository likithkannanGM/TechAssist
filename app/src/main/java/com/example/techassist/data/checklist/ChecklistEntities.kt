package com.example.techassist.data.checklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val topic: String,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val checklistId: String,
    val position: Int,
    val text: String,
    val source: String,     // "file.pdf, p. 12"
    val isSafety: Boolean,
    val isDone: Boolean,
)

data class ChecklistProgress(
    val checklistId: String,
    val totalCount: Int,
    val doneCount: Int,
)