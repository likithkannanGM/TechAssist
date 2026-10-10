package com.example.techassist

import com.example.techassist.data.search.VectorSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.abs

class VectorSearchTest {

    @Test
    fun `identical vectors score close to 1`() {
        val v = floatArrayOf(0.6f, 0.8f)
        val score = VectorSearch.cosineSimilarity(v, v)
        assertEquals(1.0f, score, 0.0001f)
    }

    @Test
    fun `orthogonal vectors score 0`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0f, VectorSearch.cosineSimilarity(a, b), 0.0001f)
    }

    @Test
    fun `opposite vectors score close to -1`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(-1f, 0f)
        assertEquals(-1.0f, VectorSearch.cosineSimilarity(a, b), 0.0001f)
    }

    @Test
    fun `mismatched dimensions throw`() {
        assertThrows(IllegalArgumentException::class.java) {
            VectorSearch.cosineSimilarity(floatArrayOf(1f, 2f), floatArrayOf(1f))
        }
    }

    @Test
    fun `topK returns best scores in descending order and respects k`() {
        val query = floatArrayOf(1f, 0f)
        val candidates = listOf(
            floatArrayOf(1f, 0f),       // identical -> 1.0
            floatArrayOf(0.7f, 0.7f),   // ~0.707
            floatArrayOf(0f, 1f),       // orthogonal -> 0.0
            floatArrayOf(-1f, 0f),      // opposite -> -1.0
        )
        val top2 = VectorSearch.topK(query, candidates, k = 2)
        assertEquals(2, top2.size)
        assertEquals(0, top2[0].index) // identical vector wins
        assertEquals(1, top2[1].index) // second-closest
        assertEquals(true, top2[0].score > top2[1].score)
    }

    @Test
    fun `topK with k larger than candidate count returns all candidates`() {
        val query = floatArrayOf(1f, 0f)
        val candidates = listOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f))
        assertEquals(2, VectorSearch.topK(query, candidates, k = 10).size)
    }
}