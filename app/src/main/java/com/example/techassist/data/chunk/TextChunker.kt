package com.example.techassist.data.chunk

/**
 * Splits page text into overlapping, roughly token-sized chunks on sentence boundaries.
 * Token counts are approximated from word counts (~0.75 tokens/word for English) since
 * no real tokenizer is wired in until Phase 6's LLM runtime is integrated.
 */
object TextChunker {
    const val DEFAULT_CHUNK_SIZE_WORDS = 220
    const val DEFAULT_OVERLAP_WORDS = 40

    data class Chunk(val text: String, val wordCount: Int)

    fun chunkPageText(
        pageText: String,
        chunkSizeWords: Int = DEFAULT_CHUNK_SIZE_WORDS,
        overlapWords: Int = DEFAULT_OVERLAP_WORDS,
    ): List<Chunk> {
        require(overlapWords < chunkSizeWords) { "overlapWords must be smaller than chunkSizeWords" }
        if (pageText.isBlank()) return emptyList()

        val sentences = splitIntoSentences(pageText)
        if (sentences.isEmpty()) return emptyList()

        val chunks = mutableListOf<Chunk>()
        var currentWords = mutableListOf<String>()

        fun flushCurrent() {
            if (currentWords.isNotEmpty()) {
                val text = currentWords.joinToString(" ")
                chunks.add(Chunk(text, currentWords.size))
            }

        }

        for (sentence in sentences) {
            val sentenceWords = sentence.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (sentenceWords.isEmpty()) continue

            // A single sentence longer than the whole chunk size is split on word boundaries directly,
            // since waiting for a sentence end would otherwise produce one oversized chunk.
            if (sentenceWords.size > chunkSizeWords) {
                flushCurrent()
                currentWords = mutableListOf()
                var start = 0
                while (start < sentenceWords.size) {
                    val end = minOf(start + chunkSizeWords, sentenceWords.size)
                    chunks.add(Chunk(sentenceWords.subList(start, end).joinToString(" "), end - start))
                    start += (chunkSizeWords - overlapWords).coerceAtLeast(1)
                }
                continue
            }

            if (currentWords.size + sentenceWords.size > chunkSizeWords) {
                flushCurrent()
                // Start the next chunk with the overlap tail of the previous one.
                val overlapTail = currentWords.takeLast(overlapWords)
                currentWords = overlapTail.toMutableList()
            }
            currentWords.addAll(sentenceWords)
        }
        flushCurrent()

        return chunks
    }


    /** Simple rule-based sentence split: good enough for technical manuals, not linguistically perfect. */
    private fun splitIntoSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+(?=[A-Z0-9])"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
}