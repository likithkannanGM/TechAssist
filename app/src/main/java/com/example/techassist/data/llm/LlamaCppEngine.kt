package com.example.techassist.data.llm

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.ladenthin.llama.LlamaModel
import net.ladenthin.llama.parameters.InferenceParameters
import net.ladenthin.llama.parameters.ModelParameters
import net.ladenthin.llama.value.Pair

class LlamaCppEngine : LlmEngine {
    private var model: LlamaModel? = null
    @Volatile private var lastUsed = 0L

    override val isLoaded: Boolean get() = model != null
    override val lastUsedAtMillis: Long get() = lastUsed

    override suspend fun load(modelPath: String) = withContext(Dispatchers.IO) {
        close()
        model = LlamaModel(
            ModelParameters()
                .setModel(modelPath)
                .setGpuLayers(0)              // CPU only
                .setThreads(THREADS)
                .setThreadsBatch(THREADS_BATCH)
                .setCtxSize(CONTEXT_SIZE)
        )
        lastUsed = SystemClock.elapsedRealtime()
    }


    override fun generate(systemPrompt: String, userMessage: String): Flow<String> = flow {
        val m = checkNotNull(model) { "Model is not loaded." }
        val params = InferenceParameters("")
            .withMessages(systemPrompt, listOf(Pair("user", userMessage)))
            .withTemperature(0.2f)
            .withNPredict(MAX_NEW_TOKENS)
        try {
            // use{} releases the native generation slot if the collector is canceled mid-stream
            m.generateChat(params).use { stream ->
                for (output in stream) emit(output.text)
            }
        } finally {
            lastUsed = SystemClock.elapsedRealtime()
        }
    }.flowOn(Dispatchers.IO)

    override fun close() {
        model?.close()
        model = null
    }

    private companion object {
        const val CONTEXT_SIZE = 3072
        const val MAX_NEW_TOKENS = 256
        const val THREADS = 4
        const val THREADS_BATCH = 6
    }
}