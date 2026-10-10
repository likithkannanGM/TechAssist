package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.SafetyNoteExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyNoteExtractorTest {

    private fun chunk(name: String, page: Int, text: String) =
        RetrievedChunk("c-$page", "d-$name", name, page, text, 0.8f)

    @Test fun `uppercase warning with continuation lines is captured until a blank line`() {
        val text = "Intro\nWARNING: Hazardous voltage.\nSwitch off the isolator.\n\nNext paragraph"
        val notes = SafetyNoteExtractor.extract(listOf(chunk("a.pdf", 3, text)))
        assertEquals(1, notes.size)
        assertEquals("WARNING", notes[0].level)
        assertEquals("Hazardous voltage. Switch off the isolator.", notes[0].text)
        assertEquals("[1] a.pdf, p. 3", notes[0].source)
    }

    @Test fun `warning light symptom is not treated as a safety note`() {
        val notes = SafetyNoteExtractor.extract(listOf(chunk("a.pdf", 1, "Warning light is on after startup.")))
        assertTrue(notes.isEmpty())
    }

    @Test fun `titled label with a colon is captured and level is normalized`() {
        val notes = SafetyNoteExtractor.extract(listOf(chunk("a.pdf", 1, "Caution: hot surface.")))
        assertEquals(1, notes.size)
        assertEquals("CAUTION", notes[0].level)
        assertEquals("hot surface.", notes[0].text)

    }

    @Test fun `duplicate notes across chunks are reported once`() {
        val text = "DANGER: Do not open while energized."
        val notes = SafetyNoteExtractor.extract(listOf(chunk("a.pdf", 1, text), chunk("b.pdf", 2, text)))
        assertEquals(1, notes.size)
        assertEquals("[1] a.pdf, p. 1", notes[0].source)
    }

    @Test fun `very long notes are capped`() {
        val notes = SafetyNoteExtractor.extract(listOf(chunk("a.pdf", 1, "WARNING: " + "x".repeat(1000))))
        assertTrue(notes[0].text.length <= 300)
    }

    @Test fun `chunks without safety labels give no notes`() {
        assertTrue(SafetyNoteExtractor.extract(listOf(chunk("a.pdf", 1, "Replace the filter yearly."))).isEmpty())
    }
}