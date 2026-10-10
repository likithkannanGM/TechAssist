package com.example.techassist.data.search

import kotlin.math.sqrt

object VectorSearch {
    data class ScoredIndex(val index: Int, val score: Float)

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vector dimension mismatch: ${a.size} vs ${b.size}" }
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom < 1e-9f) 0f else dot / denom
    }

    /** Scores every candidate against the query and returns the best k, highest score first. */
    fun topK(query: FloatArray, candidates: List<FloatArray>, k: Int): List<ScoredIndex> =
        candidates.indices
            .map { ScoredIndex(it, cosineSimilarity(query, candidates[it])) }
            .sortedByDescending { it.score }
            .take(k)
}