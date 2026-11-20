package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.FocusedContext
import com.example.techassist.data.rag.PassageSelector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusedContextTest {
    private val lines = listOf("aaaaaaaaaa", "bbbbbbbbbb", "cccccccccc", "dddddddddd", "eeeeeeeeee")

    @Test fun `window grows around the centre while it fits`() {
        assertEquals("bbbbbbbbbb\ncccccccccc\ndddddddddd", FocusedContext.window(lines, 2, 32))
    }

    @Test fun `window at the start only grows forwards`() {
        assertEquals("aaaaaaaaaa\nbbbbbbbbbb", FocusedContext.window(lines, 0, 21))
    }

    @Test fun `a single line longer than the budget is cut`() {
        assertEquals(40, FocusedContext.window(listOf("x".repeat(100)), 0, 40).length)
    }

    @Test fun `the result never exceeds the budget`() {
        for (budget in 10..60) assertTrue(FocusedContext.window(lines, 2, budget).length <= budget)
    }

    @Test fun `the prompt context holds the best line, not the start of the chunk`() = runBlocking {
        val text = "Application temperature 5°C – +40°C\nMains power supply 24 V DC +/- 10%\n" +
                "Net weight 3060 g\nColour RAL9003 signal white"

        val chunk = RetrievedChunk("c", "d", "a.pdf", 15, text, 0.8f)
        val sameVector: suspend (String) -> FloatArray = { floatArrayOf(1f, 0f) }
        val selection = PassageSelector.selectWithContext("net weight", listOf(chunk), 0.6f, 20, sameVector)
        assertEquals("Net weight 3060 g", selection.focused[0].text)
    }
}