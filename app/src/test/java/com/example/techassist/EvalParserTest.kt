package com.example.techassist

import com.example.techassist.data.eval.EvalParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvalParserTest {

    @Test fun `both case types are parsed and comments and blank lines are ignored`() {
        val text = """
            # my test set

            ANSWERABLE | What is the supply voltage? | drive.pdf | 12
            UNANSWERABLE | Who won the 2018 World Cup?
        """.trimIndent()
        val result = EvalParser.parse(text)
        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.cases.size)
        assertTrue(result.cases[0].answerable)
        assertEquals("drive.pdf", result.cases[0].expectedDocument)
        assertEquals(setOf(12), result.cases[0].expectedPages)
        assertFalse(result.cases[1].answerable)
    }

    @Test fun `page lists and ranges are expanded`() {
        assertEquals(setOf(12, 14, 15, 16), EvalParser.parsePages("12,14-16"))
    }

    @Test fun `invalid page specs are rejected`() {
        assertNull(EvalParser.parsePages(""))
        assertNull(EvalParser.parsePages("abc"))
        assertNull(EvalParser.parsePages("0"))
        assertNull(EvalParser.parsePages("9-3"))
        assertNull(EvalParser.parsePages("1,,2"))
    }

    @Test fun `bad lines produce errors with line numbers and valid lines are still kept`() {
        val text = "ANSWERABLE | question only\nQUESTION | what | a.pdf | 1\nANSWERABLE | ok question | a.pdf | 3"
        val result = EvalParser.parse(text)
        assertEquals(1, result.cases.size)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors[0].startsWith("Line 1"))
        assertTrue(result.errors[1].startsWith("Line 2"))
    }

    @Test fun `answerable case without pages is an error`() {
        assertEquals(1, EvalParser.parse("ANSWERABLE | question | a.pdf | ").errors.size)
    }
}