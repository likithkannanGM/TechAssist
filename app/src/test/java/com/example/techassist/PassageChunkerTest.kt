package com.example.techassist

import com.example.techassist.data.chunk.PassageChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassageChunkerTest {

    private fun line(n: Int) = "Line number $n describes one separate installation detail of the unit"
    private val twentyLines = (1..20).joinToString("\n") { line(it) }

    @Test fun `blank text gives no chunks`() {
        assertTrue(PassageChunker.chunkPageText("  \n \n").isEmpty())
    }

    @Test fun `a short page stays in one chunk`() {
        val chunks = PassageChunker.chunkPageText("Mode switch position 1\nSetting for wall mounting")
        assertEquals(1, chunks.size)
    }

    @Test fun `no chunk exceeds the maximum`() {
        val chunks = PassageChunker.chunkPageText(twentyLines)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.text.length <= PassageChunker.MAX_CHARS })
    }

    @Test fun `every line appears in at least one chunk`() {
        val chunks = PassageChunker.chunkPageText(twentyLines)
        for (n in 1..20) {
            assertTrue("line $n missing", chunks.any { it.text.lines().contains(line(n)) })
        }

    }

    @Test fun `consecutive chunks overlap by their last line`() {
        val chunks = PassageChunker.chunkPageText(twentyLines)
        val lastOfFirst = chunks[0].text.lines().last()
        assertTrue(chunks[1].text.lines().contains(lastOfFirst))
    }

    @Test fun `a long unbroken run is split and still respects the maximum`() {
        val chunks = PassageChunker.chunkPageText("x".repeat(2000))
        assertTrue(chunks.size >= 4)
        assertTrue(chunks.all { it.text.length <= PassageChunker.MAX_CHARS })
        assertEquals(2000, chunks.sumOf { chunk -> chunk.text.count { it == 'x' } })   // nothing lost
    }

    @Test fun `table of contents dot leaders are dropped`() {
        val toc = (1..20).joinToString("\n") { "Section $it " + ".".repeat(80) + " $it" }
        assertTrue(PassageChunker.chunkPageText(toc).isEmpty())
    }

    @Test fun `a stray page number is dropped but short real text is kept`() {
        assertTrue(PassageChunker.chunkPageText("12").isEmpty())
        assertEquals(1, PassageChunker.chunkPageText("NO DATA").size)
    }

    @Test fun `word count counts whitespace separated words`() {
        assertEquals(3, PassageChunker.chunkPageText("one two\nthree")[0].wordCount)
    }
}