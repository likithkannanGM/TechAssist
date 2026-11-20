package com.example.techassist.data.llm

import kotlinx.coroutines.flow.Flow

interface LlmEngine : AutoCloseable {
    val isLoaded: Boolean
    /** When the model was last used, in SystemClock.elapsedRealtime() milliseconds (0 = never). */
    val lastUsedAtMillis: Long
    suspend fun load(modelPath: String)
    fun generate(systemPrompt: String, userMessage: String): Flow<String>

    /** One tiny generation that finishes by itself, so the model is read from storage now and not during the first answer. */
    suspend fun warmUp() {
        generate("Reply with the single word OK.", "Say OK.").collect { }
    }
}