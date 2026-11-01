package com.example.techassist.data.checklist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChecklistEntity::class, ChecklistItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChecklistDatabase : RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile private var instance: ChecklistDatabase? = null

        fun getInstance(context: Context): ChecklistDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChecklistDatabase::class.java,
                    "checklists.db",
                ).build().also { instance = it }
            }
    }
}