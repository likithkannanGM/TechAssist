package com.example.techassist.data.llm

import kotlinx.coroutines.flow.Flow

/** The rest of the app depends on this, not on llama.cpp, so the runtime can be swapped later. */
interface LlmEngine : AutoCloseable {
    val isLoaded: Boolean
    suspend fun load(modelPath: String)
    /** Streams the reply as text pieces. Cancelling the collector stops generation. */
    fun generate(systemPrompt: String, userMessage: String): Flow<String>
}