package com.example.techassist

import com.example.techassist.data.chunk.TextChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextChunkerTest {

    @Test
    fun `empty text produces no chunks`() {
        assertEquals(0, TextChunker.chunkPageText("").size)
        assertEquals(0, TextChunker.chunkPageText("   \n  ").size)
    }

    @Test
    fun `short text produces exactly one chunk`() {
        val text = "Connect the power cable. Then turn on the switch."
        val chunks = TextChunker.chunkPageText(text, chunkSizeWords = 220, overlapWords = 40)
        assertEquals(1, chunks.size)
    }

    @Test
    fun `long text is split into multiple chunks within size limit`() {
        val sentence = "This is a sample technical instruction sentence for testing chunk boundaries."
        val longText = (1..50).joinToString(" ") { sentence } // ~650 words
        val chunks = TextChunker.chunkPageText(longText, chunkSizeWords = 220, overlapWords = 40)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.wordCount <= 220) }
    }

    @Test
    fun `consecutive chunks share overlap words`() {
        val sentence = "Step number %d requires checking the wiring carefully before proceeding."
        val longText = (1..40).joinToString(" ") { sentence.format(it) }
        val chunks = TextChunker.chunkPageText(longText, chunkSizeWords = 220, overlapWords = 40)
        assertTrue(chunks.size >= 2)
        val firstChunkTailWords = chunks[0].text.split(" ").takeLast(10)
        val secondChunkStartWords = chunks[1].text.split(" ").take(20)
        assertTrue(firstChunkTailWords.any { it in secondChunkStartWords })
    }

    @Test
    fun `single oversized sentence is still split`() {
        val hugeSentence = "word".repeat(1).let { (1..500).joinToString(" ") { "token$it" } } + "."
        val chunks = TextChunker.chunkPageText(hugeSentence, chunkSizeWords = 220, overlapWords = 40)
        assertTrue(chunks.size > 1)
    }
}