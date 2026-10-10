package com.example.techassist.data.rag

import com.example.techassist.data.DocumentRepository.RetrievedChunk

object RagPromptBuilder {
    const val MAX_CHARS_PER_CHUNK = 700
    const val NOT_ENOUGH_INFO = "Not enough information in the imported documents to answer that."

    data class Prompt(val system: String, val user: String)

    fun build(question: String, hits: List<RetrievedChunk>): Prompt {
        val system = buildString {
            appendLine("You answer technical questions using only the context below.")
            appendLine("If the context does not contain the answer, reply exactly: $NOT_ENOUGH_INFO")
            appendLine("Be concise. When you use a source, mention its number like [1].")
            appendLine()
            appendLine("Context:")
            hits.forEachIndexed { i, hit ->
                appendLine("[${i + 1}] (${hit.documentName}, p. ${hit.pageNumber})")
                appendLine(trimChunk(hit.text))
                appendLine()
            }
        }.trimEnd()
        return Prompt(system = system, user = question.trim())
    }

    /** Human-readable source labels, numbered to match the [n] markers in the prompt. */
    fun sourceLabels(hits: List<RetrievedChunk>): List<String> =
        hits.mapIndexed { i, hit -> "[${i + 1}] ${hit.documentName}, p. ${hit.pageNumber}" }

    internal fun trimChunk(text: String): String {
        val t = text.trim()
        return if (t.length <= MAX_CHARS_PER_CHUNK) t else t.take(MAX_CHARS_PER_CHUNK).trimEnd() + "…"
    }
}