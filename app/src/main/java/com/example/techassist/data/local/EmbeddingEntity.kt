package com.example.techassist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val chunkId: String,
    val documentId: String, // denormalized, so Phase 5 can filter by document without a join
    val vector: ByteArray,  // 384 floats, little-endian, via FloatArrayConverters
)