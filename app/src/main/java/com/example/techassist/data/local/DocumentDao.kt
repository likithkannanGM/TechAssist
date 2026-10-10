package com.example.techassist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Update
    suspend fun update(document: DocumentEntity)

    @Delete
    suspend fun delete(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY importedAtEpochMillis DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query("SELECT COUNT(*) FROM documents WHERE displayName = :displayName")
    suspend fun countByName(displayName: String): Int
}