package com.example.techassist

import com.example.techassist.data.search.LexicalIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LexicalIndexTest {
    private val chunks = listOf(
        "ZBUS Corridor data bus ABUS Audio bus",
        "System release V10 V11 or V12 patch version CP99Config must be installed",
        "Net weight 3060 g plastic frame",
        "Display shows the time in idle mode",
    )
    private val index = LexicalIndex(chunks)

    private fun best(query: String): Int = index.scores(query).withIndex().maxByOrNull { it.value }!!.index

    @Test fun `a rare literal term finds its chunk`() {
        val scores = index.scores("What does ZBUS stand for?")
        assertEquals(0, best("What does ZBUS stand for?"))
        assertTrue(scores[0] > 0f)
        assertEquals(0f, scores[1], 0f)
    }

    @Test fun `version codes match`() {
        assertEquals(1, best("What must be installed when using V10.x or V11.x?"))
    }

    @Test fun `a term that is nowhere in the corpus scores zero everywhere`() {
        assertTrue(index.scores("price").all { it == 0f })
    }

    @Test fun `unseen query terms lower the score`() {
        val withExtra = index.scores("ZBUS price")[0]
        val without = index.scores("ZBUS")[0]
        assertTrue(withExtra < without)
    }

    @Test fun `scores stay within zero and one`() {
        assertTrue(index.scores("display time weight zbus v10").all { it in 0f..1f })
    }

    @Test fun `an empty index does not crash`() {
        assertEquals(0, LexicalIndex(emptyList()).scores("anything").size)
    }
}