package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.RagPromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RagPromptBuilderTest {

    private fun chunk(name: String, page: Int, text: String) =
        RetrievedChunk("c-$page", "d-$name", name, page, text, 0.8f)

    @Test fun `sources are numbered in order and carry file and page`() {
        val hits = listOf(chunk("a.pdf", 3, "alpha"), chunk("b.pdf", 7, "beta"))
        val prompt = RagPromptBuilder.build("What is alpha?", hits)
        assertTrue(prompt.system.contains("[1] (a.pdf, p. 3)"))
        assertTrue(prompt.system.contains("[2] (b.pdf, p. 7)"))
        assertEquals(listOf("[1] a.pdf, p. 3", "[2] b.pdf, p. 7"), RagPromptBuilder.sourceLabels(hits))
    }

    @Test fun `question goes in the user message, not the system prompt`() {
        val prompt = RagPromptBuilder.build("  What is alpha?  ", listOf(chunk("a.pdf", 1, "alpha")))
        assertEquals("What is alpha?", prompt.user)
        assertFalse(prompt.system.contains("What is alpha?"))
    }

    @Test fun `long chunks are trimmed`() {
        val long = "x".repeat(RagPromptBuilder.MAX_CHARS_PER_CHUNK * 3)
        val trimmed = RagPromptBuilder.trimChunk(long)
        assertTrue(trimmed.length <= RagPromptBuilder.MAX_CHARS_PER_CHUNK + 1) // +1 for the ellipsis
        assertTrue(trimmed.endsWith("…"))
    }

    @Test fun `short chunks are kept as-is`() {
        assertEquals("short text", RagPromptBuilder.trimChunk("  short text \n"))
    }

    @Test fun `prompt contains the refusal sentence`() {
        val prompt = RagPromptBuilder.build("q", listOf(chunk("a.pdf", 1, "t")))
        assertTrue(prompt.system.contains(RagPromptBuilder.NOT_ENOUGH_INFO))
    }
}