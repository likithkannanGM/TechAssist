package com.example.techassist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DocumentEntity::class, ExtractedPageEntity::class, ChunkEntity::class, EmbeddingEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun extractedPageDao(): ExtractedPageDao
    abstract fun chunkDao(): ChunkDao
    abstract fun embeddingDao(): EmbeddingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "techassist.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}