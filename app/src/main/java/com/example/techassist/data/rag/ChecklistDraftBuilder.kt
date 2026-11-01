package com.example.techassist.data.rag

import com.example.techassist.data.DocumentRepository.RetrievedChunk

object ChecklistDraftBuilder {
    const val MAX_ITEMS = 40

    data class DraftItem(val text: String, val source: String, val isSafety: Boolean)

    private val NUMBER_PREFIX = Regex("^\\[\\d+]\\s*")

    /** Safety notes first (marked), then the manual's own steps in document order. */
    fun build(hits: List<RetrievedChunk>): List<DraftItem> {
        val safety = SafetyNoteExtractor.extract(hits).map {
            DraftItem("${it.level}: ${it.text}", it.source.replace(NUMBER_PREFIX, ""), isSafety = true)
        }
        val steps = StepExtractor.extract(hits).map {
            DraftItem(it.text, it.source, isSafety = false)
        }
        return (safety + steps).distinctBy { it.text.lowercase() }.take(MAX_ITEMS)
    }
}