package com.example.techassist.data.rag

import com.example.techassist.data.DocumentRepository.RetrievedChunk

object SafetyNoteExtractor {
    data class SafetyNote(val level: String, val text: String, val source: String)

    // "WARNING", "CAUTION", "DANGER" in capitals, or "Warning:" style with a colon/bang.
    // A bare "Warning light is on" is a fault symptom, not a safety note, so it must not match.
    private val UPPER = Regex("^\\s*(WARNING|CAUTION|DANGER)\\b[:!.\\s-]*(.*)$")
    private val TITLED = Regex("^\\s*(Warning|Caution|Danger)\\s*[:!]\\s*(.*)$")

    private const val MAX_LINES = 3
    private const val MAX_CHARS = 300

    fun extract(hits: List<RetrievedChunk>): List<SafetyNote> {
        val labels = RagPromptBuilder.sourceLabels(hits)
        val seen = mutableSetOf<String>()
        val notes = mutableListOf<SafetyNote>()

        hits.forEachIndexed { i, hit ->
            val lines = hit.text.lines()
            var idx = 0
            while (idx < lines.size) {
                val start = matchStart(lines[idx])
                if (start == null) { idx++; continue }

                val parts = mutableListOf(start.second)
                var next = idx + 1
                var taken = 1
                while (next < lines.size && taken < MAX_LINES &&
                    lines[next].isNotBlank() && matchStart(lines[next]) == null
                ) {
                    parts.add(lines[next].trim())
                    next++
                    taken++
                }

                val text = parts.filter { it.isNotEmpty() }.joinToString(" ").take(MAX_CHARS)
                if (text.isNotEmpty() && seen.add(text.lowercase())) {
                    notes.add(SafetyNote(level = start.first, text = text, source = labels[i]))
                }
                idx = next
            }
        }
        return notes
    }

    /** True if the line starts a WARNING/CAUTION/DANGER note. */
    fun isSafetyLabel(line: String): Boolean = matchStart(line) != null

    /** Returns (LEVEL, text after the label) if the line starts a safety note, else null. */    private fun matchStart(line: String): Pair<String, String>? {
        UPPER.find(line)?.let { return it.groupValues[1] to it.groupValues[2].trim() }
        TITLED.find(line)?.let { return it.groupValues[1].uppercase() to it.groupValues[2].trim() }
        return null
    }
}