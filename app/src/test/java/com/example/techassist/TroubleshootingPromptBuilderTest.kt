package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.RagPromptBuilder
import com.example.techassist.data.rag.TroubleshootingPromptBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TroubleshootingPromptBuilderTest {

    private val hits = listOf(RetrievedChunk("c1", "d1", "a.pdf", 5, "Check the fuse.", 0.8f))

    @Test fun `user message contains fault, equipment and checked items when given`() {
        val p = TroubleshootingPromptBuilder.build("No output", "Drive X1", "Fuse is fine", hits)
        assertTrue(p.user.contains("Fault: No output"))
        assertTrue(p.user.contains("Equipment: Drive X1"))
        assertTrue(p.user.contains("Already checked: Fuse is fine"))
    }

    @Test fun `blank optional fields are left out`() {
        val p = TroubleshootingPromptBuilder.build("No output", "", "  ", hits)
        assertFalse(p.user.contains("Equipment:"))
        assertFalse(p.user.contains("Already checked:"))
    }

    @Test fun `system prompt has numbered context and the refusal sentence`() {
        val p = TroubleshootingPromptBuilder.build("No output", "", "", hits)
        assertTrue(p.system.contains("[1] (a.pdf, p. 5)"))
        assertTrue(p.system.contains(RagPromptBuilder.NOT_ENOUGH_INFO))
    }
}