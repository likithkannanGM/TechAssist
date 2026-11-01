package com.example.techassist.data.rag

import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.search.LexicalIndex
import com.example.techassist.data.search.VectorSearch
import kotlin.math.max

/** Picks the one sentence or table row that best matches the question. It quotes the manual and never generates text. */
object PassageSelector {
    const val MAX_HITS = 2          // only the best chunks are searched line by line
    const val MAX_LINES = 24        // each line costs one short embedding, so keep this small
    const val MIN_LINE_CHARS = 12
    const val MAX_CHARS = 280
    const val MIN_SCORE = 0.25f     // below this, no passage is shown
    private const val WRAP_MIN_CHARS = 45
    private const val MAX_EXTENSION = 2

    data class Passage(val text: String, val source: String, val score: Float)

    private class Candidate(val lines: List<String>, val index: Int, val source: String)

    suspend fun select(
        question: String,
        hits: List<RetrievedChunk>,
        denseWeight: Float,
        embed: suspend (String) -> FloatArray,
    ): Passage? {
        val candidates = ArrayList<Candidate>()
        for (hit in hits.take(MAX_HITS)) {
            val lines = hit.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val source = "${hit.documentName}, p. ${hit.pageNumber}"
            lines.forEachIndexed { i, line ->

                if (line.length >= MIN_LINE_CHARS && candidates.size < MAX_LINES) {
                    candidates += Candidate(lines, i, source)
                }
            }
        }
        if (candidates.isEmpty()) return null

        val texts = candidates.map { it.lines[it.index] }
        val lexical = LexicalIndex(texts).scores(question)
        val queryVector = embed(question)

        var best = -1
        var bestScore = -1f
        for (i in candidates.indices) {
            val dense = max(VectorSearch.cosineSimilarity(queryVector, embed(texts[i])), 0f)
            val score = denseWeight * dense + (1f - denseWeight) * lexical[i]
            if (score > bestScore) {
                bestScore = score
                best = i
            }
        }
        if (best < 0 || bestScore < MIN_SCORE) return null

        val c = candidates[best]
        return Passage(rebuildSentence(c.lines, c.index).take(MAX_CHARS), c.source, bestScore)
    }

    /** PDF text is hard-wrapped: long lines that do not end a sentence continue on the next line. Short table rows stay alone. */
    internal fun rebuildSentence(lines: List<String>, index: Int): String {
        var start = index
        var steps = 0
        while (start > 0 && steps < MAX_EXTENSION && isWrapped(lines[start - 1])) { start--; steps++ }

        var end = index
        steps = 0
        while (end < lines.lastIndex && steps < MAX_EXTENSION && isWrapped(lines[end])) { end++; steps++ }
        return lines.subList(start, end + 1).joinToString(" ")
    }

    private fun isWrapped(line: String): Boolean =
        line.length >= WRAP_MIN_CHARS && line.last() !in ".!?:;"
}