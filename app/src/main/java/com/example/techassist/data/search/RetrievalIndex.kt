package com.example.techassist.data.search

import kotlin.math.max
import kotlin.math.sqrt

class IndexedChunk(
    val id: String,
    val documentId: String,
    val documentName: String,
    val pageNumber: Int,
    val text: String,
    val vector: FloatArray,
)

/** All chunks, their vectors and a keyword index, held in memory for fast scoring. */
class RetrievalIndex(private val chunks: List<IndexedChunk>) {
    data class Hit(val chunk: IndexedChunk, val dense: Float, val lexical: Float, val score: Float)

    private val lexicalIndex = LexicalIndex(chunks.map { it.text })
    private val norms = FloatArray(chunks.size) { norm(chunks[it].vector) }

    val size: Int get() = chunks.size

    fun search(
        queryVector: FloatArray,
        queryText: String,
        topK: Int,
        denseWeight: Float,
        documentIdFilter: String? = null,
    ): List<Hit> {
        if (chunks.isEmpty()) return emptyList()
        val lexical = lexicalIndex.scores(queryText)
        val queryNorm = norm(queryVector)
        val lexicalWeight = 1f - denseWeight

        val hits = ArrayList<Hit>()
        for (i in chunks.indices) {
            val chunk = chunks[i]
            if (documentIdFilter != null && chunk.documentId != documentIdFilter) continue
            if (chunk.vector.size != queryVector.size) continue

            var dot = 0f
            for (d in queryVector.indices) dot += queryVector[d] * chunk.vector[d]
            val denom = queryNorm * norms[i]
            val dense = if (denom < 1e-9f) 0f else dot / denom
            val score = denseWeight * max(dense, 0f) + lexicalWeight * lexical[i]
            hits.add(Hit(chunk, dense, lexical[i], score))
        }
        return hits.sortedByDescending { it.score }.take(topK)
    }

    private fun norm(v: FloatArray): Float {
        var s = 0f
        for (x in v) s += x * x
        return sqrt(s)
    }
}