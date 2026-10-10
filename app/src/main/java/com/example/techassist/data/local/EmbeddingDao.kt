package com.example.techassist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<EmbeddingEntity>)

    @Query("SELECT * FROM embeddings")
    suspend fun getAll(): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE documentId = :documentId")
    suspend fun getForDocument(documentId: String): List<EmbeddingEntity>

    @Query("DELETE FROM embeddings WHERE documentId = :documentId")
    suspend fun deleteForDocument(documentId: String)
}