package com.example.techassist.data.rag

object RagConfig {
    /** Minimum relevance score for a chunk to count as relevant. Tuned with the Evaluation screen. */
    const val MIN_SIMILARITY = 0.4f

    /**
     * Share of the relevance score that comes from meaning (embeddings); the rest comes from keyword match.
     * 1.0 reproduces the old embedding-only behaviour.
     */
    const val DENSE_WEIGHT = 0.6f

    /** How many of the retrieved chunks go into the language model prompt, and how much of each. */
    const val WEAK_SIMILARITY = 0.25f

    /** The model is unloaded after this long without use and reloaded on the next question. */
    const val MODEL_IDLE_UNLOAD_MS = 120_000L

    const val PROMPT_CHUNKS = 2
    const val PROMPT_CHARS_PER_CHUNK = 450
}