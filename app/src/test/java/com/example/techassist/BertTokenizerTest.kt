package com.example.techassist

import com.example.techassist.data.embedding.BertTokenizer
import org.junit.Assert.assertEquals
import org.junit.Test
class BertTokenizerTest {
    // A tiny fixture vocab — enough to exercise CLS/SEP wrapping, padding, and basic wordpiece splitting.
    private val vocab = mapOf(
        "[PAD]" to 0, "[UNK]" to 1, "[CLS]" to 2, "[SEP]" to 3,
        "hello" to 4, "world" to 5, "wash" to 6, "##ing" to 7,
    )
    private val tokenizer = BertTokenizer(vocab)

    @Test
    fun `wraps tokens with CLS and SEP`() {
        val encoding = tokenizer.encode("hello world", maxSeqLen = 8)
        assertEquals(2L, encoding.inputIds[0]) // [CLS]
        assertEquals(4L, encoding.inputIds[1]) // hello
        assertEquals(5L, encoding.inputIds[2]) // world
        assertEquals(3L, encoding.inputIds[3]) // [SEP]
    }

    @Test
    fun `pads to max sequence length with correct attention mask`() {
        val encoding = tokenizer.encode("hello", maxSeqLen = 6)
        assertEquals(6, encoding.inputIds.size)
        assertEquals(longArrayOf(1, 1, 1, 0, 0, 0).toList(), encoding.attentionMask.toList())
    }

    fun `splits unknown word into wordpiece subtokens when possible`() {
        val encoding = tokenizer.encode("washing", maxSeqLen = 8)
        // "washing" -> "wash" + "##ing" (literal substring concatenation, no spelling change)
        assertEquals(6L, encoding.inputIds[1]) // wash
        assertEquals(7L, encoding.inputIds[2]) // ##ing
    }
    @Test
    fun `falls back to UNK for truly unknown words`() {
        val encoding = tokenizer.encode("xyzzy", maxSeqLen = 8)
        assertEquals(1L, encoding.inputIds[1]) // [UNK]
    }
}