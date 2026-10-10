package com.example.techassist.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: DocumentStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DocumentStatus = DocumentStatus.valueOf(value)
}