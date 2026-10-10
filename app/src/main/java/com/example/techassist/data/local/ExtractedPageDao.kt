package com.example.techassist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExtractedPageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<ExtractedPageEntity>)

    @Query("SELECT * FROM extracted_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getForDocument(documentId: String): List<ExtractedPageEntity>

    @Query("DELETE FROM extracted_pages WHERE documentId = :documentId")
    suspend fun deleteForDocument(documentId: String)
}