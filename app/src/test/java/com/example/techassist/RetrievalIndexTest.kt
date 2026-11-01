package com.example.techassist

import com.example.techassist.data.search.IndexedChunk
import com.example.techassist.data.search.RetrievalIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrievalIndexTest {
    private fun chunk(id: String, doc: String, text: String, vx: Float, vy: Float) =
        IndexedChunk(id, doc, "$doc.pdf", 1, text, floatArrayOf(vx, vy))

    // A matches the keyword but is a weaker embedding match; B is the stronger embedding match with no keyword.
    private val a = chunk("A", "d1", "ZBUS corridor data bus", 0.6f, 0.8f)
    private val b = chunk("B", "d1", "unrelated text about something", 0.8f, 0.6f)
    private val query = floatArrayOf(1f, 0f)

    @Test fun `dense-only weight ranks by embedding similarity`() {
        val hits = RetrievalIndex(listOf(a, b)).search(query, "ZBUS", topK = 2, denseWeight = 1.0f)
        assertEquals("B", hits[0].chunk.id)
    }

    @Test fun `hybrid weight lets a keyword match win`() {
        val hits = RetrievalIndex(listOf(a, b)).search(query, "ZBUS", topK = 2, denseWeight = 0.6f)
        assertEquals("A", hits[0].chunk.id)
        assertTrue(hits[0].lexical > hits[1].lexical)
    }

    @Test fun `topK limits the result`() {
        assertEquals(1, RetrievalIndex(listOf(a, b)).search(query, "ZBUS", topK = 1, denseWeight = 0.6f).size)
    }


    @Test fun `document filter keeps only that document`() {
        val c = chunk("C", "d2", "ZBUS corridor data bus", 0.9f, 0.1f)
        val hits = RetrievalIndex(listOf(a, b, c)).search(query, "ZBUS", topK = 5, denseWeight = 0.6f, documentIdFilter = "d2")
        assertEquals(listOf("C"), hits.map { it.chunk.id })
    }

    @Test fun `an empty index gives no hits`() {
        assertTrue(RetrievalIndex(emptyList()).search(query, "ZBUS", topK = 3, denseWeight = 0.6f).isEmpty())
    }
}