package com.example.techassist.data.rag

object RagConfig {
    /** Minimum relevance score for a chunk to count as relevant. Tuned with the Evaluation screen. */
    const val MIN_SIMILARITY = 0.4f

    /**
     * Share of the relevance score that comes from meaning (embeddings); the rest comes from keyword match.
     * 1.0 reproduces the old embedding-only behaviour.
     */
    const val DENSE_WEIGHT = 1.0f
}