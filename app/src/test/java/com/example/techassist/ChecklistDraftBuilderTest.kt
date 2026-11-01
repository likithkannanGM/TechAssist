package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.ChecklistDraftBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistDraftBuilderTest {

    private fun chunk(page: Int, text: String) =
        RetrievedChunk("c$page", "d1", "a.pdf", page, text, 0.8f)

    @Test fun `safety items come first, are flagged, and carry a plain source label`() {
        val hits = listOf(chunk(2, "WARNING: High voltage.\n\n1. Isolate the supply before opening."))
        val draft = ChecklistDraftBuilder.build(hits)
        assertEquals(2, draft.size)
        assertTrue(draft[0].isSafety)
        assertEquals("WARNING: High voltage.", draft[0].text)
        assertEquals("a.pdf, p. 2", draft[0].source)
        assertFalse(draft[1].isSafety)
    }

    @Test fun `list is capped`() {
        val text = (1..60).joinToString("\n") { "$it. Perform inspection step number $it" }
        assertEquals(ChecklistDraftBuilder.MAX_ITEMS, ChecklistDraftBuilder.build(listOf(chunk(1, text))).size)
    }

    @Test fun `pages without lists give an empty draft`() {
        assertTrue(ChecklistDraftBuilder.build(listOf(chunk(1, "Plain descriptive paragraph."))).isEmpty())
    }

}