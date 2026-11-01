package com.example.techassist

import com.example.techassist.data.search.TextTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTokenizerTest {
    @Test fun `lowercases, drops stop words and single characters, splits on punctuation`() {
        assertEquals(listOf("v10", "patch"), TextTokenizer.tokenize("What is the V10.x patch?"))
    }

    @Test fun `part numbers stay whole`() {
        assertEquals(
            listOf("part", "74659z4", "89500a2", "gb0"),
            TextTokenizer.tokenize("Part 74659Z4 and 89500A2.GB0"),
        )
    }

    @Test fun `plural s is stripped from longer words only`() {
        assertEquals(listOf("screw"), TextTokenizer.tokenize("screws"))
        assertEquals(listOf("zbus"), TextTokenizer.tokenize("ZBUS"))
        assertEquals(listOf("class"), TextTokenizer.tokenize("class"))
    }

    @Test fun `empty or stop-word-only text gives no terms`() {
        assertTrue(TextTokenizer.tokenize("").isEmpty())
        assertTrue(TextTokenizer.tokenize("What is the?").isEmpty())
    }
}