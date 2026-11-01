package com.example.techassist.data.search

import kotlin.math.ln

/**
 * Keyword index over chunk texts. For a query it gives each chunk a score between 0 and 1:
 * the importance-weighted share of the query's terms that the chunk contains (BM25 term weighting,
 * divided by the best possible total). Rare terms count more, and query terms that appear
 * nowhere in the corpus count against every chunk.
 */
class LexicalIndex(chunkTexts: List<String>) {
    private val size = chunkTexts.size
    private val termCounts: List<Map<String, Int>>
    private val lengths: IntArray
    private val docFreq: Map<String, Int>
    private val avgLength: Float

    init {
        val tokenized = chunkTexts.map { TextTokenizer.tokenize(it) }
        termCounts = tokenized.map { tokens -> tokens.groupingBy { it }.eachCount() }
        lengths = IntArray(size) { tokenized[it].size }
        avgLength = if (size == 0) 1f else (lengths.sum().toFloat() / size).coerceAtLeast(1f)
        val df = HashMap<String, Int>()
        termCounts.forEach { counts -> counts.keys.forEach { df[it] = (df[it] ?: 0) + 1 } }
        docFreq = df
    }

    fun scores(query: String): FloatArray {
        val result = FloatArray(size)
        val terms = TextTokenizer.tokenize(query).distinct()
        if (terms.isEmpty() || size == 0) return result

        val idf = terms.map { idf(it) }
        val idfSum = idf.sum()
        for (i in 0 until size) {
            val counts = termCounts[i]
            val norm = K1 * (1 - B + B * lengths[i] / avgLength)
            var sum = 0f
            terms.forEachIndexed { t, term ->
                val tf = counts[term] ?: return@forEachIndexed
                val saturation = tf * (K1 + 1) / (tf + norm)
                sum += idf[t] * minOf(1f, saturation)
            }
            result[i] = sum / idfSum
        }
        return result
    }

    private fun idf(term: String): Float {
        val df = docFreq[term] ?: 0
        return ln(1f + (size - df + 0.5f) / (df + 0.5f))
    }

    private companion object {
        const val K1 = 1.2f
        const val B = 0.75f
    }
}