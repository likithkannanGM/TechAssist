package com.example.techassist

import com.example.techassist.data.llm.GgufCheck
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GgufCheckTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `file starting with GGUF is accepted`() {
        val f = tmp.newFile().apply { writeBytes("GGUF".toByteArray() + ByteArray(16)) }
        assertTrue(GgufCheck.hasMagic(f))
    }

    @Test fun `other content is rejected`() {
        val f = tmp.newFile().apply { writeText("%PDF-1.7 not a model") }
        assertFalse(GgufCheck.hasMagic(f))
    }

    @Test fun `file shorter than 4 bytes is rejected`() {
        val f = tmp.newFile().apply { writeBytes(byteArrayOf(1, 2)) }
        assertFalse(GgufCheck.hasMagic(f))
    }
}