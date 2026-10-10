package com.example.techassist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>)

    @Query("SELECT * FROM chunks WHERE documentId = :documentId ORDER BY pageNumber ASC, chunkIndexOnPage ASC")
    suspend fun getForDocument(documentId: String): List<ChunkEntity>

    @Query("SELECT * FROM chunks")
    suspend fun getAll(): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ChunkEntity>

    @Query("SELECT COUNT(*) FROM chunks WHERE documentId = :documentId")
    suspend fun countForDocument(documentId: String): Int

    @Query("DELETE FROM chunks WHERE documentId = :documentId")
    suspend fun deleteForDocument(documentId: String)
}