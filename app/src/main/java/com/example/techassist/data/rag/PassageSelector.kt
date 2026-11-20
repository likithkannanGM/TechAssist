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

    /** [passage] is the best single sentence; [focused] are the top chunks cut down to the lines around their best match. */
    class Selection(val passage: Passage?, val focused: List<RetrievedChunk>)

    private class Candidate(val hitIndex: Int, val lines: List<String>, val lineIndex: Int, val source: String)

    suspend fun select(
        question: String,
        hits: List<RetrievedChunk>,
        denseWeight: Float,
        embed: suspend (String) -> FloatArray,
    ): Passage? = selectWithContext(question, hits, denseWeight, Int.MAX_VALUE, embed).passage

    suspend fun selectWithContext(
        question: String,
        hits: List<RetrievedChunk>,
        denseWeight: Float,
        contextChars: Int,
        embed: suspend (String) -> FloatArray,
    ): Selection {
        val top = hits.take(MAX_HITS)
        val candidates = ArrayList<Candidate>()
        top.forEachIndexed { h, hit ->
            val lines = hit.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val source = "${hit.documentName}, p. ${hit.pageNumber}"
            lines.forEachIndexed { i, line ->
                if (line.length >= MIN_LINE_CHARS && candidates.size < MAX_LINES) {
                    candidates += Candidate(h, lines, i, source)
                }
            }
        }
        if (candidates.isEmpty()) return Selection(null, top.map { it.copy(text = it.text.take(contextChars)) })

        val texts = candidates.map { it.lines[it.lineIndex] }
        val lexical = LexicalIndex(texts).scores(question)
        val queryVector = embed(question)
        val scores = FloatArray(candidates.size) { i ->
            val dense = max(VectorSearch.cosineSimilarity(queryVector, embed(texts[i])), 0f)
            denseWeight * dense + (1f - denseWeight) * lexical[i]
        }

        val best = scores.indices.maxByOrNull { scores[it] } ?: 0
        val passage = if (scores[best] >= MIN_SCORE) {
            val c = candidates[best]
            Passage(rebuildSentence(c.lines, c.lineIndex).take(MAX_CHARS), c.source, scores[best])
        } else null

        val focused = top.mapIndexed { h, hit ->
            val bestForHit = candidates.indices.filter { candidates[it].hitIndex == h }.maxByOrNull { scores[it] }
            val text = if (bestForHit == null) {
                hit.text.take(contextChars)
            } else {
                FocusedContext.window(candidates[bestForHit].lines, candidates[bestForHit].lineIndex, contextChars)
            }
            hit.copy(text = text)
        }
        return Selection(passage, focused)
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