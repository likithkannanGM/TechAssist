package com.example.techassist.data.rag

import com.example.techassist.data.DocumentRepository.RetrievedChunk

object TroubleshootingPromptBuilder {

    fun build(
        fault: String,
        equipment: String,
        alreadyChecked: String,
        hits: List<RetrievedChunk>,
    ): RagPromptBuilder.Prompt {
        val system = buildString {
            appendLine("You help a technician diagnose a fault using only the context below.")
            appendLine("Reply in exactly this format:")
            appendLine("Likely causes:")
            appendLine("- (up to 3 short bullets)")
            appendLine("Checks to perform:")
            appendLine("1. (short numbered steps, in a safe order)")
            appendLine("If the context does not help, reply exactly: ${RagPromptBuilder.NOT_ENOUGH_INFO}")
            appendLine("Do not invent values, part numbers or procedures that are not in the context.")
            appendLine("Mention source numbers like [1] when you use them.")
            appendLine()
            appendLine("Context:")
            hits.forEachIndexed { i, hit ->
                appendLine("[${i + 1}] (${hit.documentName}, p. ${hit.pageNumber})")
                appendLine(RagPromptBuilder.trimChunk(hit.text))
                appendLine()
            }
        }.trimEnd()

        val user = buildString {

            appendLine("Fault: ${fault.trim()}")
            if (equipment.isNotBlank()) appendLine("Equipment: ${equipment.trim()}")
            if (alreadyChecked.isNotBlank()) appendLine("Already checked: ${alreadyChecked.trim()}")
        }.trimEnd()

        return RagPromptBuilder.Prompt(system = system, user = user)
    }
}