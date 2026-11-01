package com.example.techassist

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.rag.PassageSelector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassageSelectorTest {

    private fun chunk(page: Int, text: String) = RetrievedChunk("c$page", "d1", "a.pdf", page, text, 0.8f)

    // Same vector for everything: the meaning part ties, so the keyword part decides.
    private val sameEmbedding: suspend (String) -> FloatArray = { floatArrayOf(1f, 0f) }

    @Test fun `picks the line that contains the question's words`() = runBlocking {
        val text = "Application temperature 5°C – +40°C\nMains power supply 24 V DC +/- 10%\nNet weight 3060 g"
        val passage = PassageSelector.select("What is the net weight?", listOf(chunk(15, text)), 0.6f, sameEmbedding)
        assertNotNull(passage)
        assertEquals("Net weight 3060 g", passage!!.text)
        assertEquals("a.pdf, p. 15", passage.source)
    }

    @Test fun `a wrapped sentence is rebuilt from its lines`() = runBlocking {
        val text = "Approx. 20 seconds after switching on the SINF, the\n" +
                "Honeywell logo is displayed, which signals that the device\n" +
                "has successfully started up."
        val passage = PassageSelector.select(
            "How long after switching on does the Honeywell logo appear?", listOf(chunk(11, text)), 0.6f, sameEmbedding,

            )
        assertEquals(
            "Approx. 20 seconds after switching on the SINF, the Honeywell logo is displayed, " +
                    "which signals that the device has successfully started up.",
            passage!!.text,
        )
    }

    @Test fun `short table rows are not merged`() {
        val lines = listOf("Net weight 3060 g", "Protection class IP20", "Colour RAL9003")
        assertEquals("Protection class IP20", PassageSelector.rebuildSentence(lines, 1))
    }

    @Test fun `no passage when nothing matches by meaning or keywords`() = runBlocking {
        val question = "price"
        val embed: suspend (String) -> FloatArray = { t -> if (t == question) floatArrayOf(1f, 0f) else floatArrayOf(0f, 1f) }
        assertNull(PassageSelector.select(question, listOf(chunk(4, "Net weight 3060 g")), 0.6f, embed))
    }

    @Test fun `long passages are cut to the maximum`() = runBlocking {
        val long = "word ".repeat(100).trim()
        val passage = PassageSelector.select("word", listOf(chunk(1, long)), 0.6f, sameEmbedding)
        assertTrue(passage!!.text.length <= PassageSelector.MAX_CHARS)
    }

    @Test fun `no hits gives no passage`() = runBlocking {
        assertNull(PassageSelector.select("anything", emptyList(), 0.6f, sameEmbedding))
    }
}