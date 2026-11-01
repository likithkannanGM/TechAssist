package com.example.techassist.data.rag

import com.example.techassist.data.DocumentRepository.RetrievedChunk

/**
 * Finds numbered or bulleted list items in retrieved page text, in document order.
 * Wording is copied from the manual, never generated.
 */
object StepExtractor {
    data class Step(val text: String, val source: String)

    private const val MIN_CHARS = 10
    private const val MAX_CHARS = 300
    private const val MAX_CONTINUATION_LINES = 3

    // "1. Text", "2) Text", "Step 3: Text"  (needs whitespace after the marker, so "2.5 mm" is not a step)
    // "(a) Text", "b. Text"
    // "• Text", "- Text", "– Text"
    private val MARKERS = listOf(
        Regex("""^\s*(?:Step\s+)?\d{1,2}\s*[.):]\s+(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^\s*\(?[a-h][.)]\s+(.+)$"""),
        Regex("""^\s*[•●▪◦·*–—-]\s+(.+)$"""),
    )

    private val DOT_LEADERS = Regex("\\.{4,}")   // table-of-contents lines
    private val WHITESPACE = Regex("\\s+")

    fun extract(hits: List<RetrievedChunk>): List<Step> {
        val seen = mutableSetOf<String>()
        val steps = mutableListOf<Step>()

        hits.sortedWith(compareBy<RetrievedChunk>({ it.documentName }, { it.pageNumber })).forEach { hit ->
            val source = "${hit.documentName}, p. ${hit.pageNumber}"
            val lines = hit.text.lines()
            var i = 0
            while (i < lines.size) {
                val first = markerContent(lines[i])
                if (first == null) { i++; continue }

                // PDF text is hard-wrapped, so join the lines that continue this step.
                val parts = mutableListOf(first.trim())
                var next = i + 1
                var continuation = 0
                while (next < lines.size && continuation < MAX_CONTINUATION_LINES &&
                    lines[next].isNotBlank() &&
                    markerContent(lines[next]) == null &&
                    !SafetyNoteExtractor.isSafetyLabel(lines[next])
                ) {
                    parts.add(lines[next].trim())
                    next++
                    continuation++
                }

                val text = parts.joinToString(" ").replace(WHITESPACE, " ").take(MAX_CHARS)
                if (isUsable(text) && seen.add(text.lowercase())) steps.add(Step(text, source))
                i = next
            }
        }
        return steps
    }

    private fun markerContent(line: String): String? =
        MARKERS.firstNotNullOfOrNull { it.find(line)?.groupValues?.get(1) }

    private fun isUsable(text: String): Boolean =
        text.length >= MIN_CHARS && !DOT_LEADERS.containsMatchIn(text)
}