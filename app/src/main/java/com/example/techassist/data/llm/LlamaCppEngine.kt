package com.example.techassist.data.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.ladenthin.llama.LlamaModel
// Guesses: replace with whatever Alt+Enter offers
import net.ladenthin.llama.parameters.InferenceParameters
import net.ladenthin.llama.parameters.ModelParameters
import net.ladenthin.llama.value.Pair

class LlamaCppEngine : LlmEngine {
    private var model: LlamaModel? = null

    override val isLoaded: Boolean get() = model != null

    override suspend fun load(modelPath: String) = withContext(Dispatchers.IO) {
        close()
        model = LlamaModel(
            ModelParameters()
                .setModel(modelPath)
                .setGpuLayers(0)              // CPU only
                .setCtxSize(CONTEXT_SIZE)     // if this fails, see the note below
        )
    }

    override fun generate(systemPrompt: String, userMessage: String): Flow<String> = flow {
        val m = checkNotNull(model) { "Model is not loaded." }
        val params = InferenceParameters("")
            .withMessages(systemPrompt, listOf(Pair("user", userMessage)))
            .withTemperature(0.2f)        // guess: see below if red
            .withNPredict(MAX_NEW_TOKENS) // seen in the README's examples
        // use{} releases the native generation slot if the collector is canceled mid-stream
        m.generateChat(params).use { stream ->
            for (output in stream) emit(output.text)
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        model?.close()
        model = null
    }

    private companion object {
        const val CONTEXT_SIZE = 3072
        const val MAX_NEW_TOKENS = 256
    }
}