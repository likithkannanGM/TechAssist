package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.StepExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepExtractorTest {

    private fun chunk(name: String, page: Int, text: String) =
        RetrievedChunk("c-$name-$page", "d-$name", name, page, text, 0.8f)

    @Test fun `numbered steps are extracted and wrapped lines are joined`() {
        val text = "Intro text\n1. Switch off the supply\nand lock the isolator.\n2) Remove the front cover."
        val steps = StepExtractor.extract(listOf(chunk("a.pdf", 3, text)))
        assertEquals(
            listOf("Switch off the supply and lock the isolator.", "Remove the front cover."),
            steps.map { it.text },
        )
        assertEquals("a.pdf, p. 3", steps[0].source)
    }

    @Test fun `bullet and lettered markers are recognised`() {
        val text = "• Check the earth connection\n- Tighten all terminals\n(a) Verify the supply voltage"
        assertEquals(3, StepExtractor.extract(listOf(chunk("a.pdf", 1, text))).size)
    }

    @Test fun `decimal numbers are not mistaken for steps`() {
        val text = "2.5 mm cable size is required here\n3.2 Wiring overview"
        assertTrue(StepExtractor.extract(listOf(chunk("a.pdf", 1, text))).isEmpty())
    }

    @Test fun `table of contents lines with dot leaders are skipped`() {
        val text = "1. Introduction ........ 5"
        assertTrue(StepExtractor.extract(listOf(chunk("a.pdf", 1, text))).isEmpty())
    }

    @Test fun `very short items are dropped`() {
        assertTrue(StepExtractor.extract(listOf(chunk("a.pdf", 1, "1. OK"))).isEmpty())
    }

    @Test fun `a blank line ends a step`() {
        val text = "1. Isolate the supply\n\nUnrelated paragraph text"
        val steps = StepExtractor.extract(listOf(chunk("a.pdf", 1, text)))
        assertEquals(listOf("Isolate the supply"), steps.map { it.text })
    }

    @Test fun `duplicates across chunks appear once`() {
        val text = "1. Isolate the supply before opening."
        val steps = StepExtractor.extract(listOf(chunk("a.pdf", 1, text), chunk("a.pdf", 2, text)))
        assertEquals(1, steps.size)
    }

    @Test fun `steps follow document and page order, not retrieval order`() {
        val late = chunk("a.pdf", 5, "1. Final inspection of all terminals")
        val early = chunk("a.pdf", 2, "1. Mount the unit on the panel")
        val steps = StepExtractor.extract(listOf(late, early))
        assertEquals(listOf("Mount the unit on the panel", "Final inspection of all terminals"), steps.map { it.text })
    }
}